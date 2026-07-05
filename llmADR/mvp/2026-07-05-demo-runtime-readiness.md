# Demo Runtime Orchestration and Data Readiness

## Status
Accepted

## Context

The MVP should be easy to run as a visible demo: database, data import, backend API, and web UI should start in a predictable order.
The current manual flow is useful for development, but it is fragile as a demo ritual because the API and UI are only useful after the demo GTFS data has been imported.

The ingestor may initially be a one-shot local demo process, but a future version may become a recurring scheduled import.
The runtime orchestration should not depend only on the ingestor process exiting, because that model does not work well for a long-running scheduler.

## Decision / Goal

Introduce an explicit **data readiness** concept for demo/runtime orchestration.

Preferred readiness source: a database-backed import status table, for example:

```text
import_status
- id
- dataset_name
- status              -- RUNNING / COMPLETED / FAILED
- started_at
- completed_at
- locations_count
- connections_count
- error_message
```

For the MVP, the first meaningful dataset name can be:

```text
budapest-mini
```

The intended runtime chain is:

```text
database ready
  -> schema/migrations ready
  -> ingestor imports dataset and marks data ready
  -> API reports ready only when required data is present/import completed
  -> web UI waits for API readiness, not for ingestor directly
```

## Scope

For a future Docker Compose based local demo:

- PostgreSQL should expose a DB healthcheck.
- Ingestor should wait for the database/schema to be ready before importing.
- Ingestor should write import status to the database.
- API readiness should check that demo data is available.
- Web UI container should depend on API readiness rather than knowing about ingestor internals.

Possible local demo command goal:

```bash
docker compose up
```

where the stack eventually brings up:

```text
db -> ingestor -> api -> web-ui
```

## Out of scope

- Implementing this orchestration immediately.
- Kubernetes Jobs/Deployments.
- Production-grade import history UI.
- Multi-city import management.
- Full scheduled import implementation.

## Rationale

A database-backed readiness flag is preferred over a file flag because:

- containers can be recreated and files can disappear;
- database state is visible to ingestor, API, healthchecks, and tests;
- the approach works for both one-shot ingestor and future scheduled ingestor;
- import errors and partial imports can be represented explicitly;
- it creates a useful extension point for future operational endpoints.

Checking only row counts, for example `locations > 0` and `connections > 0`, is simpler but weaker because it cannot reliably distinguish a completed import from a partially running import.

## Consequences

- The project will need a small import status schema when this is implemented.
- API readiness becomes a business readiness check, not only a process-alive check.
- The web UI remains decoupled from ingestion details.
- Docker Compose orchestration can start simple with one-shot ingestor and evolve toward scheduler mode later.

## Acceptance criteria for future implementation

- A local demo flow can be started with a small number of documented commands, ideally `docker compose up`.
- API does not report ready until required demo data is available.
- Web UI starts against an API that is ready or clearly reports waiting/unavailable state.
- The readiness model works whether ingestor is one-shot or scheduled.
- Import failure is visible in the database status and does not look like a successful ready state.

## Result

Not implemented yet.
