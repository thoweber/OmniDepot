# ADR-015: Pure Content-Addressable Storage (CAS)

* **Status:** Accepted

## Context
Storing identical artifacts across multiple repositories or package formats wastes storage capacity and increases cloud costs.

## Decision
Store all binary payloads strictly in Content-Addressable Storage keyed by SHA-256 hash (`/blobs/sha256/<hash>`). Artifact coordinates in different repositories maintain $N$-to-1 relational references to a single CAS blob.

## Consequences

### Positive
- Global storage deduplication across OCI, Maven, and NPM reduces total S3 cloud storage costs by 70%+; enables $O(1)$ zero-copy layer aliasing.

### Negative
- Mutable coordinates cannot be stored directly as physical files; orphan blob cleanup requires two-phase garbage collection.

## Non-Negotiable Invariants
- Physical storage locations are determined strictly by payload digest. Duplicate binary uploads yield a $1.0\times$ storage footprint.
