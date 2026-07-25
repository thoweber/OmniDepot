# ADR-011: Angular SPA Embedded Static Delivery

* **Status:** Accepted

## Context
Managing a separate Node.js server for the frontend increases deployment complexity and operational footprint.

## Decision
Compile the Angular 17+ Single-Page Application into static assets inside `repo-ui` and bundle them into the Quarkus deployment artifact served via optimized HTTP caching headers.

## Consequences

### Positive
- Single, zero-dependency deployment artifact; zero Node.js server operational costs or container sidecars.

### Negative
- Updating the frontend UI requires running a Maven/Quarkus build step; compiled static assets slightly increase final application image size.

## Non-Negotiable Invariants
- The frontend must be fully self-contained as static HTML/JS/CSS assets with zero Node.js runtime requirement in production.
