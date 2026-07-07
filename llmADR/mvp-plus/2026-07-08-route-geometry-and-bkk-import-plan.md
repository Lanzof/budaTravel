# MVP+ Route Geometry and BKK Import Plan

## Status
Proposed

## Context

The first technical MVP is complete: the project can start a local demo stack, import a small GTFS dataset, expose stops and routes through the API, and render stops plus a demo route in the web UI.

The current route visualization is intentionally schematic: the frontend draws a polyline through route segment stop coordinates. This proves the end-to-end flow, but it can draw visually incorrect lines, for example cutting across the Danube instead of following the actual vehicle path.

The next phase should avoid speculative work. Before implementing more route visualization or import features, we verified the available GTFS archives:

- `budapest_gtfs.zip` contains a populated `shapes.txt` with approximately 617k rows and `trips.txt.shape_id`.
- `improved-gtfs-volanbusz.zip` contains a populated `shapes.txt` with approximately 7.3M rows and `trips.txt.shape_id`.
- `budapest-mini.zip` contains a populated `shapes.txt` with approximately 610 rows and `trips.txt.shape_id`.

Therefore, shape-based route geometry is based on real data we already have, not on an assumption.

## Decision / Goal

Define the next phase as **MVP+ / Usable Route Demo**.

The goal is to turn the technical proof of life into a minimally useful route demo:

> A user can see stops, choose origin and destination, build a route, and see a route line that follows GTFS route geometry where possible.

This phase should still run quickly on the mini dataset. Full scheduled import from BKK should be researched and designed, but not made a prerequisite for the route geometry work.

## Scope

### 1. Shape-based route geometry

Use GTFS `shapes.txt` to improve route visualization.

Expected direction:

1. Import shape points from `shapes.txt`.
2. Preserve the relationship from `trips.txt`:
   - `trip_id -> shape_id`.
3. When route search returns a path based on trip/connection data, resolve the relevant shape geometry.
4. Return route geometry in the API response.
5. Let the web UI draw geometry points instead of only stop-to-stop straight lines.

The first implementation can be pragmatic:

- support the demo mini dataset first;
- support single-trip or simple direct route geometry first;
- fall back to stop-to-stop polyline when shape data cannot be resolved.

### 2. UI route selection improvements

Keep improving the user-facing route flow:

- search/select origin and destination stops;
- choose by stop ID/name or map click;
- show readable errors for no-route cases;
- keep the demo route button as a fast sanity check.

### 3. BKK API / data source research

Do a short research task before implementation.

Questions to answer:

- Does the BKK API return full GTFS archives or incremental data?
- What is the archive size?
- Does the endpoint expose versioning, `ETag`, `Last-Modified`, checksum, or timestamp metadata?
- How often is the data expected to change?
- Is an API key always required?
- Can we avoid re-downloading and re-importing identical archives?

The outcome should be a small ADR or tech plan for import source strategy.

### 4. Scheduled import later

Scheduled import should be implemented after the data source strategy is clear.

Expected direction:

- `GtfsArchiveProvider` abstraction;
- bundled demo archive provider;
- local file provider;
- remote BKK archive provider;
- download cache;
- import only if data changed;
- import status table updates;
- avoid breaking the active dataset during failed import, possibly with staging tables or an import generation marker.

## Non-goals for MVP+

MVP+ does not attempt to become Google Maps.

Out of scope for this phase:

- full realtime BKK integration;
- production-grade scheduled import;
- walking navigation;
- advanced transfer optimization;
- map matching;
- mobile Android client;
- public deployment.

## Rationale

Starting with BKK API integration is tempting, but it increases feedback-loop cost. If the API returns large archives, every local experiment may become:

```text
download tens of MB -> unpack -> import -> test
```

That is too heavy for quick MVP+ development.

Shape-based geometry, on the other hand, solves a visible user-facing problem while still using the fast mini dataset. Since `shapes.txt` is present in both full and mini archives, work done on the mini dataset should transfer naturally to full GTFS import later.

## Risks

- Shape points can be large in full datasets; storage/indexing needs care later.
- A route path may involve multiple trips/transfers, so one shape may not be enough.
- Cutting a shape segment between origin and destination stops can be non-trivial if stop-to-shape distances are noisy.
- The current data model may not preserve enough GTFS identifiers to resolve shapes cleanly.

## Acceptance criteria

MVP+ route geometry is considered successful when:

- shape data from `budapest-mini.zip` is imported or otherwise available to the backend;
- the route API can return geometry for the known demo route;
- the web UI draws the demo route without cutting directly through the Danube when shape geometry is available;
- stop-to-stop polyline remains available as a fallback;
- BKK API scheduled import is documented as a later staged feature rather than mixed into the first geometry implementation.

## Follow-up stories

- Import GTFS shapes from `shapes.txt`.
- Store `trip_id -> shape_id` metadata.
- Extend route response DTO with optional route geometry.
- Render route geometry in web UI.
- Research BKK API archive metadata and caching strategy.
- Design remote archive provider and scheduled import flow.
