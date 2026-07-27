# omnidepot Documentation

Welcome to the **omnidepot** documentation portal. The omnidepot package repository is an open-source, high-performance polyglot package repository supporting **OCI Distribution**, **Maven/Gradle**, and **NPM** formats, designed as an **Evolutionary Modular Monolith** on **Java 25 LTS** and **Quarkus 3.37+**.

---

## Documentation Sections

```
┌───────────────────────────────────────────────────────────────────────────┐
│                          omnidepot Documentation                          │
└──────────────┬─────────────────────────────┬──────────────────────────────┘
               │                             │
  ┌────────────▼────────────┐   ┌────────────▼────────────┐   ┌─────────────▼────────────┐
  │   User Documentation    │   │  Developer Onboarding   │   │       Architecture       │
  ├─────────────────────────┤   ├─────────────────────────┤   ├──────────────────────────┤
  │ • Quick Start & Docker  │   │ • Prerequisites (JDK25) │   │ • Modular Monolith DDD   │
  │ • OCI V2 Distribution   │   │ • Container Setup       │   │ • Hexagonal Storage SPI  │
  │ • Maven & Gradle Setup  │   │ • Fast Feedback Loops   │   │ • Database & Liquibase   │
  │ • NPM Registry Config   │   │ • Quarkus Live Dev Mode │   │ • ADRs (ADR-001..031)    │
  └─────────────────────────┘   └─────────────────────────┘   └──────────────────────────┘
```

### [Developer Onboarding & Environment Guide](file:///home/developer/projects/omnidepot/docs/developer-guide/index.md)
Complete step-by-step developer documentation for setting up a local development environment, starting Docker containers, running database migrations, executing fast local feedback loops, and running Quarkus live dev mode. Also includes the [Story Creation Workflow Guide](file:///home/developer/projects/omnidepot/docs/developer-guide/story-creation-workflow.md) and [Autonomous AI Story Execution Workflow Guide](file:///home/developer/projects/omnidepot/docs/developer-guide/ai-story-workflow.md) for processing stories with `/goal`.

### [User Documentation](file:///home/developer/projects/omnidepot/docs/user-guide/index.md)
Instructions on installing, running, and configuring omnidepot as a developer or administrator. Learn how to push and pull Docker/OCI images, publish Java Maven/Gradle artifacts, and host NPM packages.

### [Architecture](file:///home/developer/projects/omnidepot/docs/architecture/index.md)
Technical specifications detailing the Evolutionary Modular Monolith design, Hexagonal DDD SPI boundaries, Liquibase database migrations, and 31 Architectural Decision Records (ADRs).

---

## Key Highlights

- **Java 25 LTS:** Harnesses Virtual Threads, Records, Sealed Classes, and Pattern Matching.
- **Polyglot Protocol Adapters:** Native support for OCI V2 (`/v2/`), Maven (`/maven/`), and NPM (`/npm/`).
- **Content-Addressable Storage (CAS):** SHA-256 digest deduplication across Filesystem and AWS S3/RustFS providers.
- **Strict Architecture Boundaries:** ArchUnit enforcement guarantees clean module isolation and prevents unintended cross-domain coupling.
