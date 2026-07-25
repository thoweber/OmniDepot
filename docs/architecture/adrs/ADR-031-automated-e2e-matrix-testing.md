# ADR-031: Automated E2E Matrix Integration Testing with Native Package CLIs

* **Status:** Accepted

## Context
Unit tests cannot guarantee true protocol compatibility with native client tools (`docker`, `podman`, `mvn`, `npm`).

## Decision
Execute automated end-to-end matrix integration tests in CI across three deployment topologies:
1. *Dev Machine:* Embedded H2 + Local FS + Zero-Auth + Caddy.
2. *Standard Enterprise:* PostgreSQL + RustFS S3 + PAT Token Broker.
3. *Clustered HA:* 2x App Pods + PostgreSQL + Apache Kafka + RustFS S3.

## Consequences

### Positive
- Guarantees 100% protocol compliance with real client CLIs across single-node, dev, and multi-pod enterprise clusters prior to merging to `main`.

### Negative
- Requires running multi-container Docker Compose stacks in CI, adding 2–3 minutes to full matrix CI pipeline execution times.

## Non-Negotiable Invariants
- E2E test suites must use un-mocked official CLI binaries executing real push, pull, proxy, and air-gapped fallback workflows.
