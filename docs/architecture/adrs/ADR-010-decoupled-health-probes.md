# ADR-010: Decoupled Liveness and Readiness Health Probes

* **Status:** Accepted

## Context
Coupling Kubernetes Liveness probes to external database availability causes cascading container restarts during brief database network blips.

## Decision
Isolate management endpoints on port 9000. `/q/health/live` verifies only JVM responsiveness and thread deadlocks. `/q/health/ready` evaluates database, S3, and message broker connectivity.

## Consequences

### Positive
- Prevents destructive cascading container restarts during transient infrastructure disruptions.

### Negative
- Requires explicitly configuring two separate ports (8080 for traffic, 9000 for probes) in Kubernetes deployment manifests.

## Non-Negotiable Invariants
- Liveness probes must never execute network I/O or database queries.
