# ADR-019: Token Broker CLI Auth Architecture

* **Status:** Accepted

## Context
Package manager CLIs use different authentication schemes (Basic Auth, Bearer Tokens, OAuth).

## Decision
Implement a centralized Token Broker endpoint (`/v2/token`) that exchanges Personal Access Tokens (PATs) for short-lived, RSA/ECDSA-signed JWTs containing explicit repository scope claims.

## Consequences

### Positive
- Streaming layer routes validate signed JWTs in-memory off-heap without executing database lookups, keeping hot-path auth checks under 1.0ms.

### Negative
- Requires managing RSA/ECDSA key pairs and token rotation logic inside the IAM bounded context.

## Non-Negotiable Invariants
- Layer streaming routes must validate incoming JWT signatures off-heap in memory without executing database lookups.
