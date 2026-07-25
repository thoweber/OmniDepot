# ADR-001: Evolutionary Modular Monolith Architecture

* **Status:** Accepted

## Context
OmniDepot requires high development velocity without the operational overhead of microservices, while preserving a clear extraction path if specific domain components scale independently.

## Decision
Build OmniDepot as a Modular Monolith inside a single deployment container, strictly enforcing Hexagonal Bounded Context boundaries via Maven multi-module isolation (`repo-core-api`, `repo-core-domain`, `repo-storage-*`, `repo-infra-*`, `repo-format-*`).

## Consequences

### Positive
- High development velocity, single deployment artifact, simple operations, and clear extraction path if a domain module needs to become a standalone service.

### Negative
- Requires strict architectural discipline and automated ArchUnit enforcement to prevent accidental module coupling ("spaghetti monolith").

## Non-Negotiable Invariants
- Direct inter-module database joins across domain boundaries are strictly forbidden.
- Inter-context communication must occur exclusively via explicit Domain Events or Core API interfaces.
