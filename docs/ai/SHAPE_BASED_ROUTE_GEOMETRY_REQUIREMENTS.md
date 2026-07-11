# Shape-based Route Geometry Requirements

## Status

Implemented for MVP+ shape-based demo route geometry. Future improvements remain tracked as GTFS normalization/import-source tech debt.

## Problem

The current web UI draws route lines using stop coordinates from route segments. This proves the end-to-end flow, but the line is schematic and can cut through the city, buildings, or the Danube.

GTFS provides `shapes.txt`, which contains ordered geographic points for real vehicle paths. The BKK source archives and the current `budapest-mini.zip` both contain populated shape data.

We need to use this data so the route line follows the actual public transport path where possible.

## Goal

For a found route, return map geometry based on GTFS shapes and render it in the web UI.

The MVP+ goal is not perfect Google Maps-level navigation. The goal is:

> If the backend knows the trip/connection used by a route segment, and that trip has `shape_id`, the API should return the relevant shape polyline instead of only stop-to-stop coordinates.

## Confirmed source data

The mini dataset contains real BKK shape data:

- `trips.txt` has `shape_id`.
- `stop_times.txt` has `shape_dist_traveled`.
- `shapes.txt` has ordered points with `shape_id`, `shape_pt_sequence`, `shape_pt_lat`, `shape_pt_lon`, `shape_dist_traveled`.

Known demo route:

```text
origin:      F00985 — Deák Ferenc tér M
destination: F00045 — Donáti utca
trip_id:     D075211
shape_id:    CB58
shape range: 0.0 -> 1943.0
```

For this demo route, `CB58` contains roughly 104 shape points in the relevant distance range.

## Functional requirements

### FR-1 — Import GTFS trip metadata

During GTFS import, the system must read `trips.txt` and preserve at least:

- `trip_id`
- `route_id`
- `shape_id`

This can initially be an in-memory map during import, but the resulting connection data must retain enough metadata to resolve shape geometry later.

### FR-2 — Import stop-time shape distances

During `stop_times.txt` import, the system must read `shape_dist_traveled` when present.

For each generated `Connection` between consecutive stops, preserve:

- `trip_id`
- `route_id`
- `shape_id`
- `from_stop_sequence`
- `to_stop_sequence`
- `from_shape_dist_traveled`
- `to_shape_dist_traveled`

### FR-3 — Import GTFS shape points

The system must import `shapes.txt` into a queryable structure.

Required fields:

- `shape_id`
- `shape_pt_sequence`
- `lat`
- `lon`
- `shape_dist_traveled`

For MVP+, a database table is preferred because the API needs to resolve geometry after ingestion is complete.

### FR-4 — Resolve segment geometry

For a route segment based on a `Connection`, the backend must try to resolve shape geometry:

1. Get `shape_id` from the connection.
2. Get `from_shape_dist_traveled` and `to_shape_dist_traveled`.
3. Query shape points for that `shape_id` between those distances.
4. Return ordered points as segment geometry.

If distances are missing or invalid, the backend may fall back to sequence-based or stop-to-stop geometry.

### FR-5 — Preserve fallback behavior

If shape geometry cannot be resolved, the API must still return enough information for the frontend to draw the current stop-to-stop polyline.

Shape support must not break the existing demo route behavior.

### FR-6 — Extend route API response

The route API response should include optional geometry.

Implemented DTO shape:

```json
{
  "segments": [
    {
      "from": { "stopId": "F00985", "name": "Deak Ferenc ter M", "lat": 47.497701, "lon": 19.053353 },
      "to": { "stopId": "F00045", "name": "Donati utca", "lat": 47.501307, "lon": 19.036072 },
      "gtfs": { "tripId": "D075211", "shapeId": "CB58", "fromStopSequence": 1, "toStopSequence": 5 },
      "geometry": [
        { "lat": 47.497678, "lon": 19.053365 },
        { "lat": 47.497669, "lon": 19.053327 }
      ]
    }
  ]
}
```

Alternative later optimization: encoded polyline string. Do not use it for the first implementation unless response size becomes a problem.

### FR-7 — Render shape geometry in web UI

The frontend must prefer returned geometry points when present.

Fallback order:

1. Segment geometry from API.
2. Existing segment endpoint coordinates.

### FR-8 — Support the known demo route

The known demo route must render using shape geometry:

```text
F00985 -> F00045
```

Acceptance expectation: the route line follows the street/transport path from `CB58` and no longer draws only a sparse stop-to-stop line.

## Non-functional requirements

### NFR-1 — Keep local demo fast

The MVP+ implementation must remain fast with `budapest-mini.zip`.

Full archive performance can be optimized later, but the model should not make full import obviously impossible.

### NFR-2 — Avoid over-engineering

Do not implement full multimodal routing, walking paths, realtime BKK data, or scheduled import as part of this feature.

### NFR-3 — Keep API backward-compatible where practical

`RouteSegment` was intentionally changed to a nested, self-describing response structure (`from`, `to`, `timing`, `transport`, `gtfs`, `geometry`) before public API stability was required.

### NFR-4 — Deterministic fallback

If shape data is missing, malformed, or not linked, the route endpoint should still return a route using existing segment coordinates.

## Data model proposal

### Extend `connections`

Add nullable columns:

```sql
trip_id VARCHAR(255),
route_id VARCHAR(255),
shape_id VARCHAR(255),
from_stop_sequence INTEGER,
to_stop_sequence INTEGER,
from_shape_dist_traveled DOUBLE PRECISION,
to_shape_dist_traveled DOUBLE PRECISION
```

Nullable fields keep old/demo data resilient and allow fallback behavior.

### Add `gtfs_shape_points`

Proposed table:

```sql
CREATE TABLE gtfs_shape_points (
    id UUID PRIMARY KEY,
    shape_id VARCHAR(255) NOT NULL,
    shape_pt_sequence INTEGER NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lon DOUBLE PRECISION NOT NULL,
    shape_dist_traveled DOUBLE PRECISION
);

CREATE INDEX ix_gtfs_shape_points_shape_sequence
    ON gtfs_shape_points(shape_id, shape_pt_sequence);

CREATE INDEX ix_gtfs_shape_points_shape_dist
    ON gtfs_shape_points(shape_id, shape_dist_traveled);
```

For full datasets, shape tables may become large. This is acceptable for MVP+ but should be measured later.

## Implementation outline

1. Add migration for connection metadata and shape points.
2. Add core entity/repository for shape points.
3. Extend GTFS DTOs:
   - `GtfsTrip.shape_id`
   - `GtfsStopTime.shape_dist_traveled`
   - new `GtfsShapePoint`.
4. Import `trips.txt` into `Map<tripId, tripMetadata>`.
5. Import `shapes.txt` before/after stop times.
6. Save connection metadata while processing consecutive stop times.
7. Extend route DTO with optional geometry points.
8. Resolve geometry while mapping `RoutingPath` to `RouteResponse`.
9. Update frontend polyline logic to prefer geometry.
10. Add tests for:
    - import metadata mapping;
    - shape point query by `shape_id` and distance range;
    - route response includes geometry for demo path;
    - fallback still works without geometry.

## Open questions

1. Should geometry live per segment or at route level?
   - MVP recommendation: per segment first, because the backend already returns route segments.
2. Should the API return `{ lat, lon }` objects or `[lat, lon]` tuples?
   - MVP recommendation: `{ lat, lon }` for readability and DTO friendliness.
3. How should we cut shape segments when `shape_dist_traveled` is absent?
   - MVP recommendation: fallback to stop-to-stop geometry; advanced nearest-point matching later.
4. Do we need to persist `trips` as a separate table?
   - MVP recommendation: not yet. Store needed trip metadata directly on `connections`.
5. Should full import use bulk inserts/copy for shape points?
   - Later. Mini dataset does not need it; full archive likely will.

## Acceptance criteria

- `budapest-mini.zip` import stores shape points for `CB58` and `DX30`.
- Connections generated from trip `D075211` store `trip_id = D075211` and `shape_id = CB58`.
- The route API returns geometry for demo route `F00985 -> F00045`.
- The geometry contains significantly more than only origin/destination points.
- The frontend draws returned geometry when available.
- Existing stop-to-stop route rendering remains as fallback.
- Tests pass with `./gradlew test`.
