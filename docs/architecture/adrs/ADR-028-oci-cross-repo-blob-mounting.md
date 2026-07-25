# ADR-028: OCI Cross-Repository Blob Mounting for Zero-Transfer Layer Aliasing

* **Status:** Accepted

## Context
Pushing container images with shared base layers across repositories wastes network bandwidth if layers are re-uploaded.

## Decision
Implement OCI Cross-Repository Blob Mounting (`POST /v2/<target>/blobs/uploads/?mount=<digest>&from=<source>`). Verify user `pull` access on `source` and `push` access on `target`. Upon verification, link the existing CAS blob to the target repository in the database in $O(1)$ time ($\le 1.0\text{ ms}$).

## Consequences

### Positive
- Reduces network bandwidth usage and container push times by up to 90% for multi-layer images through sub-millisecond layer aliasing.

### Negative
- Requires evaluating permissions across two distinct repository scopes (`source:pull` and `target:push`) during the mount request.

## Non-Negotiable Invariants
- Cross-repository mounts must execute as pure metadata operations with zero byte copying or network transfers.
- Unauthorized mount requests must fall back to standard `202 Accepted` upload prompts without leaking blob existence.
