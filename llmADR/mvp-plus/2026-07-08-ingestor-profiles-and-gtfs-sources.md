# Ingestor Profiles and GTFS Source Strategy

## Status
Proposed

## Context

The project currently uses a bundled `budapest-mini.zip` dataset for local demo development. This keeps the feedback loop fast and deterministic.

Later, the project should be able to ingest real BKK GTFS data. BKK data may be exposed as relatively large archives, so using the live source for every local run would make development slow and fragile.

We need a clear runtime strategy for the ingestor so demo/dev runs stay lightweight while real data import remains possible.

## Decision / Goal

Use Spring profiles and explicit GTFS source configuration for the ingestor.

The intended modes are:

1. **Demo profile**
   - Uses bundled mini GTFS archive.
   - Fast and deterministic.
   - Default for local MVP demo and tests.

2. **Local file profile / source**
   - Uses a mounted or configured local GTFS archive path.
   - Useful for manually testing full archives without calling BKK API.

3. **BKK static profile / source**
   - Downloads GTFS data from BKK API or BKK-provided URL.
   - Uses API key/environment configuration when required.
   - Should support caching and import-if-changed.

4. **Scheduled import mode**
   - Later extension of the remote source.
   - Runs periodically, checks whether data changed, imports only when needed.

## Configuration direction

Use Spring configuration properties rather than hard-coded source paths.

Example shape:

```yaml
budatravel:
  gtfs:
    source: demo # demo | local-file | bkk-static
    demo:
      resource: classpath:gtfs/budapest-mini.zip
    local-file:
      path: ${GTFS_ARCHIVE_PATH:}
    bkk-static:
      url: ${BKK_GTFS_URL:https://go.bkk.hu/api/static/v1/public-gtfs/budapest_gtfs.zip}
      cache-dir: ${BKK_GTFS_CACHE_DIR:/data/gtfs-cache}
```

Profiles can then set defaults:

- `demo` profile -> `budatravel.gtfs.source=demo`
- `local-file` profile -> `budatravel.gtfs.source=local-file`
- `bkk` profile -> `budatravel.gtfs.source=bkk-static`

The exact names can be adjusted during implementation.

## Proposed abstraction

Introduce a `GtfsArchiveProvider` or equivalent interface:

```kotlin
interface GtfsArchiveProvider {
    fun getArchive(): GtfsArchive
}
```

Where `GtfsArchive` captures at least:

- readable archive content;
- dataset name/source;
- optional version/checksum/timestamp;
- whether the archive was changed compared to the previous import.

Potential implementations:

- `ClasspathGtfsArchiveProvider`
- `LocalFileGtfsArchiveProvider`
- `BkkRemoteGtfsArchiveProvider`
- later: `CachedGtfsArchiveProvider` wrapper

## Source selection

Source selection should be explicit and visible in logs.

Example startup log:

```text
GTFS source: demo, archive: classpath:gtfs/budapest-mini.zip
```

or

```text
GTFS source: bkk-static, url: https://..., cache: hit, changed: false
```

## Secret handling

BKK API key must not be committed.

Use:

- local `.env` / environment variables;
- Docker Compose environment variables;
- GitHub Actions secrets if CI/deploy ever needs static archive import;
- `.env.example` with placeholder values only.

## Scheduled import behavior

Scheduled import must not be the first implementation step.

Before scheduling, we need BKK static/live API research:

- archive size;
- response format;
- update frequency;
- `ETag` / `Last-Modified` / checksum / version availability;
- auth requirements;
- rate limits or terms.

Later scheduled import should:

1. check remote metadata;
2. skip download/import if unchanged;
3. download to cache when changed;
4. import with status tracking;
5. preserve existing active dataset if new import fails.

The BKK integration design must also decide the retention policy for old GTFS data. The likely MVP+ direction is to keep one active dataset and replace it on successful changed imports, optionally keeping a single rollback generation later. Keeping historical routes by default would make the database grow and risks stale route data affecting current route search.

## Relationship to shape geometry

Shape-based route geometry should be implemented against the demo dataset first.

Reason:

- `budapest-mini.zip` already contains real `shapes.txt` data;
- local feedback stays fast;
- the implementation will naturally apply to full GTFS archives later;
- BKK static import can be added after the data-source strategy is proven.

## Non-goals

This ADR does not implement:

- full BKK API integration;
- scheduled import;
- staging table swap;
- production deployment;
- realtime BKK vehicle data.

## Acceptance criteria

The ingestor source strategy is ready when:

- the project has a documented source selection model;
- demo import uses a configurable classpath source, not hard-coded temp-file logic;
- local-file and BKK static sources have clear task definitions;
- BKK static archive does not require an API key; future live API key handling is documented separately;
- scheduled import is treated as a later feature gated by BKK static/live API research.
