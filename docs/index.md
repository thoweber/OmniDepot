# OmniDepot Documentation

Welcome to the **OmniDepot** documentation portal. OmniDepot is an open-source, high-performance polyglot package repository supporting **OCI Distribution**, **Maven/Gradle**, and **NPM** formats, designed as an **Evolutionary Modular Monolith** on **Java 25 LTS** and **Quarkus 3.37+**.

---

## 📚 Documentation Sections

```
┌─────────────────────────────────────────────────────────────┐
│                    OmniDepot Documentation                  │
└──────────────┬───────────────────────────────┬──────────────┘
               │                               │
  ┌────────────▼────────────┐     ┌────────────▼────────────┐
  │   User Documentation    │     │      Architecture       │
  ├─────────────────────────┤     ├─────────────────────────┤
  │ • Quick Start & Docker  │     │ • Modular Monolith DDD  │
  │ • OCI V2 Distribution   │     │ • Hexagonal Storage SPI │
  │ • Maven & Gradle Setup  │     │ • Database & Liquibase  │
  │ • NPM Registry Config   │     │ • ADRs (ADR-001..031)   │
  └─────────────────────────┘     └─────────────────────────┘
```

### [🚀 User Documentation](user-guide/index.md)
Instructions on installing, running, and configuring OmniDepot as a developer or administrator. Learn how to push and pull Docker/OCI images, publish Java Maven/Gradle artifacts, and host NPM packages.

### [🏛️ Architecture](architecture/index.md)
Technical specifications detailing the Evolutionary Modular Monolith design, Hexagonal DDD SPI boundaries, Liquibase database migrations, and 31 Architectural Decision Records (ADRs).

---

## ⚡ Key Highlights

- **Java 25 LTS:** Harnesses Virtual Threads, Records, Sealed Classes, and Pattern Matching.
- **Polyglot Protocol Adapters:** Native support for OCI V2 (`/v2/`), Maven (`/maven/`), and NPM (`/npm/`).
- **Content-Addressable Storage (CAS):** SHA-256 digest deduplication across Filesystem and AWS S3/RustFS providers.
- **Strict Architecture Boundaries:** ArchUnit enforcement guarantees clean module isolation and prevents unintended cross-domain coupling.
