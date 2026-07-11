# Local Dev Flow — MVP

## Purpose

This document captures the current local workflow for the MVP demo.
It covers both the Docker Compose end-to-end flow and the Gradle/npm split flow for development.

## Requirements

- JDK 21
- Docker / Docker Compose
- Git

The project uses the Gradle wrapper, so a system Gradle install is not required.

## Run tests

```bash
./gradlew test --no-daemon
```

This compiles the multi-module backend and runs the current test suite.

## Start PostgreSQL

For local API/ingestor experiments, start only the database service:

```bash
docker compose up -d db
```

The default local database settings are currently:

```text
url: jdbc:postgresql://localhost:5432/route_finder_db
username: route_user
password: route_pass
```

These are local development credentials only.

## Import demo GTFS data

The ingestor currently imports the small demo archive:

```text
ingestor/src/main/resources/gtfs/budapest-mini.zip
```

Run:

```bash
./gradlew :ingestor:bootRun
```

Current behavior:

- clears existing `locations` and `connections` data;
- imports stops from `budapest-mini.zip`;
- imports stop-time connections;
- uses the GTFS service date from `calendar_dates.txt` for deterministic demo timestamps.

## Start API

```bash
./gradlew :api:bootRun
```

Readiness check after demo data import:

```bash
curl http://localhost:8080/api/v1/readiness
```

Expected result: `200 OK` with `ready: true` after the ingestor has completed.

## Demo API calls

List stops:

```bash
curl 'http://localhost:8080/api/v1/locations?limit=200'
```

Search demo route:

```bash
curl -X POST 'http://localhost:8080/api/v1/routes/search' \
  -H 'Content-Type: application/json' \
  -d '{
    "originStopId": "F00985",
    "destinationStopId": "F00045",
    "departureDateTime": "2026-01-27T04:44:00+01:00",
    "optimization": "FASTEST",
    "transportTypes": ["BUS"]
  }'
```

## Start web UI locally

For split local development, start the Vite app after the API is running:

```bash
cd web-ui
npm ci
npm run dev -- --host 127.0.0.1
```

Open:

```text
http://localhost:5173
```

The UI loads stops from `/api/v1/locations`, can set origin/destination from map markers, and can run the bundled demo route.

## Full Docker demo

The current one-command demo is:

```bash
docker compose up --build
```

Startup order:

```text
db -> ingestor -> api -> web-ui
```

Open:

```text
http://localhost:5173
```

## Stop local services

```bash
docker compose down
```

If you want to remove the local PostgreSQL volume too:

```bash
docker compose down -v
```

## Known gaps

- Full GTFS archives are intentionally not tracked in git.
- BKK remote/scheduled import is not implemented yet.
- Browser E2E is still manual; no Playwright smoke test is committed yet.
