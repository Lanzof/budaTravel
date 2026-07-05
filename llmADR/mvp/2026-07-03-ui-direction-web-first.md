# UI Direction: Web First, Android Later

## Status
Accepted

## Context

The project needs a visible MVP: a map with stops and a minimal route rendered from backend data.
The original UI idea considered Android because the backend is written in Kotlin, but Android framework knowledge is currently a learning bottleneck.

The project owner is primarily a backend developer. The first UI should minimize framework overhead and maximize feedback speed.

## Decision / Goal

Build the first MVP UI as a web app in this repository.

Suggested structure:

```text
web-ui/
```

The web UI should consume the backend HTTP API and provide the first end-to-end demo.
Android can be added later as a second client after the API and UX flow are clearer.

## Scope

The web MVP should support:

- map centered on Budapest;
- stop markers loaded from backend;
- origin/destination selection;
- route search request to backend;
- route rendering on the map;
- minimal local setup documentation.

## Out of scope

- Native Android app for the first MVP.
- Mobile polish.
- Offline mode.
- User accounts.
- Production deployment.

## Rationale

Web-first is preferred because:

- faster iteration and debugging;
- easier portfolio demonstration;
- simpler map rendering with libraries such as Leaflet or MapLibre;
- lower initial framework complexity than Android;
- easier to evolve alongside the backend in a monorepo.

## Consequences

- The backend API must be browser-friendly.
- CORS/local dev setup may be needed.
- The first UI will validate practical API needs before Android work starts.

## Parallel work model

Backend and frontend can be developed in parallel after a minimal API contract is agreed.

Backend track:

- stable stop list endpoint;
- route search endpoint;
- demo dataset;
- CORS/local dev support if needed.

Frontend track:

- map skeleton using React + Vite + TypeScript + Leaflet;
- mocked API fixtures first if backend work is not ready;
- stop marker rendering;
- route polyline rendering.

## Acceptance criteria

- The repository contains a documented web-first decision.
- Future frontend work starts under `web-ui/` unless superseded by a later ADR.
- Android is explicitly deferred, not abandoned.

## Result

Not implemented yet.
