# Local Dev Flow — MVP

## Purpose

This document captures the current local workflow for backend MVP development.
It is intentionally small and will evolve as the web UI and deployment setup appear.

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

Health check:

```bash
curl http://localhost:8080/api/v1/routes/health
```

Expected response:

```text
API is running
```

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

## Stop local services

```bash
docker compose down
```

If you want to remove the local PostgreSQL volume too:

```bash
docker compose down -v
```

## Known gaps

- `docker-compose.yaml` currently includes an `app` service, but the most reliable MVP dev flow is DB + local Gradle processes.
- Full GTFS archives are intentionally not tracked in git.
- Web UI local flow will be documented after `web/` is introduced.
