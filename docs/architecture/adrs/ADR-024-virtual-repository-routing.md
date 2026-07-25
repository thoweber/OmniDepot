# ADR-024: Virtual Repository Aggregation & Precedence Routing

* **Status:** Accepted

## Context
Development teams require unified endpoint URLs (e.g., `maven-public`, `npm-all`) combining multiple hosted and proxy repositories in a prioritized search order.

## Decision
Introduce a `VirtualRepository` aggregate holding an ordered list of member repositories with integer `precedence` ranks and glob path filters (`include_patterns`, `exclude_patterns`). Short-circuit evaluation on the first matching member repository and cache path resolutions in Caffeine L1.

## Consequences

### Positive
- Simplifies client tool configuration with single endpoint URLs; L1 cache short-circuiting delivers sub-millisecond route resolution.

### Negative
- Overlapping glob patterns across member repositories can create routing precedence confusion if misconfigured.

## Non-Negotiable Invariants
- Virtual repositories store zero physical binary payloads; they act purely as routing evaluators.
