# ADR-030: Trunk-Based Development and Short-Lived Branching Strategy

* **Status:** Accepted

## Context
Long-lived feature branches cause severe merge conflicts and delay feedback.

## Decision
Adopt Trunk-Based Development (TBD). The `main` branch is the sole persistent trunk and must remain buildable and releasable at all times. All feature development occurs on short-lived branches ($\le 24\text{--}48\text{ hours}$) or forks. Incomplete features are gated behind configuration properties (`@LookupIfProperty` or feature flags).

## Consequences

### Positive
- Eliminates long-lived branch drift and merge complexity; ensures continuous integration of all code; enables fast release loops.

### Negative
- Requires strict discipline around fast code reviews, feature flagging, and mandatory automated CI pass gates before merging.

## Non-Negotiable Invariants
- Code merged to `main` must pass all ArchUnit, Liquibase, and unit/integration test suites.
