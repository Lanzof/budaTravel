---
name: Routing API TDD Implementation Plan
overview: ""
todos: []
isProject: false
---

---
todos:
  - id: "dto-setup"
    content: "Introduce DTOs for locations and routing requests/responses and cover them with serialization tests."
    status: completed
  - id: "locations-list-endpoint"
    content: "Implement GET /api/v1/locations with filters and TDD."
    status: completed
  - id: "location-by-id-endpoint"
    content: "Implement GET /api/v1/locations/{stopId} with TDD."
    status: completed
  - id: "locations-autocomplete-endpoint"
    content: "Implement GET /api/v1/locations/autocomplete with TDD."
    status: completed
  - id: "routing-core-logic"
    content: "Implement core multi-hop routing service with max 3 transfers and tests."
    status: completed
  - id: "route-search-endpoint"
    content: "Implement POST /api/v1/routes/search with validation, mapping, and TDD."
    status: completed
  - id: "openapi-update"
    content: "Update OpenAPI/Swagger for new endpoints and models."
    status: completed
  - id: "integration-tests"
    content: "Add end-to-end integration tests for locations and route search flows."
    status: completed
isProject: false
---
# Routing API TDD Implementation Plan

### High-level approach

- Implement the API strictly according to `llmADR/ROUTING_API_SPEC.md`.
- Follow TDD: for each endpoint/behavior, first create/extend tests, then implement code to make them pass.
- Progress in small, vertical slices: **one endpoint (or sub-capability) per stage**.
- Keep `core` module responsible for routing logic and `api` for HTTP contracts and validation.

---

## Stage 0 – Test and project scaffolding

- **Goal**: Ensure the project is ready for TDD for API and core logic.
- **Steps**:
- Set up or verify Spring Boot test dependencies in `api/build.gradle.kts` and `core/build.gradle.kts` (JUnit 5, Spring Boot starter test, MockMvc/WebTestClient, etc.).
- Confirm existing tests (`ApiApplicationTests` and core service test suite) run green.
- Add a small smoke test in `api` that hits existing `/api/v1/routes/health` (if present) to validate MockMvc/WebTestClient setup.

---

## Stage 1 – DTOs for locations and routes (shared/common module)

- **Goal**: Introduce DTOs for Location and RouteSearch request/response according to the spec.
- **Steps**:
- Add tests in `common` (or `api`) verifying basic DTO semantics and JSON serialization shape (e.g., using Jackson tester or object mapper snapshot-style tests):
  - `LocationDto` and `LocationSuggestionDto` with `stopId`, `name`, `lat`, `lon`.
  - `RouteSearchRequest` with `originStopId`, `destinationStopId`, `departureDateTime`, `optimization`, `transportTypes`.
  - Updated `RouteResponse`/`RouteSegment` structure (including coordinates and IDs) if they differ from current DTOs.
- Implement DTO classes and Jackson annotations (if needed) to make the tests pass, keeping them in `common` if they are shared between `api` and `core`.

---

## Stage 2 – GET /api/v1/locations (list with filters)

- **Goal**: Implement listing of locations with optional `q`, `stopId`, pagination, and 400 on conflicting params.
- **Steps**:
- Add controller tests in `api`:
  - 200 with no params: defaults `limit`=50, `offset`=0, returns a list mapped from `Location` entities.
  - 200 with `q` only: invokes `LocationRepo.findByNameContainingIgnoreCase`.
  - 200 with `stopId` only: invokes `LocationRepo.findByStopId`.
  - 400 when both `q` and `stopId` are provided.
  - 400 for invalid `limit`/`offset` (negative, zero if forbidden, non-numeric in query binding).
- Add a small service-level test (in `core` or `api`) if you introduce a `LocationService` abstraction.
- Implement `LocationController` (or extend existing controller) and, optionally, `LocationService`:
  - Wire to `LocationRepo` from `core`.
  - Apply filter and pagination logic.
  - Map `Location` entities to `LocationDto`.
- Ensure JSON shape matches the spec via response tests.

---

## Stage 3 – GET /api/v1/locations/{stopId}

- **Goal**: Implement retrieval of a single location by `stopId` with 404 on missing.
- **Steps**:
- Add controller tests:
  - 200 when `stopId` exists, verifying body matches `LocationDto`.
  - 404 when `stopId` is unknown.
- Implement endpoint in `LocationController` using `LocationRepo.findByStopId`:
  - Decide whether multiple matches are an error or just pick the first; document and test the chosen behavior.
- Optionally add a small service-level unit test if using `LocationService`.

---

## Stage 4 – GET /api/v1/locations/autocomplete

- **Goal**: Implement autocomplete endpoint with required `q` and result limiting.
- **Steps**:
- Add controller tests:
  - 200 when `q` is provided: returns at most `limit` results mapped to `LocationSuggestionDto`.
  - 400 when `q` is missing or blank.
  - Ensure default `limit`=10 and max=50 are enforced.
- Implement endpoint logic (reuse `LocationRepo.findByNameContainingIgnoreCase` and/or `findByStopId` with heuristic matching).
- Keep separate mapping function for suggestions if it diverges from full `LocationDto` later.

---

## Stage 5 – Core routing model and algorithm (multi-hop, max 3 transfers)

- **Goal**: Prepare core routing logic independent from HTTP, including multi-hop algorithm and optimization.
- **Steps**:
- In `core`, design a `RoutingService` (new) or extend `RouteService` to support:
  - Input: origin/ destination `stopId`s, `departureDateTime` (OffsetDateTime), `optimization`, `transportTypes`.
  - Output: domain-level route representation (e.g., list of segments referencing `Location` and `Connection`).
- Write unit tests for routing behavior using an in-memory graph or test database:
  - Finds a simple direct route.
  - Finds multi-hop route when direct route is absent or worse.
  - Respects max 3 transfers (no routes with >3 changes).
  - Distinguishes FASTEST vs CHEAPEST when multiple routes exist.
  - Respects transport filter (only BUS from current GTFS data).
- Implement an initial, straightforward algorithm (e.g., Dijkstra or layered search) on `Connection` and `Location`:
  - Convert `departureDateTime` to internal `OffsetDateTime` in Europe/Budapest.
  - Assemble routes as ordered lists of segments.
- Add mapper from route domain model to DTO-like structure suitable for `RouteResponse`.

---

## Stage 6 – API contract for POST /api/v1/routes/search (validation & mapping)

- **Goal**: Implement the HTTP endpoint skeleton, focusing on request validation, mapping to core service, and basic response shape.
- **Steps**:
- Add controller tests in `api` for `POST /api/v1/routes/search`:
  - 400 when `originStopId` or `destinationStopId` missing.
  - 400 when `originStopId == destinationStopId`.
  - 400 when `departureDateTime` is missing or not a valid ISO-8601 with offset.
  - 400 for unsupported `optimization` or `transportTypes` values.
  - 404 when origin or destination `stopId` don’t exist.
- Introduce a controller (e.g., extend `RoutesController`) that:
  - Parses `RouteSearchRequest`.
  - Validates fields and returns appropriate error responses.
  - Converts `departureDateTime` string to `OffsetDateTime`.
  - Delegates to `RoutingService` (`core`) and maps the result to `RouteResponse` DTOs (including coordinates and IDs).
- Keep routing algorithm stubs simple at first if needed; tests can use small fixtures.

---

## Stage 7 – Route Search business rules & edge cases

- **Goal**: Enforce all business rules around transfers, no-route cases, and timezones in API layer.
- **Steps**:
- Add tests that cover:
  - No route found → 422 Unprocessable Entity with the specified error body.
  - All returned routes obey max 3 transfers (segments.size - 1 ≤ 3).
  - Optimization preference is respected by API response order (FASTEST / CHEAPEST).
  - Proper handling of edge `departureDateTime` values (close to end of day, etc.), at least in unit tests.
- Extend controller and service logic to:
  - Map ”no result” from `RoutingService` to a 422 error.
  - Ensure routes returned from core satisfy max 3 transfers constraint (double-check in API layer if desired).

---

## Stage 8 – OpenAPI/Swagger updates

- **Goal**: Ensure the new/updated endpoints and DTOs are fully described in Swagger.
- **Steps**:
- Update `OpenApiConfig` in `api` to include:
  - Models: `LocationDto`, `LocationSuggestionDto`, `RouteSearchRequest`, updated `RouteResponse` and `RouteSegment`, error model.
  - Endpoints: `GET /api/v1/locations`, `GET /api/v1/locations/{stopId}`, `GET /api/v1/locations/autocomplete`, `POST /api/v1/routes/search`.
- Add tests (if feasible) that verify OpenAPI is generated and contains key paths/schemas (e.g., snapshot or simple string assertions).

---

## Stage 9 – Integration tests and regression checks

- **Goal**: Validate the whole flow end-to-end using test data.
- **Steps**:
- Create an integration test in `api` that spins up the Spring context with an in-memory or test Postgres instance.
- Preload a small dataset of `Location` and `Connection` entities (or reuse ingestor output) for scenarios:
  - Simple direct route.
  - Multi-hop route within 3 transfers.
  - No available route.
- Hit the HTTP endpoints:
  - `/api/v1/locations` and variants.
  - `POST /api/v1/routes/search` for different `optimization` values.
- Assert on full JSON responses (routes structure, coordinates, durations, errors).

---

## Stage 10 – Cleanup and documentation

- **Goal**: Finalize routing API feature.
- **Steps**:
- Remove or refactor any obsolete code (e.g., legacy single-hop `RouteServiceImpl` behavior if it conflicts with new design).
- Ensure all tests (unit + integration) are green and stable.
- Update project-level README or a feature-specific doc to link to `llmADR/ROUTING_API_SPEC.md` and briefly describe the new endpoints.
