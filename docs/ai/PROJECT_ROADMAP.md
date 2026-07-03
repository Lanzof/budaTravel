# budaTravel MVP Roadmap

## Project framing

`budaTravel` is a pet/portfolio project for learning Kotlin, Gradle, databases, routing, infrastructure, and AI-assisted development.

The near-term goal is **not** to compete with Google Maps. The near-term goal is an end-to-end demo:

> Start backend + DB + UI, open a map, see Budapest stops, select two stops, request a route from our backend, and render the result.

## Current state

- Multi-module Kotlin/Spring Boot project.
- Modules:
  - `api` — REST API, validation, OpenAPI, error handling.
  - `core` — entities, repositories, routing/search logic.
  - `common` — shared DTOs.
  - `ingestor` — GTFS import pipeline.
- PostgreSQL via Docker Compose.
- Routing MVP exists:
  - max 3 transfers / 4 segments;
  - `FASTEST` and `CHEAPEST` optimization;
  - returns route segments.
- Tests pass with JDK 21:
  - `./gradlew test` → `BUILD SUCCESSFUL`.

## Recommended MVP target

### MVP v0: visual proof of life

User story:

1. Start database and API.
2. Open UI.
3. See map centered on Budapest.
4. See imported stops as markers.
5. Click/select origin and destination stops.
6. Press “Build route”.
7. See one or more returned route options.
8. See selected route drawn on the map.

This is enough for a strong portfolio milestone because it demonstrates full-stack integration.

## Architectural direction

### Map rendering

Use an external map library only as the **map substrate**:

- Web MVP: Leaflet or MapLibre + OpenStreetMap tiles.
- Android later: Google Maps SDK, MapLibre, or another map SDK.

The backend should still own:

- stop data;
- route search;
- route alternatives;
- transport-domain semantics.

Avoid using Google Directions as the main routing engine for this project, otherwise the backend loses much of its learning value.

### Route visualization levels

Do this incrementally:

1. **Level 1 — straight polyline between stops**
   - Good enough for MVP.
   - May visually cut through buildings, but proves integration.

2. **Level 2 — stop-to-stop segment lines**
   - Draw each route segment between consecutive stops.
   - Better structure, still schematic.

3. **Level 3 — GTFS shapes**
   - Use `shapes.txt` if available.
   - Draw real vehicle path geometry.

4. **Level 4 — walking transfers / map matching**
   - Optional, later.

## Data strategy

### Problem

Full GTFS archives are large and slow to ingest locally. Current local import can take 15–30 minutes.

### Recommendation

Create a small, consistent demo dataset:

```text
sample-data/budapest-mini/
```

It should contain:

- a small subset of stops;
- a few routes/trips;
- valid relationships between stops, trips, stop times, routes, calendars;
- enough data to build at least several meaningful route examples;
- import time measured in seconds, not minutes.

This is what I meant by “seed”: a small known dataset for local demo/dev/test.

### Later

Keep full GTFS import as a separate path:

- manual import first;
- scheduled import later;
- eventually CI/deploy-friendly import flow.

## Backend/API roadmap

### Phase 1 — stabilize current MVP

- Confirm local run path:
  - DB starts;
  - API starts;
  - ingestor can populate DB;
  - tests pass.
- Document local startup in README.
- Make sure `/api/v1/locations` works for map markers.
- Make sure `/api/v1/routes/search` returns enough geometry data for UI.

### Phase 2 — UI-friendly API additions

Potential endpoints:

```http
GET /api/v1/locations?bbox=...
GET /api/v1/locations/{stopId}
GET /api/v1/locations/autocomplete?query=...
POST /api/v1/routes/search
```

For MVP, route response should include:

- total duration;
- total price if relevant;
- segments;
- segment start/end stop coordinates;
- departure/arrival times;
- transport type;
- carrier/route label if available.

Later it may include:

- polyline geometry;
- route color;
- transfer instructions;
- walking segments.

### Phase 3 — routing correctness

Later improvements:

- waiting time between transfers;
- GTFS calendar/service days;
- trip grouping;
- walking transfers between nearby stops;
- Dijkstra/A*/time-expanded graph;
- shape-aware route geometry.

## Frontend recommendation

For the first UI, prefer **web** over Android.

Reason:

- faster visible result;
- easier debugging;
- easier portfolio demo;
- simpler map rendering with Leaflet/MapLibre;
- avoids Android framework complexity while backend/API are still moving.

Suggested minimal frontend:

```text
web/
  Vite + TypeScript
  Leaflet or MapLibre
```

MVP UI screens:

- one map page;
- stop markers;
- origin/destination selection;
- route search button;
- route result list;
- selected route polyline.

Android can become the second client after the backend contract stabilizes.

## Infrastructure roadmap

### Phase 1

- `docker-compose` for Postgres + API.
- Ingestor run separately as a manual command.
- README with exact commands.

### Phase 2

- GitHub Actions:
  - run tests;
  - build project;
  - maybe build Docker image.

### Phase 3

- Optional deployment:
  - backend to a small VPS/Fly.io/Render/etc.;
  - DB managed or containerized;
  - frontend static deploy.

### Phase 4

- Kubernetes only if it remains interesting as a learning goal.
- Do not introduce it before the MVP is visible.

## Suggested first issues

1. Document local development startup.
2. Create `budapest-mini` GTFS sample dataset.
3. Add/verify endpoint for map stop markers.
4. Add route response fields needed by UI.
5. Create minimal web map prototype.
6. Draw stops on map.
7. Select origin/destination from map.
8. Call route search API from UI.
9. Draw returned route as polyline.
10. Add GitHub Actions test pipeline.

## Suggested first PR

The best first PR is probably **not** a big feature.

Recommended first PR:

> Improve local developer setup: document JDK 21, Docker Compose DB, API startup, ingestor startup, and `./gradlew test`.

Why:

- low risk;
- immediately useful;
- creates a reliable baseline;
- helps future AI-assisted work because the project has clear commands.

Second PR:

> Add a tiny demo dataset or a script for producing one.

Third PR:

> Add minimal web UI map prototype.
