# Routing Refactor Plan (PostgreSQL)

## Status
- Completed

## Objective
- Refactor routing logic to avoid loading all `Connection` rows into memory.
- Keep PostgreSQL as the primary data store.
- Make routing behavior deterministic and test-driven for `FASTEST` happy path.
- Enforce data rule: if a trip segment duration is `0 seconds`, normalize it to `1 minute`.

## Scope
- Refactor `core` routing service and related repository query methods.
- Keep API contract unchanged in this phase.
- Do not introduce a graph database in this phase.
- Manage schema exclusively via Flyway migrations (no schema changes via `ddl-auto`).

## Implementation Steps

## Step 1. Migrate DDL from `ddl-auto` to Flyway
- Introduce Flyway as the source of truth for schema lifecycle.
- Create baseline migration scripts for table creation (at least `location` and `connections`, plus required constraints/keys).
- Move all further schema evolution (indexes/constraints/column changes) to versioned Flyway migrations only.
- Disable automatic schema generation in runtime config (`ddl-auto`) for non-test environments.
- Keep test setup explicit:
  - either run Flyway in tests;
  - or use dedicated test migration strategy, but still avoid relying on JPA auto-DDL as the canonical schema path.
- **Mandatory validation after migration step:** full test suite must pass (`./gradlew test`).

## Step 2. Repository Query Refactor
- Remove usage of `connectionRepo.findAll()` from routing flow.
- Add targeted repository queries for next-hop expansion:
  - by `from_location_id`;
  - by `departure_time >= :currentTime`;
  - by `type in (:types)`;
  - with deterministic ordering (for stable tests).
- Keep result sets bounded per expansion step (limit candidates).

## Step 3. Routing Algorithm Refactor
- Implement best-first search (Dijkstra/A*-like), not full path enumeration.
- Search state should include:
  - current stop;
  - current time;
  - current path;
  - transfer count;
  - accumulated duration metric for `FASTEST`.
- Enforce max transfers: `3` (equivalent to max `4` segments).
- Add pruning of dominated states to avoid combinatorial explosion.

## Step 4. Duration Normalization Rule
- Add a single normalization function for segment duration:
  - if computed duration is `0 seconds`, return `Duration.ofMinutes(1)`.
- Use this rule in:
  - segment-level duration calculations;
  - route-level total duration aggregation.
- Ensure behavior is covered by unit tests.

## Step 5. FASTEST Happy-Path Test Design
- Add/extend tests in `core` to guarantee best-first behavior with a controlled graph:
  - Build graph from `A` to `B` with exactly 3 branches:
    - Branch 1: valid, fastest.
    - Branch 2: valid, slower.
    - Branch 3: invalid because it requires 4 transfers, must be ignored.
- Assertions:
  - invalid branch is not returned;
  - returned route is the fastest among valid branches.
- For now, test only `FASTEST` happy-path behavior.

## Step 6. PostgreSQL Fitness (No Full Table Scan in Code)
- Verify service code path does not call `findAll()` for routing.
- Add/confirm indexes required by query pattern via **Flyway SQL migration scripts**:
  - `connections(from_location_id, departure_time)`;
  - `connections(type, departure_time)` (or separate index on `type` if preferred);
  - `location(stop_id)` unique index.

## Acceptance Criteria
- Extended tests guarantee best-first search behavior using graph `A -> B` with three branches:
  - one branch with 4 transfers is ignored;
  - among two valid branches, fastest is selected.
- Max 3 transfers rule is enforced.
- `0 seconds => 1 minute` rule is consistently applied.
- No full in-memory `Connection` dump is used in routing code.
- Indexes and table DDL are delivered as Flyway migrations.
- Runtime schema creation via `ddl-auto` is not used as the primary schema mechanism.
