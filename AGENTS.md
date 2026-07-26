# OmniDepot: Repository Agent Guidelines & Autonomous Guardrails

> **Notice for Antigravity CLI Agent (`agy`):**
> This file defines non-negotiable repository guardrails, local feedback verification loops, and the mandatory Test-Driven Development (TDD) execution protocol for autonomous multi-file development in OmniDepot.

---

## 1. Tech Stack & Runtime Baseline

* **JDK Runtime:** Java 25 LTS
* **Core Framework:** Quarkus 3.37.4 + Eclipse Vert.x (Non-blocking reactive streaming)
* **Architecture Style:** Evolutionary Modular Monolith (15 Maven reactor modules under `io.omnidepot`)
* **Databases:** PostgreSQL 16+ (Production / Clustered), Embedded H2 (Dev / Test)
* **Database Migrations:** Liquibase dual-dialect XML changelogs (`repo-infra-db`)
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
* **Database Changelog Validation:**  
  `./mvnw compile liquibase:updateTestingRollback -pl repo-infra-db`

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
2. **Strict Encapsulation:** Concrete implementations must remain `package-private`. Only interfaces in `repo-core-api` are `public`.
3. **Jakarta Validation & @NullMarked:** Enforce boundary checks using Jakarta Validation (`@NotNull`, `@NotBlank`, `@Valid`) at REST/Kafka/DB entrypoints. Never use `org.junit.jupiter.api.Assertions`. All production packages require `@NullMarked` in `package-info.java`.
4. **Hermetic Testcontainers:** Integration tests (`*IT.java`) must bring up isolated containers (PostgreSQL 16, RustFS, Kafka) and tear down without leaving state side-effects.
