# ADR-022: Upstream Credential Forwarding and Isolation

* **Status:** Accepted

## Context
Pull-through proxies fetching from private upstream registries (e.g., private ECR, Quay, Sonatype) must authenticate securely without exposing upstream credentials to client users.

## Decision
Encrypt upstream credentials at rest using AES-256-GCM. The proxy engine injects decrypted upstream credentials into outgoing requests server-side.

## Consequences

### Positive
- Downstream users never see or handle upstream secrets; private upstream repositories can be cached securely.

### Negative
- Requires managing encryption keys for credential storage; upstream authentication errors must be carefully sanitized before returning responses to clients.

## Non-Negotiable Invariants
- Upstream credentials must never be leaked to downstream client HTTP responses or error logs.
