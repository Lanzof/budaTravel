## Routing API Specification (budaTravel)

### 1. Overview

This document describes new HTTP endpoints for:

- Managing and discovering **locations** (stops).
- Performing **multi-hop route search** between locations, optimized for **fastest** or **cheapest** route.

Existing codebase:

- Backend: Kotlin, Spring Boot.
- Modules:
  - `core` — domain entities and repositories (`Location`, `Connection`, `LocationRepo`, `ConnectionRepo`, `RouteService`).
  - `api` — HTTP API and DTOs.
  - `ingestor` — GTFS ingestion into the database.
  - `common` — shared DTOs (`RouteResponse`, etc.).

The new API must reuse the existing domain model where possible.
Schema lifecycle is managed via Flyway migrations; JPA `ddl-auto` is used in `validate` mode.

---

### 2. Domain and Data Model

#### 2.1. Location

- Entity: `io.lanzof.core.entity.Location`
- Fields (relevant for API):

  - `stopId: String` — primary external identifier (from GTFS, e.g. stop code).
  - `name: String` — human-readable stop name.
  - `lat: Double` — latitude.
  - `lon: Double` — longitude.
  - Internal `id: UUID` is **not exposed** to API clients.

#### 2.2. Connection

- Entity: `io.lanzof.core.entity.Connection`
- Fields (relevant for routing):

  - `fromLocation: Location`
  - `toLocation: Location`
  - `departureTime: OffsetDateTime`
  - `arrivalTime: OffsetDateTime`
  - `price: BigDecimal`
  - `carrier: String`
  - `type: String` — transport type:
    - Must support (at least): `"BUS"`, `"TRAIN"`, `"METRO"`.
    - **Current data source** (GTFS archive of Budapest buses) uses `"BUS"` only.
    - In persisted data, `"PUBLIC_TRANSPORT"` is treated as `"BUS"` alias by routing logic.

#### 2.3. Route and Segment DTOs

New/updated DTOs used by the routing API:

- `RouteResponse` (per full route/path):

  - `totalDuration: string`  
    - ISO-8601 duration, e.g. `"PT1H30M"`.
  - `totalPrice: number` (decimal)  
    - Sum of segment prices.
  - `segments: RouteSegment[]`  
    - Ordered segments from origin to destination.

- `RouteSegment`:

  - `fromStopId: string`
  - `toStopId: string`
  - `fromName: string`
  - `toName: string`
  - `fromLat: number`
  - `fromLon: number`
  - `toLat: number`
  - `toLon: number`
  - `departureTime: string`  
    - ISO-8601 `OffsetDateTime`, e.g. `"2025-01-10T14:30:00+01:00"`.
  - `arrivalTime: string`  
    - ISO-8601 `OffsetDateTime`.
  - `carrier: string`
  - `type: string` — `"BUS"` / `"TRAIN"` / `"METRO"` / etc.

All timestamps in responses must include timezone offset.
(`Z`/UTC offset is also valid ISO-8601 offset representation.)

---

### 3. Location API

Base path: `/api/v1/locations`

All responses use JSON.

#### 3.1. GET /api/v1/locations

**Purpose**: list locations (stops) with optional filtering and pagination.

**Request**

- Query parameters:

  - `q: string (optional)`  
    - Substring search by stop name (case-insensitive).

  - `stopId: string (optional)`  
    - Exact match by `stopId`.

  - `limit: integer (optional, default = 50, max = 200)`  
    - Maximum number of results.

  - `offset: integer (optional, default = 0)`  
    - Number of items to skip.

**Mutual exclusivity rule**

- If **both** `q` and `stopId` are provided in the same request:  
  - API must return **400 Bad Request** with a clear error message.

**Response 200 OK**

- Body: `LocationDto[]`

  ```json
  [
    {
      "stopId": "ABC123",
      "name": "Budapest, Example Stop",
      "lat": 47.4979,
      "lon": 19.0402
    }
  ]
  ```

- `LocationDto`:

  - `stopId: string`
  - `name: string`
  - `lat: number`
  - `lon: number`

**Response 400 Bad Request**

- When both `q` and `stopId` are provided.
- When `limit` or `offset` are invalid (negative, non-numeric, etc.).

Example:

```json
{
  "code": "INVALID_QUERY_PARAMETERS",
  "message": "Parameters 'q' and 'stopId' cannot be used together."
}
```

---

#### 3.2. GET /api/v1/locations/{stopId}

**Purpose**: get details of a single location by `stopId`.

**Request**

- Path parameter:

  - `stopId: string` — external identifier of the stop.

**Response 200 OK**

```json
{
  "stopId": "ABC123",
  "name": "Budapest, Example Stop",
  "lat": 47.4979,
  "lon": 19.0402
}
```

**Response 404 Not Found**

- When a location with the given `stopId` does not exist.

---

#### 3.3. GET /api/v1/locations/autocomplete

**Purpose**: lightweight endpoint for UI autocomplete (search suggestions).

**Request**

- Query parameters:

  - `q: string (required)`  
    - Substring used for suggestions.
    - May match by name; optionally may also match by `stopId`.

  - `limit: integer (optional, default = 10, max = 50)`

**Response 200 OK**

- Body: `LocationSuggestionDto[]` (same fields as `LocationDto`):

  ```json
  [
    {
      "stopId": "ABC123",
      "name": "Budapest, Example Stop",
      "lat": 47.4979,
      "lon": 19.0402
    }
  ]
  ```

**Response 400 Bad Request**

- When `q` is missing or empty.

---

### 4. Route Search API (Multi-hop)

Base path: `/api/v1/routes`

Existing endpoint `POST /api/v1/routes/search` is **redefined** to support multi-hop routing with optimization by fastest or cheapest route, while preserving the general concept of returning an array of `RouteResponse`.

#### 4.1. Business rules

- Routing supports **multi-hop paths** (A → ... → B) built on top of `Connection` graph.
- Default optimization criterion: **FASTEST** (shortest total duration).
- Alternative criterion: **CHEAPEST** (lowest total price).
- **Maximum number of transfers** (changes of vehicle/line) is limited to **3**:
  - This is a **business constraint**:
    - The algorithm must **not** produce routes with more than 3 transfers.
- Timezone & time format:
  - All timestamps stored and processed as `OffsetDateTime`.
  - Clients send time as a single ISO-8601 datetime with offset (field `departureDateTime`).
  - There is no separate `timeZone` field.
- Segment duration normalization:
  - If `arrivalTime == departureTime` for a segment (0 seconds), segment duration is treated as **1 minute**.
  - Route `totalDuration` is calculated using this normalization.
- Search strategy and limits (implementation-level guarantees):
  - Best-first search (Dijkstra-like ordering) is used instead of full graph path enumeration.
  - Max route results returned per request: **5**.
  - No full in-memory dump of `connections` table is used in routing flow.

#### 4.2. Request: POST /api/v1/routes/search

**Request Body: `RouteSearchRequest`**

```json
{
  "originStopId": "ORIGIN_STOP",
  "destinationStopId": "DEST_STOP",
  "departureDateTime": "2025-01-10T14:30:00+01:00",
  "optimization": "FASTEST",
  "transportTypes": ["BUS"]
}
```

- Fields:

  - `originStopId: string` (required)
  - `destinationStopId: string` (required)

  - `departureDateTime: string` (required)  
    - ISO-8601 datetime with timezone offset, e.g. `"2025-01-10T14:30:00+01:00"`.
    - Server validates format and uses parsed `OffsetDateTime` in routing queries.

  - `optimization: string (optional)`  
    - Allowed values: `"FASTEST"`, `"CHEAPEST"`.
    - Default: `"FASTEST"`.

  - `transportTypes: string[] (optional)`  
    - Allowed values (extensible): `"BUS"`, `"TRAIN"`, `"METRO"`, ...
    - For current GTFS data, only `"BUS"` is effectively available.
    - Default: `["BUS"]` (if omitted).

- Future/optional (can be implemented later and included in spec if needed):

  - `maxResults: integer (optional, default = e.g. 10–20)` — maximum number of returned routes.

> Note: Business rule "max 3 transfers" must always be respected even if optional parameters to limit hops are added later.

---

#### 4.3. Response: 200 OK

**Body: `RouteResponse[]`**

Example:

```json
[
  {
    "totalDuration": "PT1H30M",
    "totalPrice": 0.0,
    "segments": [
      {
        "fromStopId": "STOP_A",
        "toStopId": "STOP_B",
        "fromName": "Budapest, Stop A",
        "toName": "Budapest, Stop B",
        "fromLat": 47.4979,
        "fromLon": 19.0402,
        "toLat": 47.4985,
        "toLon": 19.0450,
        "departureTime": "2025-01-10T14:30:00+01:00",
        "arrivalTime": "2025-01-10T14:50:00+01:00",
        "carrier": "BKK",
        "type": "BUS"
      },
      {
        "fromStopId": "STOP_B",
        "toStopId": "STOP_C",
        "fromName": "Budapest, Stop B",
        "toName": "Budapest, Stop C",
        "fromLat": 47.4985,
        "fromLon": 19.0450,
        "toLat": 47.5010,
        "toLon": 19.0500,
        "departureTime": "2025-01-10T15:00:00+01:00",
        "arrivalTime": "2025-01-10T15:30:00+01:00",
        "carrier": "BKK",
        "type": "BUS"
      }
    ]
  }
]
```

- Each `RouteResponse`:
  - Represents a full path from `originStopId` to `destinationStopId`.
  - `segments[]` are ordered in actual travel order.
  - Number of transfers = `segments.size - 1` and must be ≤ 3 (due to business rule).

---

#### 4.4. Error Handling

- **400 Bad Request**

  - Invalid or missing fields in `RouteSearchRequest`:

    - Missing `originStopId` / `destinationStopId`.
    - Missing or invalid `departureDateTime` (not ISO-8601 with offset).
    - `originStopId == destinationStopId`.
    - Unsupported `optimization` value.
    - Unsupported values in `transportTypes`.

  - Example:

    ```json
    {
      "code": "INVALID_REQUEST",
      "message": "Field 'departureDateTime' must be a valid ISO-8601 datetime with timezone offset."
    }
    ```

- **404 Not Found**

  - When `originStopId` or `destinationStopId` does not exist in the locations table.

- **422 Unprocessable Entity**

  - When no route can be found that satisfies:
    - origin
    - destination
    - departureDateTime
    - max 3 transfers
    - selected `transportTypes`
  - Example:

    ```json
    {
      "code": "NO_ROUTE_FOUND",
      "message": "No route found for the given parameters."
    }
    ```

---

### 5. Map Integration Considerations

- Backend responsibilities:

  - Expose precise coordinates for:
    - Locations (via `/locations` and `/locations/autocomplete`).
    - Segments in routes (via `RouteSegment.fromLat/fromLon/toLat/toLon`).

- Frontend responsibilities:

  - Use MapLibre or Google Maps to render:
    - Markers for stops.
    - Polylines between segment coordinates to visualize routes.

- The backend does **not** generate detailed road geometry; it provides stop-to-stop segment coordinates. The frontend builds the visual route representation.

---

### 6. OpenAPI / Documentation

- OpenAPI is generated by SpringDoc and controller annotations, and includes:

  - All new/updated endpoints:
    - `GET /api/v1/locations`
    - `GET /api/v1/locations/{stopId}`
    - `GET /api/v1/locations/autocomplete`
    - `POST /api/v1/routes/search` (new request/response schemas)

  - Schemas:
    - `LocationDto`
    - `LocationSuggestionDto`
    - `RouteSearchRequest`
    - `RouteResponse`
    - `RouteSegment`
    - Error response models.

- Ensure generated Swagger UI clearly reflects:
  - Time format requirements (ISO-8601 with offset).
  - Optimization options.
  - Business rule on max 3 transfers (documented in description).

---

### 7. Persistence and Query Constraints

- Database schema and indexes are managed via Flyway migrations:
  - `V1__init_schema.sql` — base tables/constraints.
  - `V2__routing_indexes.sql` — routing indexes.
- Routing-critical indexes:
  - `location(stop_id)` unique index.
  - `connections(from_location_id, departure_time)` index.
  - `connections(type, departure_time)` index.
- Routing expansion uses bounded next-hop query by:
  - `from_location_id`,
  - `departure_time >= :time`,
  - `type IN (...)`,
  - ordered by `departure_time`.
- Runtime schema auto-creation is not used as primary mechanism.

---

### 8. Acceptance Criteria

1. **Location API**
   - `GET /api/v1/locations`:
     - Works with `q`, with `stopId`, with pagination.
     - Returns 400 when both `q` and `stopId` are provided.
   - `GET /api/v1/locations/{stopId}`:
     - Returns 200 for existing `stopId`.
     - Returns 404 for non-existing `stopId`.
   - `GET /api/v1/locations/autocomplete`:
     - Requires `q`, returns limited suggestions.

2. **Route Search API**
   - `POST /api/v1/routes/search`:
     - Accepts `RouteSearchRequest` as defined.
   - Supports `optimization = FASTEST | CHEAPEST`.
   - Respects max 3 transfers rule in all produced routes.
   - Applies duration normalization rule: `0 seconds` segment duration => `1 minute`.
   - Returns `RouteResponse[]` with segments including coordinates.
   - Properly validates ISO-8601 `departureDateTime` with timezone offset.
   - Returns at most 5 best routes per request, ordered by requested optimization.

3. **Error Handling**
   - Meaningful 400/404/422 responses with structured error bodies.

4. **Documentation**
   - OpenAPI/Swagger updated and shows all new contracts.
   - Example requests/responses are available via Swagger UI.

5. **Routing Performance Constraints**
   - Routing flow does not use `connectionRepo.findAll()`/full table load.
   - Routing DB indexes are delivered and versioned through Flyway migrations.
