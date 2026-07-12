# MVP+ Task Tree

## Goal

Move from a technical MVP to a usable route demo while keeping local development fast.

Primary scenario:

> Start the stack, open the map, choose origin/destination, build a route, see readable route details and route geometry that follows GTFS shapes when available.

## Epic 8 — MVP+ usable route demo

### Story 8.1 — Shape-based route geometry

#### Task 8.1.1 — Verify source data
- [x] Confirm full BKK GTFS archive has populated `shapes.txt`.
- [x] Confirm `budapest-mini.zip` has populated `shapes.txt`.
- [x] Confirm `trips.txt.shape_id` exists.
- [x] Confirm `stop_times.txt.shape_dist_traveled` exists.
- [x] Identify demo route shape: `D075211` -> `CB58`.

#### Task 8.1.2 — Extend GTFS import metadata
- [x] Add `shape_id` to GTFS trip parsing.
- [x] Add `shape_dist_traveled` to GTFS stop-time parsing.
- [x] Build `trip_id -> route_id/shape_id` metadata during import.
- [x] Store trip/shape metadata on generated connections.

#### Task 8.1.3 — Persist shape points
- [x] Add migration for `gtfs_shape_points`.
- [x] Add entity/repository for shape points.
- [x] Import `shapes.txt` points from mini archive.
- [x] Add indexes by `shape_id + sequence` and `shape_id + distance`.

#### Task 8.1.4 — Resolve geometry in backend
- [x] Query shape points for a connection by `shape_id` and distance range.
- [x] Add fallback to stop endpoints when shape geometry is missing.
- [x] Extend route DTO with optional geometry points.
- [x] Add tests for shape geometry resolution.

#### Task 8.1.5 — Render geometry in web UI
- [x] Prefer backend geometry when present.
- [x] Keep current stop-to-stop polyline fallback.
- [x] Verify demo route no longer visually cuts through the Danube in local Docker demo.

### Story 8.2 — Better route selection UX

#### Task 8.2.1 — Stop search/autocomplete
- [x] Add API support if current location endpoint is not enough.
- [x] Add frontend search inputs for origin/destination.
- [x] Support selecting a suggestion.
- [x] Preserve stop ID visibility for debugging.

#### Task 8.2.1.1 — Accent-insensitive stop search
- [x] Add normalized stop name storage for search without production backfill.
- [x] Normalize imported stop names by lowercasing, removing diacritics, and collapsing separators.
- [x] Search autocomplete by normalized query so `Deak Ferenc` can match `Deák Ferenc`.
- [x] Keep this as a Postgres-local MVP solution before considering trigram/OpenSearch.

#### Task 8.2.2 — Map selection
- [x] Allow clicking a stop marker to set origin or destination.
- [x] Highlight selected origin/destination markers.
- [x] Add swap origin/destination action.

#### Task 8.2.3 — Route details panel
- [x] Show route duration and price if present.
- [x] Show stop sequence / segment list.
- [x] Show route/transport metadata when available.
- [x] Keep readable no-route and validation errors.

### Story 8.2.5 — Web UI layout polish

The current route/details sidebar is useful for debugging, but it mixes product controls with MVP/debug information. Future UI cleanup should make the route-building flow more stable and readable.

#### Task 8.2.5.1 — Move technical/demo info to the header area
- [x] Move MVP description, API endpoint hint, loaded stops count, loading status, and demo route action from the right sidebar to the top/header area near `budaTravel map MVP`.
- [x] Keep this block compact so it does not compete with the map.
- [x] Preserve demo route as a fast regression/sanity action for local MVP checks.

#### Task 8.2.5.2 — Reorder the right sidebar around user flow
- [x] Show selected stop first.
- [x] Keep a stable selected-stop placeholder when nothing is selected so the panel does not jump after marker clicks.
- [x] Show origin/destination route controls after the selected stop block.
- [x] Show route summary after route controls.
- [x] Show route segments after route summary.

#### Task 8.2.5.3 — Separate product UI from debug metadata
- [x] Keep route/shape/trip IDs available while the project is in MVP/debug mode.
- [x] Move verbose technical metadata behind a collapsible debug/details section.
- [x] Avoid letting debug fields dominate the route-building workflow.

### Story 8.3 — BKK API/data source research

#### Task 8.3.1 — BKK endpoint research
- [ ] Identify exact BKK GTFS endpoint/API flow.
- [ ] Determine whether response is full archive or incremental data.
- [ ] Measure archive size.
- [ ] Check auth requirements and whether API key is mandatory.

#### Task 8.3.2 — Cache/version research
- [ ] Check `ETag` support.
- [ ] Check `Last-Modified` support.
- [ ] Check checksum/version/timestamp metadata.
- [ ] Decide how to detect unchanged data.
- [ ] Decide whether BKK imports should replace the active dataset, keep one rollback generation, or retain history; avoid stale routes bloating the database.

#### Task 8.3.3 — Document remote import strategy
- [ ] Write ADR/tech plan for BKK remote archive import.
- [ ] Define env vars: `BKK_API_KEY`, `BKK_GTFS_URL`, cache dir.
- [ ] Define behavior for missing/invalid key.
- [ ] Define import-if-changed flow.

### Story 8.4 — Ingestor profiles and source abstraction

#### Task 8.4.1 — Profile/source configuration
- [ ] Add `budatravel.gtfs.source` property.
- [ ] Add `demo` profile using bundled mini archive.
- [ ] Add local-file source config using `GTFS_ARCHIVE_PATH`.
- [ ] Reserve BKK remote config using `BKK_GTFS_URL` and `BKK_API_KEY`.

#### Task 8.4.2 — Archive provider abstraction
- [ ] Introduce `GtfsArchiveProvider` or equivalent.
- [ ] Implement classpath/demo provider.
- [ ] Implement local file provider.
- [ ] Keep BKK remote provider as separate later task.
- [ ] Remove temporary-file workaround once importer accepts `Resource`/stream/archive abstraction.

#### Task 8.4.3 — Scheduled import design
- [ ] Decide one-shot vs long-running scheduler mode.
- [ ] Define import status transitions for skipped/unchanged data.
- [ ] Design failure behavior without breaking active data.
- [ ] Consider staging/import-generation model.

### Story 8.5 — E2E and demo quality

#### Task 8.5.1 — Docker smoke test
- [x] Add a scripted smoke test for an already started Docker Compose demo stack.
- [x] Manually verify Docker Compose demo startup and demo route rendering after shape geometry implementation.
- [x] Check API readiness.
- [x] Check web UI is reachable.
- [x] Call demo route API for the known demo route.

#### Task 8.5.2 — Browser E2E
- [ ] Add Playwright or equivalent later.
- [ ] Open web UI.
- [ ] Click demo route.
- [ ] Assert route line appears.

#### Task 8.5.2.1 — Web UI component tests
- [ ] Decide when to introduce Vitest + React Testing Library for web UI behavior.
- [ ] Cover stable route-planning interactions first: demo route fill, autocomplete selection, swap origin/destination, disabled route search states, and route summary reset.
- [ ] Keep browser smoke checks for layout/map integration; use component tests for deterministic UI state transitions.

#### Task 8.5.3 — Portfolio/demo docs
- [ ] Add screenshots or GIF.
- [ ] Document one-command demo path.
- [ ] Document known demo route and expected result.

## Suggested implementation order

1. Shape metadata + shape point import.
2. Backend route geometry response.
3. Web UI geometry rendering.
4. Stop search, route details UX, and layout polish.
5. BKK API/data source research.
6. Ingestor source profiles and provider abstraction.
7. Scheduled import design/implementation.
8. E2E smoke/demo polish.


### Story 8.6 — Next-stage realtime and geocoding experiments

These items are intentionally postponed until after the current shape-geometry MVP+ work.

#### Task 8.6.1 — BKK realtime research
- [ ] Research BKK GTFS-Realtime endpoints and response formats.
- [ ] Determine whether BKK exposes protobuf-over-HTTP feeds or actual gRPC services.
- [ ] Evaluate live vehicle positions as a WebClient/WebFlux + SSE experiment.
- [ ] Decide whether realtime positions should be rendered on the map.

#### Task 8.6.2 — OSM geocoding research
- [ ] Compare Nominatim, Photon, and Pelias-like geocoding options for Budapest address/place search.
- [ ] Check public API limits and acceptable usage policies.
- [ ] Design geocoding-to-nearest-stop flow.
- [ ] Decide whether geocoding belongs in product roadmap or remains an experiment.

### Story 8.7 — GTFS model normalization tech debt

Current state: `connections` is an internal routing graph projection. For MVP+, it also stores minimal GTFS metadata directly on each edge so route geometry can resolve the matching `shapes.txt` slice.

This is acceptable for the mini dataset and shape-geometry MVP+, but it should not become the final GTFS storage model.

#### Task 8.7.1 — Design normalized GTFS storage
- [ ] Design `gtfs_routes`, `gtfs_trips`, `gtfs_stop_times`, and related source-of-truth tables.
- [ ] Keep `connections` / graph edges as a derived routing projection, not the only GTFS storage.
- [ ] Include dataset/import generation identifiers for future full/scheduled imports.
- [ ] Decide how route metadata, calendar/service data, and realtime updates link to routing edges.

#### Task 8.7.2 — Migration plan
- [ ] Define how to backfill or rebuild `connections` from normalized GTFS tables.
- [ ] Decide whether GTFS metadata columns remain on `connections` as denormalized cache fields or move behind joins.
- [ ] Revisit this before implementing full BKK scheduled import.
