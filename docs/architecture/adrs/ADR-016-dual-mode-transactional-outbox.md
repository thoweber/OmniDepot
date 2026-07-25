# ADR-016: Dual-Mode Transactional Outbox Engine

* **Status:** Accepted

## Context
Domain event publishing must be reliable and atomic with business database transactions, supporting both single-container and distributed setups.

## Decision
Write domain events to an `outbox_events` table within the active database transaction. Relay events locally via the Vert.x EventBus in single-container mode or stream to Apache Kafka in clustered mode.

## Consequences

### Positive
- Guarantees at-least-once event delivery with zero lost events during unexpected node crashes; scales seamlessly from single containers to enterprise Kafka clusters.

### Negative
- Requires background worker table maintenance to clean up processed events; event consumers must implement idempotent handling.

## Non-Negotiable Invariants
- External event dispatches must never execute inside the primary database transaction block.
