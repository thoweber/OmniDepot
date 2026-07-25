# ADR-005: Bounded Context Isolation and Hexagonal DDD Boundaries

* **Status:** Accepted

## Context
Coupling format wire protocols (e.g., OCI distribution spec, Maven metadata XML) to internal persistence schemas creates fragile, unmaintainable code.

## Decision
Enforce four isolated Bounded Contexts: **Catalog Context**, **Storage Context**, **IAM Context**, and **Outbox Context**.

## Consequences

### Positive
- Changes to upstream format specifications (e.g., OCI v2.1) do not impact database schemas or core domain logic.

### Negative
- Introduces mapping boilerplate between wire-protocol DTOs, domain models, and relational database entities.

## Non-Negotiable Invariants
- Format adapters (`repo-format-*`) act strictly as wire-protocol converters; they must never leak protocol-specific models into core domain entities.
