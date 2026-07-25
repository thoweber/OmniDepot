# ADR-013: Decoupled Shared Component Library and Storybook

* **Status:** Accepted

## Context
Frontend components linked directly to backend API calls are difficult to test and reuse.

## Decision
Isolate presentational UI components into a shared UI library validated independently using Storybook stories before integration.

## Consequences

### Positive
- Presentational UI components are fully testable in isolation; frontend components can be reviewed in Storybook without running the Quarkus backend.

### Negative
- Requires maintaining Storybook stories and maintaining strict separation between presentational components and API container components.

## Non-Negotiable Invariants
- Presentational components must accept data via Angular `input()` signals and emit changes via `output()` events, holding no direct HTTP dependencies.
