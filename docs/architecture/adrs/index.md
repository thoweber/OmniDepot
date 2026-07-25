# Architectural Decision Records (ADRs)

OmniDepot captures critical architectural decisions using Architectural Decision Records (ADRs). Below is the complete catalog of all 31 ADRs governing the system's core architecture, storage, security, database migration, user interface, and operational pipeline.

---

## 📑 Index of Architectural Decision Records

| ADR ID | Title | Status |
| :--- | :--- | :--- |
| **[ADR-001](ADR-001-modular-monolith.md)** | Evolutionary Modular Monolith Architecture | Accepted |
| **[ADR-002](ADR-002-quarkus-vertx-reactive.md)** | Quarkus & Eclipse Vert.x Reactive Core Engine | Accepted |
| **[ADR-003](ADR-003-hexagonal-storage-spi.md)** | Pluggable Storage and Persistence SPIs | Accepted |
| **[ADR-004](ADR-004-hybrid-port-path-routing.md)** | Hybrid Port and Path Routing Strategy | Accepted |
| **[ADR-005](ADR-005-bounded-context-isolation.md)** | Bounded Context Isolation and Hexagonal DDD Boundaries | Accepted |
| **[ADR-006](ADR-006-zero-gc-off-heap-memory.md)** | Zero-GC and Off-Heap Direct Memory Allocation Rules | Accepted |
| **[ADR-007](ADR-007-asynchronous-telemetry.md)** | Asynchronous Telemetry and Atomic Performance Tracking | Accepted |
| **[ADR-008](ADR-008-shift-left-governance.md)** | Shift-Left Governance Evaluation | Accepted |
| **[ADR-009](ADR-009-hybrid-boundary-rules.md)** | Hybrid Boundary Enforcement with ArchUnit | Accepted |
| **[ADR-010](ADR-010-decoupled-health-probes.md)** | Decoupled Liveness and Readiness Health Probes | Accepted |
| **[ADR-011](ADR-011-angular-spa-static-delivery.md)** | Angular SPA Embedded Static Delivery | Accepted |
| **[ADR-012](ADR-012-material-design-3-theming.md)** | Material Design 3 and Dark-First Multi-Theming | Accepted |
| **[ADR-013](ADR-013-shared-component-storybook.md)** | Decoupled Shared Component Library and Storybook | Accepted |
| **[ADR-014](ADR-014-command-palette-productivity.md)** | Command Palette Productivity (`Cmd+K`) | Accepted |
| **[ADR-015](ADR-015-pure-content-addressable-storage.md)** | Pure Content-Addressable Storage (CAS) | Accepted |
| **[ADR-016](ADR-016-dual-mode-transactional-outbox.md)** | Dual-Mode Transactional Outbox Engine | Accepted |
| **[ADR-017](ADR-017-adaptive-l1-cache-invalidation.md)** | Adaptive L1 Cache Invalidation Strategy | Accepted |
| **[ADR-018](ADR-018-two-phase-tombstone-gc.md)** | Two-Phase Tombstone Garbage Collection & DAG Traversal | Accepted |
| **[ADR-019](ADR-019-token-broker-cli-auth.md)** | Token Broker CLI Auth Architecture | Accepted |
| **[ADR-020](ADR-020-persistent-upload-session-spi.md)** | Persistent UploadSession SPI | Accepted |
| **[ADR-021](ADR-021-pluggable-identity-zero-auth.md)** | Pluggable Identity and Zero-Auth Developer Mode | Accepted |
| **[ADR-022](ADR-022-upstream-credential-forwarding.md)** | Upstream Credential Forwarding and Isolation | Accepted |
| **[ADR-023](ADR-023-liquibase-dynamic-migration.md)** | Liquibase Dynamic Migration Dialect Isolation | Accepted |
| **[ADR-024](ADR-024-virtual-repository-routing.md)** | Virtual Repository Aggregation & Precedence Routing | Accepted |
| **[ADR-025](ADR-025-s3-5mb-part-aggregation.md)** | S3 5 MB Part Aggregation and Idempotent Ingest Handling | Accepted |
| **[ADR-026](ADR-026-non-blocking-outbox-polling.md)** | Non-Blocking Multi-Node Outbox Polling via `FOR UPDATE SKIP LOCKED` | Accepted |
| **[ADR-027](ADR-027-proxy-cache-revalidation-ttl.md)** | Proxy Cache Revalidation TTL and Upstream ETag Verification | Accepted |
| **[ADR-028](ADR-028-oci-cross-repo-blob-mounting.md)** | OCI Cross-Repository Blob Mounting for Zero-Transfer Layer Aliasing | Accepted |
| **[ADR-029](ADR-029-sidecar-tls-proxy.md)** | Infrastructure TLS Termination and Local Dev Reverse Proxy Strategy | Accepted |
| **[ADR-030](ADR-030-trunk-based-development.md)** | Trunk-Based Development and Short-Lived Branching Strategy | Accepted |
| **[ADR-031](ADR-031-automated-e2e-matrix-testing.md)** | Automated E2E Matrix Integration Testing with Native Package CLIs | Accepted |
