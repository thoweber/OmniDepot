# ADR-027: Proxy Cache Revalidation TTL and Upstream ETag Verification

* **Status:** Accepted

## Context
Pull-through proxies caching mutable tags (e.g., `latest`, `*-SNAPSHOT`) risk serving stale artifacts when upstream repositories update.

## Decision
Enforce a configurable Revalidation TTL (`repo.proxy.revalidation-ttl`, default `60s`) for mutable coordinates. When expired, issue a conditional HTTP `HEAD` request with `If-None-Match`. On `304 Not Modified`, refresh local TTL. If upstream is unreachable, serve the cached local CAS artifact accompanied by a `Warning: 110` HTTP header.

## Consequences

### Positive
- Guarantees fresh dependencies for mutable tags; minimizes upstream bandwidth via $O(1)$ HTTP 304 checks; preserves build pipeline continuity during upstream registry outages.

### Negative
- Adds a brief HTTP `HEAD` round-trip check when querying mutable tags after TTL expiration.

## Non-Negotiable Invariants
- Immutable coordinates (exact SHA/SemVer) bypass upstream revalidation ($TTL = \infty$).
- Upstream network failures must fall back to cached CAS binaries to preserve build pipeline continuity.
