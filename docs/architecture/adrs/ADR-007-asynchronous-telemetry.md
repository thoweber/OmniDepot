# ADR-007: Asynchronous Telemetry and Atomic Performance Tracking

* **Status:** Accepted

## Context
Recording download metrics, bandwidth usage, and access counts synchronously on every request creates severe database lock contention and degrades download throughput.

## Decision
Track hot-path statistics in-memory using non-blocking `LongAdder` counters. Periodically flush batch updates to the database via scheduled background tasks.

## Consequences

### Positive
- $P_{99}$ download response latencies remain unaffected by telemetry writes, preventing database row-lock contention.

### Negative
- Metrics in the database lag behind real-time by up to the batch flush interval (e.g., 5 seconds); un-flushed metrics could be lost if a container crashes abruptly.

## Non-Negotiable Invariants
- Download read paths must execute zero synchronous SQL `UPDATE` queries.
