---
name: tech-writer
description: Authors and maintains user and technical documentation in Markdown with inline Mermaid.js diagrams.
---

# Technical Documentation Skill (`/docs`)

When creating or updating technical documentation in `/docs`:

## 1. Audience Guidelines
* **User Documentation (`/docs/user-guide/`):** Target developers using omnidepot. Provide copy-pasteable CLI commands (`docker pull`, `mvn deploy`). Never mention internal Java/Quarkus constructs (`Mutiny`, `CDI`, `Panache`).
* **Technical Architecture (`/docs/architecture/`):** Target maintainers, platform engineers, and SREs. Reference relevant ADR IDs and operational impacts.

## 2. Invariant Rules
* Never duplicate ADR tables inside document bodies. Reference specific decision records by ID (e.g., `ADR-015`, `ADR-024`) and link directly to `docs/architecture/adrs/index.md`.
* Render sequence diagrams and topology flows using clean inline Mermaid syntax (`sequenceDiagram`, `graph TD`).
* Maintain alignment with the 31 Architectural Decision Records (ADRs), 7 stakeholder personas, and `.agents/config.json` SLA targets.
* Strictly enforce the global writing standards in [.agents/rules/04-writing-standards.md](file:///home/developer/projects/omnidepot/.agents/rules/04-writing-standards.md) (concise GFM, active voice, mandatory `file://` scheme links, strategic GFM alerts, zero fluff, zero emojis except sparingly in root `README.md`, and 100% lowercase product branding `omnidepot`).

## 3. Mandatory Compliance Audit Protocol
Whenever activated, audit all modified documentation files for compliance with [04-writing-standards.md](file:///home/developer/projects/OmniDepot/.agents/rules/04-writing-standards.md). Additionally, run a **Technical Setup Synchronization Check:** inspect authoritative config files (`.agents/config.json`, `pom.xml`, `docker-compose.yml`, `.agents/scripts/`) to verify that any changes to ports, CLI flags, MCP server transport commands, or environment variables are accurately reflected across all relevant documentation in `/docs` (such as [mcp-setup-guide.md](file:///home/developer/projects/OmniDepot/docs/architecture/mcp-setup-guide.md) and [developer-guide/index.md](file:///home/developer/projects/OmniDepot/docs/developer-guide/index.md)).
