# budaTravel

Multi-module Kotlin/Spring Boot project for searching public transport routes.

## Modules

- `api`: HTTP API controllers, validation, OpenAPI.
- `core`: domain entities, repositories, routing/search logic.
- `common`: shared DTOs used by API and core.
- `ingestor`: GTFS import pipeline.
- `web-ui`: React/Vite/Leaflet MVP UI.

## Routing API

Routing contract and behavior are defined in [llmADR/ROUTING_API_SPEC.md](llmADR/ROUTING_API_SPEC.md).

Implemented endpoints:

- `GET /api/v1/locations`
- `GET /api/v1/locations/{stopId}`
- `GET /api/v1/locations/autocomplete`
- `POST /api/v1/routes/search`
- `GET /api/v1/readiness`

Route search business rules:

- max 3 transfers (max 4 segments);
- `FASTEST` and `CHEAPEST` optimization;
- no route result maps to `422 NO_ROUTE_FOUND`;
- connection segment duration `0 seconds` is normalized to `1 minute`.

## Test

Run all backend tests:

```bash
./gradlew test
```

Run web UI checks:

```bash
cd web-ui
npm ci
npm run lint
npm run build
```

## Local Docker demo

Start the demo stack:

```bash
docker compose up --build
```

Startup order:

```text
db -> ingestor -> api -> web-ui
```

- PostgreSQL waits for `pg_isready`.
- `ingestor` starts after DB is healthy and imports `budapest-mini` if it is not already ready.
- `api` starts after `ingestor` exits successfully.
- `web-ui` starts after `GET /api/v1/readiness` returns `200 OK`.

Open the UI:

```text
http://localhost:5173
```

Useful cleanup command:

```bash
docker compose down -v
```

If Flyway reports a non-empty schema without `flyway_schema_history`, the local Docker volume was probably created by an older experimental run. Reset the local demo database with:

```bash
docker compose down -v
docker compose up --build
```
