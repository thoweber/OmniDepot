# ADR-004: Hybrid Port and Path Routing Strategy

* **Status:** Accepted

## Context
Package tools (Docker CLI, Maven, NPM) use distinct path structures and header conventions.

## Decision
Unify all protocol endpoints under port 8080 using explicit path prefixes (`/v2/` for OCI, `/maven/` for Maven/Gradle, `/npm/` for NPM). Provide optional dedicated port listeners via Vert.x routes if legacy tools require standalone port bindings.

## Consequences

### Positive
- Simplifies firewall rules, ingress configurations, and load balancing by exposing a single application port by default.

### Negative
- Requires careful path routing and namespacing in Vert.x to avoid route collisions across different package format specifications.

## Non-Negotiable Invariants
- Protocol routers must isolate format-specific HTTP parsing from core catalog commands.
