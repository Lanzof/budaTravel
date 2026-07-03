# budaTravel

Multi-module Kotlin/Spring Boot project for searching public transport routes.

## Modules

- `api`: HTTP API controllers, validation, OpenAPI.
- `core`: domain entities, repositories, routing/search logic.
- `common`: shared DTOs used by API and core.
- `ingestor`: GTFS import pipeline.

## Routing API

Routing contract and behavior are defined in [llmADR/ROUTING_API_SPEC.md](llmADR/ROUTING_API_SPEC.md).

Implemented endpoints:

- `GET /api/v1/locations`
- `GET /api/v1/locations/{stopId}`
- `GET /api/v1/locations/autocomplete`
- `POST /api/v1/routes/search`

Route search business rules:

- max 3 transfers (max 4 segments);
- `FASTEST` and `CHEAPEST` optimization;
- no route result maps to `422 NO_ROUTE_FOUND`;
- connection segment duration `0 seconds` is normalized to `1 minute`.

## Test

Run all tests:

```bash
./gradlew test
```
