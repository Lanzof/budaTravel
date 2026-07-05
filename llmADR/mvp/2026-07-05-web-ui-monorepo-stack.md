# Web UI Monorepo Stack for MVP

## Status
Accepted

## Context

The MVP needs a visible UI that can display Budapest stops on a map and later render a minimal route returned by the backend.
The project is primarily a learning/pet/portfolio project, not a high-traffic product.
The expected audience for the MVP is very small: mostly the project owner and possibly one more user.

The repository is currently perceived as a Gradle/Kotlin backend project with several modules, so adding frontend code must keep boundaries explicit and avoid making the build harder to understand.

## Decision / Goal

Build the first web UI inside this repository as a monorepo component:

```text
web-ui/
```

The web UI is not part of the Gradle multi-module build.
It has its own Node-based toolchain and local development commands.

Use the following initial frontend stack:

- React for UI components;
- Vite for local dev server and build tooling;
- TypeScript for typed frontend code and API DTOs;
- Leaflet for map rendering;
- OpenStreetMap-compatible tiles for local/MVP map display.

## Scope

The first UI slice should support:

- map centered on Budapest;
- loading stops from the backend API;
- using the current `/api/v1/locations` endpoint and bbox query parameters;
- displaying stops as simple map markers or points;
- showing basic stop details on click;
- documenting how to run backend and frontend locally.

The expected local flow is separate commands, for example:

```bash
# backend
./gradlew :api:bootRun

# frontend
cd web-ui
npm install
npm run dev
```

## Out of scope

- Production-scale map tile infrastructure.
- Google Maps billing/API-key integration.
- Native Android UI for the first MVP.
- Full production deployment setup.
- Perfect transport route geometry.

## Rationale

Monorepo is acceptable for the MVP because:

- backend API and UI contract can evolve together;
- a single PR can update DTOs, documentation, and frontend usage when needed;
- repository history keeps the MVP learning path in one place;
- CI can still run backend and frontend jobs independently;
- the UI can be extracted into a separate repository later if it grows.

React + Vite + TypeScript is chosen because it is common, quick to bootstrap, and friendly enough for a backend developer learning frontend.

Leaflet is chosen because it is lightweight and works well for the MVP scenario: showing map tiles, markers, and reacting to map viewport changes.
It avoids early dependency on Google Maps billing and API keys.

## Consequences

- The repository becomes a monorepo, not only a Gradle project.
- Documentation must clearly explain that `web-ui/` is launched separately from Gradle.
- The project will contain both JVM/Gradle and Node/npm ecosystems.
- Backend CORS configuration is needed for local frontend development.
- For larger traffic or production usage, tile provider terms and infrastructure must be revisited.

## Acceptance criteria

- A `web-ui/` directory is used for the first frontend implementation.
- The first frontend PR documents local startup commands.
- The first UI works with the backend API rather than hardcoding only static data.
- Future Android work remains possible but deferred.

## Result

Not implemented yet.
