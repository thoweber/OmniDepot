# OmniDepot Architecture Overview

OmniDepot is built as an **Evolutionary Modular Monolith** designed around **Hexagonal Architecture (Ports & Adapters)** and Domain-Driven Design (DDD) principles.

---

## 🏛️ System Architecture Diagram

```mermaid
graph TD
    Client[OCI Client / Maven / NPM / Web UI] --> Proxy[Caddy Sidecar TLS Proxy :8443]
    Proxy --> REST[Quarkus HTTP REST Layer :8080]

    subgraph Protocol Adapters
        OCI[repo-format-oci]
        Maven[repo-format-maven]
        NPM[repo-format-npm]
    end

    subgraph Core Monolith
        CoreAPI[repo-core-api]
        CoreDomain[repo-core-domain]
        IAM[repo-domain-iam]
    end

    subgraph Infrastructure Providers
        FS[repo-storage-fs]
        S3[repo-storage-s3]
        DB[repo-infra-db]
        Outbox[repo-infra-outbox]
    end

    REST --> OCI
    REST --> Maven
    REST --> NPM

    OCI --> CoreAPI
    Maven --> CoreAPI
    NPM --> CoreAPI

    CoreAPI --> CoreDomain
    CoreDomain --> FS
    CoreDomain --> S3
    CoreDomain --> DB
    CoreDomain --> Outbox
```

---

## 🧩 Architectural Foundations

- **[Evolutionary Modular Monolith](evolutionary-monolith.md):** 15 decoupled Maven sub-modules enforcing compilation-level isolation.
- **[Data Model & Liquibase](data-model-liquibase.md):** PostgreSQL 16 JSONB & Embedded H2 CLOB dialect isolation with mandatory Liquibase `<rollback>` safety blocks.
- **[Security & IAM SPI](security-iam.md):** Abstract `TokenBroker` SPI supporting Bearer JWTs and basic authentication.
- **[Architectural Decision Records (ADRs)](adrs/index.md):** Complete catalog of ADRs governing system boundaries, storage, and deployment topologies.
