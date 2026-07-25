# ADR-026: Non-Blocking Multi-Node Outbox Polling via `FOR UPDATE SKIP LOCKED`

* **Status:** Accepted

## Context
In multi-node deployments, concurrent background outbox workers polling `outbox_events` contend for the same pending rows, causing duplicate event dispatches and SQL deadlocks.

## Decision
Outbox polling queries must use `FOR UPDATE SKIP LOCKED` semantics. Failed event dispatches execute exponential backoff retries and transition to `FAILED` status after 3 attempts.

## Consequences

### Positive
- Guarantees zero duplicate event dispatches across nodes; completely eliminates database row-lock contention and deadlocks on outbox polling queries.

### Negative
- Event processing order across separate batches is not strictly guaranteed (at-least-once, unordered processing).

## Non-Negotiable Invariants
- Workers must skip locked rows without waiting, guaranteeing zero duplicate event dispatches across nodes.
