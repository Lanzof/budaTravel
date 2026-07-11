# MVP Backlog

## Epic 1 — Public-ready repository

### Story 1.1 — Add license and data attribution
- [x] Add Apache-2.0 project license.
- [x] Add BKK data attribution.
- [x] Document BKK CC BY 4.0 source license.

### Story 1.2 — Replace full GTFS archives with demo data
- [x] Create small consistent `budapest-mini.zip`.
- [x] Make ingestor use mini archive by default.
- [x] Remove full GTFS archives from tracked repository state.
- [ ] Decide whether git history should be rewritten before switching repository to public.

## Epic 2 — MVP scope and planning

### Story 2.1 — Preserve decisions as ADRs
- [x] Add demo data strategy ADR.
- [x] Add MVP scope and delivery plan ADR.
- [ ] Add UI direction ADR: web-first vs Android-first.
- [ ] Add route visualization ADR.

## Epic 3 — Backend local development baseline

### Story 3.1 — Document local setup
- [x] Document JDK 21 requirement.
- [x] Document DB startup.
- [x] Document API startup.
- [x] Document ingestor startup.
- [x] Document test command.

### Story 3.2 — Add CI
- [x] Add GitHub Actions workflow for `./gradlew test`.
- [x] Run CI on pushes/PRs to `ai-features` and `master`.

## Epic 4 — Map API readiness

### Story 4.1 — Stops on map
- [x] Verify `GET /api/v1/locations` response for map markers.
- [x] Evaluate whether bbox filtering is needed.
- [x] Add bbox filtering if needed.

### Story 4.2 — Route response for UI
- [x] Verify `POST /api/v1/routes/search` with mini dataset.
- [x] Confirm response includes enough coordinates/geometry for a route polyline.
- [x] Document demo stop pairs.

## Epic 5 — Web MVP

### Story 5.1 — Web skeleton
- [x] Add `web-ui` app.
- [x] Choose Leaflet.
- [x] Render Budapest map.

### Story 5.2 — Stops and routes
- [x] Load stops from backend.
- [x] Draw stop markers.
- [x] Select origin and destination from map markers/demo route.
- [x] Call route search.
- [x] Draw returned route as a polyline.

### Story 4.3 — API/UI contract sync
- [x] Verify `budapest-mini.zip` field completeness for all data used by backend and UI.
- [x] Document which GTFS fields may be empty and which are required for MVP.
- [x] Define demo origin/destination stop IDs for UI development.
- [x] Capture a sample `POST /api/v1/routes/search` request and response.
- [x] Add UI route fields: nested stops/timing/transport/GTFS metadata and geometry.
- [x] Make GTFS demo import use a stable service date from `calendar_dates.txt`.

## Epic 6 — Demo runtime orchestration

### Story 6.1 — Data readiness model
- [x] Capture ADR for data readiness and demo runtime orchestration.
- [x] Add import status schema for dataset import state.
- [x] Make ingestor write RUNNING / COMPLETED / FAILED status.
- [x] Make API readiness depend on imported demo data.

### Story 6.2 — Dockerized local demo
- [x] Add Docker Compose flow for db -> ingestor -> api -> web-ui.
- [x] Ensure web UI waits for API readiness rather than ingestor directly.
- [x] Document one-command or few-command local MVP startup.

## Epic 7 — GTFS import source cleanup

### Story 7.1 — Archive source abstraction
- [ ] Introduce a `GtfsArchiveProvider` or equivalent abstraction for GTFS archive sources.
- [ ] Let ingestion work with Spring `Resource` or `InputStream` instead of requiring a filesystem path.
- [ ] Support bundled demo archive, mounted local file, and future downloaded BKK archive through one flow.
- [ ] Remove the temporary-file workaround from `DataInit` after `GtfsService` no longer requires `String zipFilePath`.


## Epic 8 — MVP+ usable route demo

### Story 8.1 — Shape-based route geometry
- [x] Verify that real GTFS archives and mini dataset contain populated `shapes.txt`.
- [x] Import GTFS shape points from `shapes.txt`.
- [x] Preserve `trip_id -> shape_id` from `trips.txt`.
- [x] Resolve shape geometry for the known demo route.
- [x] Extend route API response with optional geometry points.
- [x] Render shape-based geometry in web UI when available.
- [x] Keep stop-to-stop polyline as fallback.

### Story 8.2 — Better route selection UX
- [x] Add stop search/autocomplete to choose origin and destination.
- [x] Support accent-insensitive stop search through derived `name_normalized` storage.
- [x] Support selecting stops from the map.
- [x] Support swapping origin and destination.
- [x] Keep the demo route button as a regression/sanity shortcut.
- [x] Show route details in a readable way, not only a line on the map.

### Story 8.2.5 — Web UI layout polish
- [x] Move MVP/debug info, endpoint hint, loaded stops count, status, and demo route action to the top/header area.
- [x] Keep the right sidebar focused on selected stop, origin/destination, route summary, and route segments.
- [x] Reorder the sidebar: selected stop placeholder first, then route controls, then route summary, then segment list.
- [x] Keep selected-stop placeholder stable so the interface does not jump when a marker is selected.
- [x] Move verbose route/GTFS debug metadata behind a collapsible details section.

### Story 8.3 — BKK API/data source research
- [ ] Determine whether BKK API returns full archives or incremental data.
- [ ] Check archive size, update frequency, and required auth.
- [ ] Check whether BKK responses provide `ETag`, `Last-Modified`, checksum, timestamp, or version metadata.
- [ ] Decide active dataset replacement/history policy for BKK imports so old routes do not bloat the database or affect route search.
- [ ] Document download-cache and import-if-changed strategy.
- [ ] Decide how API keys are provided locally and in CI/deploy environments.

### Story 8.4 — Ingestor profiles and source abstraction
- [ ] Add `budatravel.gtfs.source` configuration: demo / local-file / bkk-remote.
- [ ] Add Spring profile defaults for demo and future BKK import.
- [ ] Introduce `GtfsArchiveProvider` or equivalent.
- [ ] Implement bundled demo archive provider.
- [ ] Implement local file archive provider.
- [ ] Design BKK remote provider with cache/import-if-changed behavior.
- [ ] Implement scheduled import only after the BKK data source research is complete.

### Story 8.5 — E2E and demo quality
- [ ] Add Docker Compose smoke test for demo stack startup.
- [x] Manually verify Docker demo locally: demo route renders correctly with shape-based geometry.
- [ ] Add route API smoke check for the known demo route.
- [ ] Add browser E2E later: open UI, click demo route, verify route line.
- [ ] Add screenshots/GIF and one-command demo documentation.


### Story 8.6 — Next-stage realtime/geocoding experiments
- [ ] Research BKK GTFS-Realtime / live vehicle positions for a later WebClient/WebFlux + SSE experiment.
- [ ] Check whether BKK realtime is protobuf-over-HTTP or actual gRPC.
- [ ] Research OpenStreetMap geocoding options: Nominatim, Photon, Pelias-like services.
- [ ] Design address/place search -> nearest stop flow for a later stage.

### Story 8.7 — GTFS model normalization tech debt
- [ ] Design normalized GTFS source-of-truth tables for routes, trips, stop times, calendars, and dataset generations.
- [ ] Keep `connections` as a derived routing graph/projection.
- [ ] Revisit direct GTFS metadata fields on `connections` before full BKK scheduled import.
- [ ] Decide whether connection GTFS metadata remains as denormalized cache or moves behind joins.
