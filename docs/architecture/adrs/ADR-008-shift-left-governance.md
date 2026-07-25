# ADR-008: Shift-Left Governance Evaluation

* **Status:** Accepted

## Context
Real-time security and license policy evaluations during artifact downloads add unacceptable latency to CI build pipelines.

## Decision
Evaluate compliance, security vulnerability thresholds, and license rules during artifact ingestion. Encode evaluation outcomes into compact bitmasks (`GovernanceFlags`) stored on the coordinate record and cached in L1 memory.

## Consequences

### Positive
- Reduces download policy evaluation latency to $\le 0.1\text{ ms}$ via simple bitmask checks, accelerating CI builds.

### Negative
- Policy updates require re-evaluating and invalidating cached bitmasks across existing catalog coordinates.

## Non-Negotiable Invariants
- Hot-path download requests evaluate governance access via in-memory bitmask checks in $\le 0.1\text{ ms}$.
