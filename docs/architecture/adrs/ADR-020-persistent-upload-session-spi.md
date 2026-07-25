# ADR-020: Persistent UploadSession SPI

* **Status:** Accepted

## Context
Large chunked uploads (e.g., multi-gigabyte Docker layers via `PATCH`) must survive node restarts and support multi-node routing.

## Decision
Manage upload state via a persistent `UploadSession` entity storing provider-specific session details (e.g., S3 `UploadId`, part ETags) in JSONB.

## Consequences

### Positive
- Multi-gigabyte chunked uploads are fully resumable and node-agnostic in clustered environments.

### Negative
- Abandoned or interrupted upload sessions require scheduled background cleanup jobs to purge uncommitted S3 multipart uploads.

## Non-Negotiable Invariants
- Any cluster node must be capable of resuming and finalizing an upload session initiated by another node.
