# ADR-023: Liquibase Dynamic Migration Dialect Isolation

* **Status:** Accepted

## Context
OmniDepot supports embedded H2 for local dev and PostgreSQL 16+ for enterprise production. Database migrations must support dialect-specific features (e.g., PostgreSQL GIN indexes, JSONB) without duplicating migration files.

## Decision
Adopt Liquibase (`quarkus-liquibase`) using a master changelog structure (`db.changelog-master.xml`). Use standard Liquibase change types for ANSI SQL, and isolate dialect-specific SQL using the `dbms="postgresql"` and `dbms="h2"` attributes within the same changelog.

## Consequences

### Positive
- Eliminates duplicate DDL directory trees by consolidating cross-dialect migrations into a single changelog pipeline; provides native first-class schema rollbacks.

### Negative
- Slightly higher XML/YAML parsing overhead on cold application startup compared to lightweight plain-SQL readers.

## Non-Negotiable Invariants
- Every `<changeSet>` must include an explicit `<rollback>` block.
- Native PostgreSQL syntax (`USING GIN`, `FOR UPDATE SKIP LOCKED`) must be qualified with `dbms="postgresql"`.
