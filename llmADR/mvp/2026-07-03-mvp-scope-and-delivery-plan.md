# MVP Scope and Delivery Plan

## Status
Proposed

## Context

`budaTravel` is a pet/portfolio project for learning Kotlin, Gradle, databases, routing, infrastructure, and AI-assisted development.
The near-term goal is not to compete with Google Maps, but to create an end-to-end demo that shows transport data on a map and builds a minimal backend-powered route.

## Decision / Goal

Build an MVP where a user can:

1. start the database and backend;
2. open a simple UI;
3. see Budapest stops on a map;
4. select origin and destination stops;
5. call the backend route search endpoint;
6. see a minimal route rendered on the map.

## Scope

- Backend API + PostgreSQL.
- Ingestor run separately from the API lifecycle.
- Small demo GTFS dataset for quick local setup.
- Web-first UI for the first visible MVP.
- Schematic route rendering by stop coordinates before full GTFS shape support.

## Out of scope

- Production-grade routing correctness.
- Full GTFS scheduled import.
- Android client as the first UI.
- Real-time transport API integration.
- Kubernetes/deployment before the local MVP works.

## Plan

1. Make repository public-ready: license, data attribution, no large full GTFS archives.
2. Add CI for backend tests.
3. Stabilize local dev setup docs.
4. Verify API endpoints needed by the map UI.
5. Add a minimal web UI in this repository.
6. Render stops.
7. Select origin/destination.
8. Call route search.
9. Draw the returned route as a simple polyline.

## Acceptance criteria

- `./gradlew test` passes.
- The project can be cloned without large full data archives.
- A documented demo flow exists.
- MVP UI can display stops and a route using backend data.

## Result

Not completed yet.
