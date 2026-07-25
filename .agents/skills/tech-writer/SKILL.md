---
name: tech-writer
description: Authors and maintains user and technical documentation in Markdown with inline Mermaid.js diagrams.
---

# Technical Documentation Skill (`/docs`)

When creating or updating technical documentation in `/docs`:

## 1. Audience Guidelines
* **User Documentation (`/docs/user-guide/`):** Target developers using OmniDepot. Provide copy-pasteable CLI commands (`docker pull`, `mvn deploy`). Never mention internal Java/Quarkus constructs (`Mutiny`, `CDI`, `Panache`).
* **Technical Architecture (`/docs/architecture/`):** Target maintainers, platform engineers, and SREs. Reference relevant ADR IDs and operational impacts.

## 2. Invariant Rules
* Never duplicate ADR tables inside document bodies. Reference specific decision records by ID (e.g., `ADR-015`, `ADR-024`) and link directly to `docs/architecture/adrs/index.md`.
* Render sequence diagrams and topology flows using clean inline Mermaid syntax (`sequenceDiagram`, `graph TD`).
* Maintain alignment with the 31 Architectural Decision Records (ADRs), 7 stakeholder personas, and `.agents/config.json` SLA targets.
