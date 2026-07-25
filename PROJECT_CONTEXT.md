# OmniDepot: Master System Architecture Context & Implementation Blueprint

> **Notice for Antigravity CLI Agent:**
> You are bootstrapping **OmniDepot**, an open-source polyglot package repository (OCI, Maven, NPM) designed as an Evolutionary Modular Monolith. Strictly adhere to the architecture, boundary rules, database conventions, and 31 Architectural Decision Records (ADRs) specified below.

---

## 1. System Identity & Core Tech Stack

* **System Name:** OmniDepot
* **Architecture Style:** Evolutionary Modular Monolith (Hexagonal DDD Boundaries)
* **Java Version:** Java 25 LTS (Records, Sealed Classes, Pattern Matching, Virtual Threads)
* **Core Framework:** Quarkus 3.x + Eclipse Vert.x (Non-blocking reactive streaming)
* **Databases:** PostgreSQL 16+ (Production / Clustered) and Embedded H2 (Dev / Test)
* **Migration Engine:** Liquibase (`quarkus-liquibase`) with dynamic `dbms` attribute dialect isolation
* **Storage Abstraction:** Content-Addressable Storage (CAS) on AWS S3 / RustFS and Local File System
* **Frontend:** Angular 17+ (Signals) with Material Design 3, compiled into Quarkus static resources
* **Branching Model:** Trunk-Based Development (TBD) on a single persistent `main` branch
* **Deployment Models:** Single container (dev/small enterprise) and multi-pod Kubernetes cluster

---

## 2. Java Module Layout & Boundary Rules

To enforce strict domain boundary isolation, the codebase uses a multi-module Maven layout:

```
omnidepot/
├── pom.xml                             # Parent Reactive/Quarkus POM
├── repo-core-api/                      # Public SPIs, Value Objects & Domain Events ONLY
├── repo-core-domain/                   # Catalog, Virtual Repositories & Routing Engine
├── repo-storage-api/                   # Storage SPIs & UploadSession Interfaces
├── repo-storage-fs/                    # Local Filesystem Storage SPI Provider
├── repo-storage-s3/                    # AWS S3 / RustFS Storage SPI Provider (5MB Buffering)
├── repo-infra-db/                      # Liquibase Changelogs & JPA/Panache Entities
├── repo-infra-outbox/                  # Transactional Outbox (SKIP LOCKED & Kafka/EventBus)
├── repo-domain-iam/                    # PAT, Token Broker (PAT-to-JWT), Upstream Credentials
├── repo-format-oci/                    # OCI V2 Distribution Adapter (Cross-Repo Mounting)
├── repo-format-maven/                  # Maven/Gradle Adapter (Checksum Synthesis)
├── repo-format-npm/                    # NPM Registry Adapter
├── repo-ui/                            # Angular SPA static resources
└── repo-app/                           # Quarkus Application Bootstrap & HTTP Routes

```

### Critical Boundary Constraints (ArchUnit Enforced):

1. **Format Module Isolation:** `repo-format-*` MUST depends ONLY on `repo-core-api`. They MUST NOT import `repo-storage-*` or `repo-infra-db-*` directly.
2. **Package-Private Concrete Implementations:** Concrete provider implementations of SPIs MUST be `package-private`. Public visibility is strictly restricted to API interfaces.
3. **CDI SPI Selection:** Storage and database providers are selected dynamically at boot using `@LookupIfProperty` annotations on `@ApplicationScoped` beans.

---

## 3. Database & Schema Invariants (Liquibase & PostgreSQL/H2)

* **Migration Path:** `repo-infra-db/src/main/resources/db/changelog/db.changelog-master.xml`
* **Dialect Handling:** Standard ANSI SQL changes run on all databases. PostgreSQL-specific features (`JSONB`, `USING GIN`, `FOR UPDATE SKIP LOCKED`) and H2 equivalents are qualified in the same changelog using `dbms="postgresql"` or `dbms="h2"` attributes.
* **Mandatory Rollbacks:** Every `<changeSet>` MUST contain an explicit `<rollback>` block.
* **Hibernate 6 Mapping:** Complex metadata fields (`attributes`, `provider_state`) map to Java domain entities using `@JdbcTypeCode(SqlTypes.JSON)`.

---

## 4. Master Architectural Decision Record (ADR) Index

When making code generation decisions, ensure full compliance with all 31 ADRs:

| ADR ID | Title | Summary & Implementation Guardrails |
| --- | --- | --- |
| **ADR-001** | Modular Monolith Architecture | Single deployable container with strict domain package isolation. |
| **ADR-002** | Quarkus & Vert.x Reactive Engine | Vert.x event-loops for streaming; Quarkus CDI for business logic. |
| **ADR-003** | Pluggable Storage & Persistence | `BlobStore` and `PersistenceProvider` decoupled via CDI. |
| **ADR-004** | Port 8080 Route Strategy | Path-based protocol routing (`/v2/`, `/maven/`, `/npm/`) on port 8080. |
| **ADR-005** | DDD & Bounded Context Isolation | Clean domain separation; zero cross-context entity leaks. |
| **ADR-006** | Zero-GC & Off-Heap Memory Rules | Pass direct Netty `ByteBuf` instances directly to S3 without heap allocations. |
| **ADR-007** | Asynchronous Telemetry & Stats | Atomic in-memory counters with scheduled batch flushing to database. |
| **ADR-008** | Shift-Left Governance Evaluation | Bitmask-encoded compliance flags evaluated via read-through Caffeine cache. |
| **ADR-009** | Hybrid Boundary Enforcement | ArchUnit unit tests verify package visibility and module imports. |
| **ADR-010** | Decoupled Health Probes | `/q/health/live` on port 9000 checks JVM; `/q/health/ready` checks DB/S3. |
| **ADR-011** | Embedded Angular SPA | Compiled Angular SPA static assets served directly from `repo-ui`. |
| **ADR-012** | Material Design 3 | Dark-first reactive cyan/gray palette with full WCAG AA compliance. |
| **ADR-013** | Shared Component Library | Storybook-tested UI components isolated from API calls. |
| **ADR-014** | Command Palette Productivity | `Cmd+K` keyboard search palette for global artifact lookup. |
| **ADR-015** | Pure Content-Addressable Storage | Multi-coordinate N-to-1 mapping keyed by SHA-256 (`/blobs/sha256/...`). |
| **ADR-016** | Dual-Mode Transactional Outbox | Outbox table polled by Vert.x EventBus (local) or Apache Kafka (cluster). |
| **ADR-017** | Adaptive L1 Cache Invalidation | In-process Caffeine L1 invalidation backed by Postgres `LISTEN/NOTIFY`. |
| **ADR-018** | Two-Phase Tombstone GC | 48h soft-delete grace period with recursive OCI Multi-Arch DAG traversal. |
| **ADR-019** | Token Broker CLI Auth | `/v2/token` exchanges PAT for short-lived JWTs; validated off-heap. |
| **ADR-020** | Persistent UploadSession SPI | Resumable chunked upload state persisted in JSONB `provider_state`. |
| **ADR-021** | Pluggable Identity & Zero-Auth | Developer mode (`repo.auth.mode=disabled`) bypasses authentication checks. |
| **ADR-022** | Upstream Credential Forwarding | Secure proxy credential forwarding for private upstream registries. |
| **ADR-023** | Liquibase Migration Isolation | Dynamic XML changelogs with `dbms` attribute dialect switching. |
| **ADR-024** | Virtual Repository Aggregation | Priority-ordered member repo routing with short-circuit L1 caching. |
| **ADR-025** | S3 5 MB Part Aggregation & Ingest | 5 MB off-heap buffer before S3 `UploadPart`; `ON CONFLICT` duplicate handling. |
| **ADR-026** | Multi-Node Outbox Polling | `FOR UPDATE SKIP LOCKED` outbox queries with exponential backoff retries. |
| **ADR-027** | Proxy Cache Revalidation TTL | Conditional `HEAD` `If-None-Match` revalidation; `Warning: 110` fallback. |
| **ADR-028** | OCI Cross-Repository Mounting | $O(1)$ zero-transfer layer mounting across repos after RBAC checks. |
| **ADR-029** | Infrastructure TLS Termination | App runs plain HTTP; local dev uses Caddy sidecar with `mkcert`. |
| **ADR-030** | Trunk-Based Development | Single persistent `main` branch with short-lived PR topic branches. |
| **ADR-031** | Automated $3 \times 3$ E2E Matrix | Matrix CI tests verifying topologies against `docker`, `mvn`, and `npm`. |

---

## 5. Protocol-Specific Implementation Protocols

### OCI Distribution Adapter (`repo-format-oci`)

* Endpoint Prefix: `/v2/`
* Layer Ingest: Resumable chunked `PATCH` uploads accumulated via `UploadSession`.
* Cross-Repo Mounting (ADR-028): Intercept `POST /v2/<target>/blobs/uploads/?mount=<digest>&from=<source>`. Check `source:pull` and `target:push` scopes. If valid, link blob in DB and return `201 Created` in $\le 1.0\text{ ms}$.

### Maven Adapter (`repo-format-maven`)

* Endpoint Prefix: `/maven/`
* Checksum Synthesis (ADR-004): Do NOT persist physical files for `.sha256`, `.sha1`, `.md5`, or `.sha512`. Synthesize responses dynamically from the primary `Blob` database record during `GET`. Validate during `PUT` and return `201 Created` instantly.

### Proxy Cache Engine (`repo-core-domain`)

* Immutable Requests (exact SHA / SemVer): Serve directly from CAS.
* Mutable Requests (`latest`, `*-SNAPSHOT`): If within TTL (`repo.proxy.revalidation-ttl`, default `60s`), serve from CAS. If TTL expired, execute HTTP `HEAD` with `If-None-Match`. On `304 Not Modified`, update timestamp and serve CAS. On upstream network failure, serve CAS with `Warning: 110 omnidepot "Upstream unreachable; serving cached fallback"`.

---

## 6. Immediate Scaffolding Roadmap for `antigravity-cli`

When initiating work in `antigravity-cli`, execute the following sequential bootstrapping steps:

1. **Root & Module POMs:** Create parent `pom.xml` with Quarkus 3.x dependencies and multi-module POMs for all 13 modules.
2. **Master Liquibase Setup:** Create `repo-infra-db/src/main/resources/db/changelog/db.changelog-master.xml` and initial `V1.0.0` tables (`repositories`, `blobs`, `artifact_coordinates`, `upload_sessions`, `outbox_events`).
3. **Core SPI Interfaces:** Implement `BlobStore`, `UploadSessionRepository`, and `TokenBroker` in `repo-core-api`.
4. **Dev Proxy Setup:** Create local `docker-compose.yml` with Caddy and Postgres for local `mkcert` testing.
5. **CI Workflow:** Create `.github/workflows/e2e-matrix.yml` executing the $3 \times 3$ matrix test suites.
