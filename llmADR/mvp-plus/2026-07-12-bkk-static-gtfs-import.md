# ADR: BKK static GTFS archive cache and live data layers

## Status

Accepted for MVP+ static archive cache implementation. Live data layers remain future work.

## Context

The MVP currently imports a bundled `budapest-mini.zip` archive. For a realistic local mode we need BKK's full static GTFS archive, but it must not be committed to git or baked into the Docker image.

BKK exposes the Budapest static GTFS archive as a ZIP file:

```text
https://go.bkk.hu/api/static/v1/public-gtfs/budapest_gtfs.zip
```

Observed on 2026-07-12:

- response type: `application/zip`;
- full archive, not an incremental feed;
- size: about 48 MB;
- cache headers include `ETag`, `Last-Modified`, `Content-Length`, and `Cache-Control: max-age=86400`;
- conditional requests with `If-None-Match` and `If-Modified-Since` return `304 Not Modified`;
- the static archive endpoint did not require an API key during the check.

BKK also exposes FUTÁR JSON endpoints through an OpenAPI-described API and GTFS-Realtime protobuf feeds. Those are useful live/reference layers, but they should not replace the project's own GTFS import/routing model.

## Decision

Add `bkk-static` as a GTFS archive source behind the existing `GtfsArchiveProvider` abstraction.

The provider will:

1. use `.local/gtfs-cache` mounted into the container as `/data/gtfs-cache`;
2. store the downloaded ZIP outside git;
3. persist HTTP archive metadata next to the ZIP;
4. reuse `ETag` and `Last-Modified` through conditional requests;
5. return the cached archive when BKK responds with `304 Not Modified`;
6. keep `demo` as the default source for local MVP startup and CI.

The ingestor will record the archive fingerprint in `import_status`. If it detects that a newer archive is available while the matching dataset is already imported, it only logs a warning for now. Automatic DB rebuild/switching needs a separate import-generation design.

## Configuration

```text
GTFS_SOURCE=bkk-static
BKK_GTFS_URL=https://go.bkk.hu/api/static/v1/public-gtfs/budapest_gtfs.zip
BKK_GTFS_CACHE_DIR=/data/gtfs-cache
```

The host path is mounted through Docker Compose:

```text
./.local/gtfs-cache:/data/gtfs-cache
```

## Consequences

Positive:

- Full GTFS data can be used locally without committing archives to git.
- Repeated runs can avoid downloading ~48 MB when the archive is unchanged.
- A deleted database volume can be warmed from the already cached archive.
- The recorded archive metadata prepares the project for future import-generation handling.

Trade-offs / open questions:

- The first full import on a clean machine can be slow.
- Switching between `demo` and `bkk-static` against a non-empty DB is unsafe until import generations/staging are implemented.
- Automatic rebuild of an already imported full dataset is intentionally not implemented yet.
- Before implementing rebuild, we must discuss how active generation switching, rollback, and stale data cleanup should work.

## Future live layers

FUTÁR JSON API:

- useful for live departures, vehicles, alerts, trip details, and BKK planner benchmarking;
- should enrich or validate our data, not replace our own routing/database work.

GTFS-Realtime protobuf:

- useful as an educational realtime integration layer;
- introduces protobuf, standard GTFS-RT feeds, and realCity/BKK extensions;
- should be implemented as a separate story after static import and data-generation rules are clear.
