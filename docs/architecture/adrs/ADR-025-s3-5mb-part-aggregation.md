# ADR-025: S3 5 MB Part Aggregation and Idempotent Ingest Handling

* **Status:** Accepted

## Context
AWS S3 enforces a 5 MB minimum part size for non-final parts in Multipart Uploads, whereas client CLIs may send smaller chunk sizes. Additionally, concurrent uploads of identical content can trigger database primary key collisions.

## Decision
`S3BlobStore` buffers incoming upload streams in off-heap Netty memory until the buffer reaches $\ge 5\text{ MB}$ before dispatching `s3Client.uploadPart()`. Finalize operations use `ON CONFLICT (digest) DO UPDATE SET last_seen_at = NOW()` and gracefully abort redundant concurrent S3 uploads.

## Consequences

### Positive
- Prevents AWS S3 `EntityTooSmall` HTTP 400 errors during small-chunk uploads; eliminates primary key race conditions and storage duplication during concurrent pushes.

### Negative
- Requires holding up to 5 MB of off-heap Netty memory per active concurrent upload stream.

## Non-Negotiable Invariants
- Non-final S3 upload parts must be $\ge 5,242,880\text{ bytes}$.
- Digest collisions during concurrent ingest must complete gracefully without throwing SQL constraint exceptions.
