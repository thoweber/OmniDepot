# Evolutionary Modular Monolith Design

omnidepot follows the **Evolutionary Modular Monolith** pattern. This architecture balances high developer velocity and zero-network-latency inter-module calls with clear physical boundaries that allow extracting microservices if future scale requires it.

---

## Module Structure

```text
omnidepot-parent/
├── repo-core-api/             # SPI Interfaces & Value Objects (Sha256Digest, BlobStore)
├── repo-core-domain/          # Catalog, Routing & Virtual Repositories
├── repo-storage-api/          # Storage Provider Contracts
├── repo-storage-fs/           # Package-Private Filesystem BlobStore Provider
├── repo-storage-s3/           # Package-Private AWS S3 / RustFS BlobStore Provider
├── repo-infra-db/             # Liquibase Migrations & Database Entity Models
├── repo-infra-outbox/         # Transactional Outbox Pattern
├── repo-domain-iam/           # Token Broker & RBAC Implementation
├── repo-format-oci/           # OCI V2 Distribution Adapter
├── repo-format-maven/         # Maven Layout & Checksum Synthesis Adapter
├── repo-format-npm/           # NPM Registry Layout Adapter
├── repo-ui/                   # Web Resources & Frontend SPA
├── repo-app/                  # Quarkus Runner & ArchUnit Boundary Tests
└── repo-coverage-report/      # Aggregated JaCoCo Code Coverage Report
```

---

## Boundary Enforcement (ADR-009)

1. **Package-Private Implementation Classes:** Storage providers (`FileSystemBlobStore`, `S3BlobStore`) are package-private and instantiated via Quarkus CDI property lookup `@LookupIfProperty`.
2. **ArchUnit Boundary Rule:** `repo-format-*` protocol modules MUST depend ONLY on `repo-core-api` and third-party runtime frameworks—never directly on `repo-storage-*` or `repo-infra-db-*`.
