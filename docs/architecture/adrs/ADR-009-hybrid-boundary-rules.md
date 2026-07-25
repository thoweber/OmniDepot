# ADR-009: Hybrid Boundary Enforcement with ArchUnit

* **Status:** Accepted

## Context
Java package boundaries are easily violated during rapid feature development if not enforced by automated tooling.

## Decision
Combine Java module encapsulation with automated ArchUnit test suites executing during `mvn test`.

## Consequences

### Positive
- Automatically catches boundary violations and improper dependencies in CI before code is merged.

### Negative
- Adds a few seconds to unit test execution times; strict visibility rules can initially slow down developers unfamiliar with the architecture.

## Non-Negotiable Invariants
- ArchUnit tests must immediately fail the build if a format module attempts to import infrastructure, storage implementation, or database classes directly.
- Concrete SPI implementations must remain `package-private`.
