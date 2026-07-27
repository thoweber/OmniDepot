# OmniDepot: Repository Agent Guidelines & Autonomous Guardrails

> **Notice for Antigravity CLI Agent (`agy`):**
> This file defines non-negotiable repository guardrails, local feedback verification loops, and the mandatory Test-Driven Development (TDD) execution protocol for autonomous multi-file development in OmniDepot.

---

## 1. Tech Stack & Runtime Baseline

* **JDK Runtime:** Java 25 LTS
* **Core Framework:** Quarkus 3.37.4 + Eclipse Vert.x (Non-blocking reactive streaming)
* **Architecture Style:** Evolutionary Modular Monolith (15 Maven reactor modules under `io.omnidepot`)
* **Databases:** PostgreSQL 16+ (Production / Clustered), Embedded H2 (Dev / Test)
* **Database Migrations:** Liquibase dual-dialect XML changelogs (`omnidepot-infra-db`)
* **Front-End:** Angular 18+ Standalone Components & Signals (`@omni-depot/ui`)
* **Storage Provider:** Content-Addressable Storage (CAS) on AWS S3 / RustFS and Local Filesystem

---

## 2. Local Feedback Loops & Verification Commands

Remote CI/CD runs are too slow for autonomous iteration. Execute these fast local verification commands inside `agy`'s loop to self-correct in under 30 seconds:

* **Compile & Fast Unit Tests ($< 10\text{ s}$):**  
  `./mvnw test -Dtest=*Test`
* **Component & ArchUnit Verification ($< 30\text{ s}$):**  
  `./mvnw test -Dtest=*CT,ArchitectureBoundaryTest`
* **Full Reactor Build & Verification ($< 60\text{ s}$):**  
  `./mvnw clean verify`
* **Auto-Format Code:**  
  `./mvnw spotless:apply` (or IDE SCSS/TS formatters for `@omni-depot/ui`)
* **Database Changelog Validation (Mandatory Dual-Dialect H2 & PostgreSQL):**  
  `node .agents/scripts/liquibase-validator-server.js` (or `validate_liquibase_changelog` tool)

---

## 3. Mandatory Development Protocol (Strict TDD)

All feature development, bug fixes, and protocol adapter additions MUST follow the 3-phase TDD loop:

```text
  ┌───────────────┐        ┌───────────────┐        ┌───────────────┐
  │   1. RED      │ ─────► │   2. GREEN    │ ─────► │  3. REFACTOR  │
  │ Write Failing │        │ Minimal Prod  │        │ Apply Clean   │
  │ Test (*Test)  │        │ Code to Pass  │        │ Format & Audit│
  └───────────────┘        └───────────────┘        └───────────────┘
```

1. **RED Phase:** Write unit or component tests (`*Test.java` or `*CT.java`) using AssertJ assertions (`assertThat()`). Run `./mvnw test` to confirm the test fails with an expected assertion failure or stack trace.
2. **GREEN Phase:** Write minimal production code until `./mvnw test` passes cleanly.
3. **REFACTOR Phase:** Run `./mvnw spotless:apply` and perform static analysis / Sonar clean-up. Never declare success with unformatted code or open quality regressions.

---

## 4. Autonomous Agent Directives

1. **Never Guess Schemas or Paths:** Always inspect authoritative files (`pom.xml`, `config.json`, Liquibase XML, SPI interfaces) before generating code.
2. **Strict Encapsulation:** Concrete implementations must remain `package-private`. Only interfaces in `omnidepot-core-api` are `public`.
3. **Jakarta Validation & @NullMarked:** Enforce boundary checks using Jakarta Validation (`@NotNull`, `@NotBlank`, `@Valid`) at REST/Kafka/DB entrypoints. Never use `org.junit.jupiter.api.Assertions`. All production packages require `@NullMarked` in `package-info.java`.
4. **Hermetic Testcontainers:** Integration tests (`*IT.java`) must bring up isolated containers (PostgreSQL 16, RustFS, Kafka) and tear down without leaving state side-effects.
5. **Pipeline & DevOps Automation:** Whenever building or updating CI/CD build pipelines (`.github/workflows/*`), Dockerfiles (`src/main/docker/*`), Kubernetes manifests, or Helm charts, ALWAYS activate and adhere to the `devops-engineer` skill.
6. **Mandatory SonarCloud Audit:** Before declaring completion on any feature, bug fix, or refactoring task, the agent MUST inspect SonarCloud for open code smells, bugs, or vulnerabilities via the `sonarcloud` MCP server (or `sonar-remediation` skill), ensuring zero open critical or major issues.
7. **Mandatory Dual-Dialect Liquibase Validation:** Whenever creating or modifying any Liquibase XML changelog under `omnidepot-infra-db/src/main/resources/db/changelog/`, the agent MUST execute dual-dialect validation across BOTH `h2` and `postgresql` 16+ using the `liquibaseValidator` MCP tool (or `node .agents/scripts/liquibase-validator-server.js`) and ensure 0 errors and successful rollback cycles before declaring completion.
8. **Mandatory Global Writing Standards:** Whenever producing technical documentation (`/docs`), agent summaries, rule files, or pull request descriptions, the agent MUST strictly adhere to the global writing rules in [.agents/rules/04-writing-standards.md](file:///home/developer/projects/OmniDepot/.agents/rules/04-writing-standards.md) (concise GFM, active voice, mandatory `file://` scheme links, strategic GFM alerts, clean Mermaid diagrams, zero fluff, zero emojis except sparingly in root `README.md`, and 100% lowercase product branding `omnidepot`).
9. **Mandatory Test-Manager Protocol Verification Breakdown:** Whenever planning new feature stories, protocol adapters, or story definitions, the agent MUST activate the `test-manager` skill and provide explicit Level 1 (In-Memory Unit & Adapter Snapshot), Level 2 (API Contract & Schema Fuzzing), and Level 3 (Black-Box E2E Native CLI via Testcontainers) test scenario specifications where applicable.
10. **Mandatory Persona Traceability & Value Generation Rules:** Whenever authoring story definitions, user stories, or architecture context, the agent MUST activate the `product-owner` skill, format persona references on first mention as `[Name (Role)]` hyperlinked directly to [docs/architecture/stakeholders-personas.md](file:///home/developer/projects/OmniDepot/docs/architecture/stakeholders-personas.md), and explicitly include **Product Goal Contribution** and **Value Generation** sections.
11. **Mandatory Lead-Architect Design & Coding Governance:** Whenever planning new feature stories, designing features, writing domain code, or refactoring modules, the agent MUST activate the `lead-architect` skill and enforce Hexagonal DDD isolation, SOLID/CUPID principles, strongly-typed Value Objects (preventing primitive obsession), pre-allocated `StringBuilder` capacity on hot paths, JSpecify `@NullMarked` nullability rules, static `isNull()` / `nonNull()` checks, and zero raw exception handling.
12. **Mandatory GitHub Sub-Issue Tracking:** Whenever executing story goals with `/goal`, the agent MUST decompose multi-step story tasks into explicit GitHub **sub-issues** with meaningful descriptions (specifying target sub-module `omnidepot-*`, scope, and acceptance criteria), track execution progress, and close sub-issues with test evidence before closing the parent story issue.
13. **Mandatory Feature Branch Naming Pattern:** Whenever processing a feature story or issue, the agent MUST create and execute work on a dedicated Git feature branch following the exact pattern `feature/STORY-XXX-short-description-not-too-long` (e.g. `feature/STORY-002-oci-manifest-ingestion`). Direct commits to `main` during feature story execution are strictly forbidden.
14. **Mandatory GitHub CLI (`gh`) Environment Assumption:** Whenever executing `gh` CLI commands, the agent MUST ALWAYS assume `GITHUB_TOKEN` is already present in the execution environment and MUST NOT prepend inline token assignments (`GITHUB_TOKEN=` or `GH_TOKEN=`) to command lines.
