# API/UI Contract Sync — MVP

## Status
Implemented for current MVP demo

## Purpose

This document captures the minimum backend contract the first web UI can rely on.
It is intentionally practical: enough to let backend and frontend work in parallel without waiting for perfect routing or perfect map geometry.

## Current API endpoints relevant to UI

### List stops for map markers

```http
GET /api/v1/locations?limit=200&offset=0
GET /api/v1/locations?minLat=47.49&maxLat=47.51&minLon=19.02&maxLon=19.06
```

Response item shape:

```json
{
  "stopId": "F00985",
  "name": "Deák Ferenc tér M",
  "lat": 47.497701,
  "lon": 19.053353
}
```

Notes:

- Current endpoint supports `q`, `stopId`, `limit`, `offset`, and optional bbox parameters: `minLat`, `maxLat`, `minLon`, `maxLon`.
- For `budapest-mini.zip`, loading all stops is fine.
- For full data, bbox is the first filter; clustering may still be needed later.

### Autocomplete stops

```http
GET /api/v1/locations/autocomplete?q=deak&limit=10
```

Autocomplete search is accent-insensitive: user input and imported stop names are normalized before lookup, so `Deak Ferenc` can match `Deák Ferenc`. The current MVP implementation stores a derived `location.name_normalized` value in Postgres instead of introducing a separate search engine. The project is still pre-production, so the migration assumes a fresh local/demo database and does not backfill old rows.

Response item shape is the same minimal stop shape:

```json
{
  "stopId": "F00985",
  "name": "Deak Ferenc ter M",
  "lat": 47.497701,
  "lon": 19.053353
}
```

### Get one stop

```http
GET /api/v1/locations/{stopId}
```

Response shape:

```json
{
  "stopId": "F00985",
  "name": "Deak Ferenc ter M",
  "lat": 47.497701,
  "lon": 19.053353
}
```

### Search route

```http
POST /api/v1/routes/search
Content-Type: application/json
```

Request shape:

```json
{
  "originStopId": "F00985",
  "destinationStopId": "F00045",
  "departureDateTime": "2026-01-27T04:44:00+01:00",
  "optimization": "FASTEST",
  "transportTypes": ["BUS"]
}
```

Important current limitation:

- The ingestor maps GTFS `HH:mm:ss` times onto the service date resolved from `calendar_dates.txt` when available.
- For `budapest-mini.zip`, the stable demo service date is `2026-01-27`.

Response shape:

```json
[
  {
    "totalDuration": "PT6M",
    "totalPrice": 0,
    "segments": [
      {
        "from": {
          "stopId": "F00985",
          "name": "Deak Ferenc ter M",
          "lat": 47.497701,
          "lon": 19.053353
        },
        "to": {
          "stopId": "F00045",
          "name": "Donati utca",
          "lat": 47.501307,
          "lon": 19.036072
        },
        "timing": {
          "departureTime": "2026-01-27T04:44:00+01:00",
          "arrivalTime": "2026-01-27T04:50:00+01:00"
        },
        "transport": {
          "carrier": "BKK",
          "type": "BUS",
          "routeId": "0160"
        },
        "gtfs": {
          "tripId": "D075211",
          "shapeId": "CB58",
          "fromStopSequence": 1,
          "toStopSequence": 5
        },
        "geometry": [
          { "lat": 47.497678, "lon": 19.053365 },
          { "lat": 47.497669, "lon": 19.053327 }
        ]
      }
    ]
  }
]
```

The frontend renders route geometry by preferring each segment's `geometry` points and falling back to the `from`/`to` stop coordinates when geometry is missing.

## Demo stop pair

Use this pair for the first frontend integration:

```text
originStopId: F00985   Deak Ferenc ter M
destinationStopId: F00045   Donati utca
```

Why this pair:

- both stops exist in `budapest-mini.zip`;
- the route has 4 segments, which fits the current max segment limit;
- it is visually meaningful enough for a central Budapest map demo.

## `budapest-mini.zip` field completeness

The mini dataset keeps real BKK rows, not synthetic data.

Fields currently required by backend ingestion:

### `stops.txt`

Required and present for all 25 rows:

- `stop_id`
- `stop_name`
- `stop_lat`
- `stop_lon`

May be empty and is currently not used:

- `location_type`
- `location_sub_type`
- `parent_station`

### `stop_times.txt`

Required and present for all 26 rows:

- `trip_id`
- `stop_id`
- `arrival_time`
- `departure_time`
- `stop_sequence`

May be empty and is currently not used:

- `pickup_type`
- `drop_off_type`

### `routes.txt`

Useful for UI and currently exposed through `transport.routeId` when route metadata is available:

- `route_short_name` is present (`16`);
- `route_color` is present;
- `route_text_color` is present;
- `route_long_name` is empty.

### `trips.txt`

Present and useful for future shape/route metadata work:

- `trip_id`
- `route_id`
- `service_id`
- `trip_headsign`
- `direction_id`
- `shape_id`

### `shapes.txt`

Present for the selected trips, but not currently exposed by the API.
This can support a later Level 3 route visualization task.

## Current gaps / backend tasks

1. **Date handling in ingestor**
   - Resolved for the current MVP: `calendar_dates.txt` is used to derive a stable GTFS service date.
   - Future improvement: support full `calendar.txt` service ranges and per-trip service calendars.

2. **Route metadata**
   - Current API response does not expose `route_short_name`, route color, trip headsign, or shape id.
   - Not required for Level 1 polyline, but useful for UI polish.

3. **Map-scale stop loading**
   - Current `GET /locations` can return all rows.
   - Fine for mini data.
   - Full data likely needs bbox filtering and/or pagination strategy.

4. **Shape geometry**
   - Current response only has segment endpoint coordinates.
   - Good for Level 1 schematic polyline.
   - GTFS shapes should be separate future work.

## Frontend can start with

- `GET /api/v1/locations` for markers;
- mocked route response from this document;
- later real `POST /api/v1/routes/search` once local backend demo data is loaded.

## Backend should next verify

- imported `budapest-mini.zip` produces the documented demo route;
- CORS configuration if the web app runs on a different local port.

## Runtime readiness endpoint

The API exposes a readiness endpoint that reports whether the demo dataset is imported and usable by the UI.

```http
GET /api/v1/readiness
```

Ready response:

```http
200 OK
```

```json
{
  "datasetName": "budapest-mini",
  "ready": true,
  "status": "COMPLETED",
  "startedAt": "2026-07-05T18:00:00+04:00",
  "completedAt": "2026-07-05T18:01:00+04:00",
  "locationsCount": 25,
  "connectionsCount": 24,
  "errorMessage": null
}
```

Not-ready response:

```http
503 Service Unavailable
```

The UI can use this endpoint later to distinguish "API process is alive" from "demo data is ready for map/route usage".
