# ADR-003: Pluggable Storage and Persistence SPIs

* **Status:** Accepted

## Context
omnidepot must support multiple storage backends (AWS S3, RustFS, Local File System) and database engines (PostgreSQL 16+, H2) without coupling domain logic to specific cloud SDKs or drivers.

## Decision
Define decoupled Service Provider Interfaces (SPIs)—`BlobStore` and `UploadSessionRepository`—in `repo-core-api`. Implement storage backends in dedicated modules (`repo-storage-s3`, `repo-storage-fs`) selected dynamically at boot via Quarkus `@LookupIfProperty`.

## Consequences

### Positive
- Completely eliminates cloud provider lock-in; enables frictionless local testing using embedded H2 and local filesystem storage.

### Negative
- Abstracting storage and database drivers limits the direct use of vendor-specific proprietary features unless explicitly wrapped by an SPI extension.

## Non-Negotiable Invariants
- Core domain modules must never import AWS SDK or filesystem-specific classes directly.
