# ADR-021: Pluggable Identity and Zero-Auth Developer Mode

* **Status:** Accepted

## Context
Local testing and simple isolated networks require frictionless setup without configuring LDAP/OIDC providers.

## Decision
Implement a pluggable identity SPI supporting OIDC/LDAP in enterprise mode and a Zero-Auth mode (`repo.auth.mode=disabled`) for local development that grants full administrative rights to all requests.

## Consequences

### Positive
- Instant local developer setup with zero authentication friction; clean identity SPI for enterprise SSO integrations.

### Negative
- Risk of accidental security exposure if Zero-Auth mode is enabled in production environments due to misconfiguration.

## Non-Negotiable Invariants
- Zero-Auth mode must be explicitly disabled in production profiles.
