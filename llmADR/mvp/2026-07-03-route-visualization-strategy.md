# Route Visualization Strategy

## Status
Accepted

## Context

The MVP needs to display a backend-generated route on a map.
A fully realistic transport route geometry is more complex than the current routing MVP because it may require GTFS `shapes.txt`, trip/shape matching, walking transfers, and frontend geometry handling.

For the first visible MVP, correctness of the full visual geometry is less important than proving the end-to-end flow.

## Decision / Goal

Use incremental route visualization levels.

### Level 0 — route result list

Show textual route details without map geometry if needed for debugging.

### Level 1 — schematic stop-to-stop polyline

Draw a polyline using stop coordinates from route segments:

```text
segment.fromLat/fromLon -> segment.toLat/toLon
```

This may draw straight lines through buildings, but it is acceptable for the first MVP because it proves backend-to-frontend integration.

### Level 2 — segment-aware polyline

Still use stop coordinates, but draw each segment separately and style by transport type or route metadata.

### Level 3 — GTFS shapes

Use `shapes.txt` to draw actual route geometry where available.
This likely requires extending ingestion and API response contracts.

### Level 4 — walking transfers / advanced routing geometry

Add walking segments, nearby-stop transfers, and route geometry refinements later.

## Scope

For MVP, implement Level 1 first.

The current `RouteResponse` already contains segment endpoint coordinates, which should be enough for Level 1 rendering.

## Out of scope

- Perfect path geometry in the first UI iteration.
- Walking directions.
- Map matching.
- Multi-modal transfer visualization beyond simple segment lines.

## API implications

For Level 1, current route segment coordinates may be enough.

For Level 3, the API may need to expose route geometry, for example:

```json
{
  "geometry": [[lat, lon], [lat, lon]]
}
```

or encoded polyline format.

This should be introduced only after the simple MVP flow works.

## Parallel work model

Frontend can start with mocked route responses that use the current `RouteResponse` shape.
Backend can independently validate that route search returns stable demo routes from `budapest-mini.zip`.

## Acceptance criteria

- The MVP frontend can draw a route using the current route segment coordinates.
- Any future GTFS shape work is tracked as a separate story/task.
- The project does not block MVP UI work on perfect geometry.

## Result

Not implemented yet.
