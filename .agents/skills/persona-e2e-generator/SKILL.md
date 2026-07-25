---
name: persona-e2e-generator
description: Generates persona-driven acceptance test scenarios for Mateo, Sven, Elena, Priya, Marcus, Thomas, and Alex.
---

# Persona-Driven E2E Test Generator Skill (`/gen-persona-tests`)

When generating end-to-end integration or acceptance test scenarios:

## 1. Persona Test Mapping
* **Mateo Rossi (Developer DX):** Assert Zero-Auth mode (`repo.auth.mode=disabled`) allows immediate `docker push` without HTTP 401 challenges. Assert Angular UI returns `Cmd+K` global search results in $\le 100\text{ ms}$.
* **Sven Lindqvist (Platform/SRE):** Assert `/q/health/live` remains `200 OK` on port 9000 when PostgreSQL is paused. Assert multi-node outbox processing using `FOR UPDATE SKIP LOCKED` produces 0 duplicate events.
* **Elena Rostova (DevSecOps):** Audit HTTP response headers during upstream proxy calls to verify 0 upstream credential leaks. Assert short-lived JWT signatures validate off-heap in $\le 1.0\text{ ms}$ without DB calls.
* **Priya Sharma (FinOps):** Assert multi-format layer pushes yield a $1.0\times$ storage redundancy ratio in CAS. Assert proxy cache revalidation returns HTTP `304 Not Modified` with 0 egress bytes.
* **Dr. Marcus Vance (CTO):** Assert `repo-core-domain` contains 0 cloud provider SDK imports.
* **Thomas Dubois (OSPO/Legal):** Assert all frontend UI components use Material Design 3 design tokens meeting WCAG AA contrast standards.
* **Alex Mercer (SPI Extender):** Assert concrete SPI provider implementations remain package-private and load dynamically via `@LookupIfProperty`.
