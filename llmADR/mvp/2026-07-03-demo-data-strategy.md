# Demo Data Strategy for MVP

## Status
Accepted

## Context

The repository originally stored full GTFS archives under `ingestor/src/main/resources/gtfs/`.
They are useful for real experiments, but they are large and make the repository inconvenient for a public portfolio MVP.
Local import of full data can also take 15-30 minutes.

BKK Open Data allows use, sharing, processing, and project usage under CC BY 4.0 with attribution:

> Data source: BKK Zrt., CC BY 4.0

## Decision / Goal

Use a small curated GTFS demo archive for the MVP:

```text
ingestor/src/main/resources/gtfs/budapest-mini.zip
```

Do not keep full GTFS archives in the tracked repository state.

## Scope

The mini archive should:

- be small enough for fast clone and local demo workflows;
- remain structurally consistent GTFS data;
- contain enough stops and stop times to display markers and build a minimal route;
- include BKK attribution in repository documentation.

Current mini dataset:

- source: BKK `budapest_gtfs.zip`;
- route: `16` (`Deak Ferenc ter M / Szell Kalman ter M`);
- selected trips: one in each direction;
- includes `agency.txt`, `feed_info.txt`, `routes.txt`, `trips.txt`, `stop_times.txt`, `stops.txt`, `calendar_dates.txt`, and `shapes.txt`.

## Out of scope

- Full GTFS scheduled import.
- Real-time BKK API integration.
- API-key management.
- Perfect route geometry for all Budapest transport.

## Plan

1. Replace full tracked GTFS archives with `budapest-mini.zip`.
2. Add `DATA_SOURCES.md` with BKK attribution and license link.
3. Ignore full GTFS zip files in git while keeping the curated mini archive tracked.
4. Keep full data workflows as local/manual until a download/import script is designed.
5. Later add a reproducible script that generates the mini dataset from a full BKK GTFS archive.

## Acceptance criteria

- Repository no longer tracks full GTFS archives.
- Repository tracks a small demo GTFS archive.
- Ingestor uses the demo archive by default.
- BKK attribution is documented.
- Backend tests pass.

## Result

Implemented in branch `ai-features`.
