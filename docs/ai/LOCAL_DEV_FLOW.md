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
- Automatic BKK static archive DB rebuild/scheduled import is not implemented yet.
- Browser E2E is still manual; no Playwright smoke test is committed yet.


## GTFS archive source override

By default the ingestor uses the bundled demo archive:

```text
budatravel.gtfs.source=demo
budatravel.gtfs.demo.resource=classpath:gtfs/budapest-mini.zip
```

For manual full-archive experiments, point the ingestor at a mounted/local file:

```bash
GTFS_SOURCE=local-file GTFS_ARCHIVE_PATH=/path/to/budapest_gtfs.zip docker compose up --build ingestor
```


BKK static import can download the public static archive into a local cache. The default URL points at BKK OpenData and does not require an API key:

```text
budatravel.gtfs.source=bkk-static
budatravel.gtfs.bkk-static.url=${BKK_GTFS_URL:https://go.bkk.hu/api/static/v1/public-gtfs/budapest_gtfs.zip}
budatravel.gtfs.bkk-static.cache-dir=${BKK_GTFS_CACHE_DIR:/data/gtfs-cache}
```

Example one-shot static archive import against the Docker Compose stack:

```bash
GTFS_SOURCE=bkk-static docker compose up --build ingestor
```

The downloaded full archive is about 48 MB. Docker Compose bind-mounts `./.local/gtfs-cache` into `/data/gtfs-cache`, so repeated static archive runs can reuse `ETag` / `Last-Modified` metadata and skip unchanged downloads. If a newer archive appears while the matching dataset is already imported, the current ingestor only logs that automatic DB rebuild is not implemented yet. Reset the DB volume before switching GTFS sources until import generations are designed.

The import flow uses `GtfsArchiveProvider` and reads ZIP entries through streams, so classpath archives and local files share the same parser path. The previous temporary-file adapter for nested Spring Boot jar resources is no longer needed.
