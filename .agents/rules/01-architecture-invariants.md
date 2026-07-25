# OmniDepot Core Architecture Invariants

> **Master Architecture Reference:** Consult `PROJECT_CONTEXT.md` and `docs/architecture/adrs/index.md` (ADR-001 through ADR-031) for complete architectural context.

## 1. Domain & Boundary Protection (ADR-001, ADR-005, ADR-009)
* **Format Isolation:** Format modules (`repo-format-*`) MUST depend ONLY on `repo-core-api`. They MUST NEVER import directly from `repo-storage-*`, `repo-infra-db-*`, or `repo-domain-iam`.
* **Package Encapsulation:** Keep concrete SPI implementations `package-private`. Public visibility is strictly reserved for API interfaces in `repo-core-api`.
* **CDI Selection:** Select storage, database, or identity providers via `@LookupIfProperty` on `@ApplicationScoped` beans.
* **Decoupled Health Probes (ADR-010):** Keep `/q/health/live` restricted to JVM deadlocks on management port `9000`. Database and S3 checks belong strictly in `/q/health/ready`.

## 2. Storage, CAS & Ingestion Invariants (ADR-015, ADR-018, ADR-020, ADR-025)
* **Content-Addressable Storage:** Store physical blobs strictly by SHA-256 digest (`/blobs/sha256/...`) with multi-coordinate N-to-1 mapping.
* **S3 Direct Keying & Buffering (ADR-025):** Target the final CAS key during `CreateMultipartUpload` whenever the digest is known. Buffer incoming streams off-heap to meet S3's 5 MB minimum part threshold (`s3MinPartSizeBytes` in `.agents/config.json`).
* **Stateless Chunked Uploads (ADR-020):** Persist upload session state inside `upload_sessions` (JSONB `provider_state`). Never store chunk state exclusively in local memory.
* **DAG-Aware Tombstone GC (ADR-018):** Compute active manifest closures via recursive DAG traversal before soft-deleting orphaned blobs with a 48-hour grace period (`tombstoneGracePeriodHours`).

## 3. Performance, Auth & Relational Invariants (ADR-016, ADR-017, ADR-019, ADR-023, ADR-024, ADR-026-028)
* **Zero-DB Hot-Path Auth (ADR-019):** Validate CLI-signed JWTs in-memory off-heap (`quarkus-smallrye-jwt`) without database calls during layer streaming.
* **Non-Blocking Outbox Polling (ADR-026):** Execute multi-node outbox queries using `FOR UPDATE SKIP LOCKED`.
* **Dynamic Liquibase Dialects (ADR-023):** Maintain unified XML changelogs with `dbms="postgresql"` and `dbms="h2"` qualifiers. Include explicit `<rollback>` blocks for every `<changeSet>`.
* **Virtual Repository Routing (ADR-024):** Evaluate member repository chains using priority ordering and L1 short-circuit caching.
* **Proxy Cache Revalidation (ADR-027):** Revalidate mutable tags (`latest`, `*-SNAPSHOT`) after TTL expiration via HTTP `HEAD` `If-None-Match` checks. Fall back to cached CAS binaries with `Warning: 110` headers during upstream outages.
* **Cross-Repo Blob Mounting (ADR-028):** Link existing CAS blobs to target repositories in $O(1)$ time ($\le 1.0\text{ ms}$) upon cross-repo RBAC verification.
