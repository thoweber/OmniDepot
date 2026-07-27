# ADR-029: Infrastructure TLS Termination and Local Dev Reverse Proxy Strategy

* **Status:** Accepted

## Context
Package CLIs enforce HTTPS by default, but managing TLS certificates inside application code adds unnecessary complexity and crypto overhead.

## Decision
omnidepot runs strictly as plain HTTP on port 8080 (and port 9000 for management). Production TLS termination is offloaded to external Ingress/Service Mesh infrastructure. Local development (`docker-compose.yml`) uses a lightweight Caddy reverse proxy sidecar paired with `mkcert` to serve locally trusted HTTPS on `https://localhost:8443`.

## Consequences

### Positive
- Keeps application code and JVM stateless, lightweight, and free of TLS certificate management or crypto overhead; aligns with 12-factor cloud-native production patterns.

### Negative
- Local developer onboarding requires a one-time host setup command (`mkcert -install`) to trust the local CA root.

## Non-Negotiable Invariants
- Application code and Quarkus configurations must never manage TLS keystores or SSL certificates directly.
