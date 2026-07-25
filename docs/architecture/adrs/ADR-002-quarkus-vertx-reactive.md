# ADR-002: Quarkus & Eclipse Vert.x Reactive Core Engine

* **Status:** Accepted

## Context
Streaming multi-gigabyte package artifacts and container layers requires high-concurrency non-blocking I/O without thread pool exhaustion or high memory usage.

## Decision
Combine Quarkus 3.x CDI for business logic and dependency injection with the Eclipse Vert.x non-blocking reactive event-loop engine for high-throughput HTTP streaming.

## Consequences

### Positive
- High-throughput streaming, low memory consumption under concurrency, sub-second native cold boot times, and resistance to thread starvation.

### Negative
- Steeper learning curve for reactive programming paradigms (`Mutiny` / `Vert.x`), and more complex stack trace debugging compared to imperative blocking code.

## Non-Negotiable Invariants
- Byte streams must be processed reactively (`Mutiny` / `Multi<Buffer>`).
- Never execute blocking I/O (e.g., synchronous disk/database operations) on Vert.x event-loop threads.
