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
- [ ] Document JDK 21 requirement.
- [ ] Document DB startup.
- [ ] Document API startup.
- [ ] Document ingestor startup.
- [ ] Document test command.

### Story 3.2 — Add CI
- [ ] Add GitHub Actions workflow for `./gradlew test`.
- [ ] Run CI on pushes/PRs to `ai-features` and `master`.

## Epic 4 — Map API readiness

### Story 4.1 — Stops on map
- [ ] Verify `GET /api/v1/locations` response for map markers.
- [ ] Evaluate whether bbox filtering is needed.
- [ ] Add bbox filtering if needed.

### Story 4.2 — Route response for UI
- [ ] Verify `POST /api/v1/routes/search` with mini dataset.
- [ ] Confirm response includes enough coordinates for a schematic route polyline.
- [ ] Document demo stop pairs.

## Epic 5 — Web MVP

### Story 5.1 — Web skeleton
- [ ] Add `web/` module/app.
- [ ] Choose Leaflet or MapLibre.
- [ ] Render Budapest map.

### Story 5.2 — Stops and routes
- [ ] Load stops from backend.
- [ ] Draw stop markers.
- [ ] Select origin and destination.
- [ ] Call route search.
- [ ] Draw returned route as a polyline.

### Story 4.3 — API/UI contract sync
- [x] Verify `budapest-mini.zip` field completeness for all data used by backend and UI.
- [x] Document which GTFS fields may be empty and which are required for MVP.
- [x] Define demo origin/destination stop IDs for UI development.
- [x] Capture a sample `POST /api/v1/routes/search` request and response.
- [ ] Decide whether route/search DTO needs extra UI fields before frontend implementation.
