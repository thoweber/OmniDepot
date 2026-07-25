# ADR-017: Adaptive L1 Cache Invalidation Strategy

* **Status:** Accepted

## Context
In-memory Caffeine caches across multiple application nodes must stay synchronized without requiring a heavy Redis cluster for basic deployments.

## Decision
Use Caffeine L1 in-memory caching. In multi-node deployments, trigger cross-node L1 cache invalidation using PostgreSQL `LISTEN/NOTIFY` channels (or Kafka in enterprise clusters).

## Consequences

### Positive
- Completely eliminates the need to deploy and manage a Redis or Infinispan sidecar cluster for small-to-medium multi-pod setups.

### Negative
- PostgreSQL `LISTEN/NOTIFY` requires fallback in-process invalidation when running on embedded H2; transient database network drops require TTL fallback safety nets.

## Non-Negotiable Invariants
- Cache invalidation messages must carry coordinate keys rather than full entity payloads to keep notification traffic lightweight.
