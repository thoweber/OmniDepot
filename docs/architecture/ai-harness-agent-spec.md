# OmniDepot: AI Harness & Agent System Context Specification

> **Master Specification for OmniDepot AI Governance & `antigravity-cli`**
> This document defines the complete AI Harness specification (`.agents/` directory structure, configuration schema, coding rules, quality goals, custom agent skills, and MCP server integrations) for **OmniDepot**. Use this specification to initialize, configure, and govern automated code generation, refactoring, test creation, and architectural auditing within `antigravity-cli`.

---

## 1. AI Harness Directory Topology

The AI Harness resides in the `.agents/` directory at the root of the repository. It acts as the brain and governance system for AI agents interacting with the codebase.

```text
.agents/
├── config.json                        # Single Source of Truth for SLAs, ports, modules & MCP servers
├── rules/                             # Invariant rules loaded into prompt context
│   ├── 01-architecture-invariants.md  # Core domain & architectural boundary rules
│   ├── 02-coding-standards.md         # Java 25, Vert.x, Angular Signals & Liquibase patterns
│   └── 03-quality-and-slas.md         # Verification procedures & performance thresholds
├── skills/                            # Executable agent skills (/commands)
│   ├── tech-writer/SKILL.md           # Documentation standards & Mermaid diagram authoring
│   ├── format-adapter-scaffolder/    # /add-format: Scaffolds new package format modules
│   ├── liquibase-changelog/           # /new-migration: Generates dual-dialect Liquibase changelogs
│   ├── archunit-boundary-checker/    # /check-boundaries: Audits package visibility & DDD rules
│   ├── sla-benchmark-runner/          # /gen-benchmark: Generates k6/Gatling load test scripts
│   ├── persona-e2e-generator/        # /gen-persona-tests: Generates persona-driven acceptance tests
│   ├── frontend-engineer/             # Front-end engineering, Angular M3, Signals & Storybook
│   ├── backend-engineer/              # Back-end engineering, Java 25, Quarkus, Mutiny & DDD
│   └── test-engineer/                 # Test engineering, 3-tier pyramid, AssertJ & ArchUnit
└── templates/                         # Offloaded templates loaded on-demand
    ├── adr-template.md                # Template for creating new ADRs
    └── mermaid-templates.md           # Standardized Mermaid sequence and C4 diagram stubs
```

---

## 2. Configuration Schema (`.agents/config.json` & `.mcp.json`)

`.agents/config.json` and `.mcp.json` provide the **Single Source of Truth (SSOT)** for all system parameters, memory limits, SLA thresholds, port assignments, and Model Context Protocol (MCP) server definitions.

```json
{
  "$schema": "https://json.schemastore.org/omnidepot-agent-config.json",
  "project": {
    "name": "OmniDepot",
    "version": "1.0.0-SNAPSHOT",
    "javaVersion": "25",
    "quarkusVersion": "3.37.4",
    "groupId": "io.omnidepot",
    "architectureStyle": "Evolutionary Modular Monolith"
  },
  "networking": {
    "applicationPort": 8080,
    "managementPort": 9000,
    "localDevTlsPort": 8443,
    "localDevHost": "https://localhost:8443"
  },
  "modules": {
    "coreApi": "repo-core-api",
    "coreDomain": "repo-core-domain",
    "storageApi": "repo-storage-api",
    "storageFs": "repo-storage-fs",
    "storageS3": "repo-storage-s3",
    "infraDb": "repo-infra-db",
    "infraOutbox": "repo-infra-outbox",
    "domainIam": "repo-domain-iam",
    "formatOci": "repo-format-oci",
    "formatMaven": "repo-format-maven",
    "formatNpm": "repo-format-npm",
    "ui": "repo-ui",
    "app": "repo-app",
    "coverageReport": "repo-coverage-report"
  },
  "qualityGoalsAndSLA": {
    "nativeMemoryIdleMb": 80,
    "nativeColdBootSeconds": 0.8,
    "jvmColdBootSeconds": 1.5,
    "hotPathJwtVerifyMsP99": 1.0,
    "governanceEvaluationMsP99": 0.1,
    "globalSearchPaletteMsP99": 100.0,
    "crossRepoMountMsP99": 1.0,
    "s3MinPartSizeBytes": 5242880,
    "proxyCacheRevalidationTtlSeconds": 60,
    "tombstoneGracePeriodHours": 48
  },
  "database": {
    "productionDialect": "postgresql",
    "developmentDialect": "h2",
    "migrationEngine": "liquibase",
    "changelogMasterPath": "repo-infra-db/src/main/resources/db/changelog/db.changelog-master.xml"
  },
  "mcpServers": {
    "archunitChecker": {
      "command": "./mvnw",
      "args": ["test", "-Dtest=ArchitectureBoundaryTest"]
    },
    "liquibaseValidator": {
      "command": "./mvnw",
      "args": ["compile", "liquibase:updateTestingRollback", "-pl", "repo-infra-db"]
    },
    "intellij": {
      "command": "npx",
      "args": ["-y", "@jetbrains/mcp-proxy"],
      "description": "IntelliJ IDEA MCP Proxy for IDE AST navigation and inspection (Port 63342)",
      "env": {
        "INTELLIJ_PORT": "63342"
      }
    },
    "github": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-github"],
      "description": "GitHub MCP Server for repository issues, pull requests, and workflow automation",
      "env": {
        "GITHUB_PERSONAL_ACCESS_TOKEN": "${GITHUB_TOKEN}"
      }
    }
  }
}
```

---

## 3. Custom Agent Skills (`.agents/skills/`)

- **`test-engineer`:** 3-tier test pyramid (*Test/*CT/*IT), AssertJ exclusive assertion rules, `@DisplayName("When ... then ...")` pattern, ArchUnit boundary rules, and JaCoCo 80%+ branch coverage enforcement.
- **`backend-engineer`:** Java 25 LTS, Quarkus 3.37+, Vert.x Mutiny reactive streams (`Multi<Buffer>`), Hexagonal DDD encapsulation, Panache `@JdbcTypeCode(SqlTypes.JSON)` mappings, and off-heap zero-GC streaming.
- **`frontend-engineer`:** Angular 18+, Material Design 3 (M3), Signals, `@omni-depot/ui` presentational component rules, and Storybook theme switcher setup.
- **`/docs` (`tech-writer`):** Documentation generation adhering to User vs Architecture isolation rules and Mermaid standards.
- **`/add-format` (`format-adapter-scaffolder`):** Scaffolds new package format modules (`repo-format-<name>`) with package-private routes and ArchUnit boundaries.
- **`/new-migration` (`liquibase-changelog`):** Generates versioned XML changelogs with `dbms="postgresql"` and `dbms="h2"` qualifiers and mandatory `<rollback>` blocks.
- **`/check-boundaries` (`archunit-boundary-checker`):** Audits package visibility, CDI `@LookupIfProperty` annotations, and DDD encapsulation.
- **`/gen-benchmark` (`sla-benchmark-runner`):** Generates automated k6 benchmark scripts validating SLA thresholds from `config.json`.
- **`/gen-persona-tests` (`persona-e2e-generator`):** Generates persona-driven acceptance tests for Mateo, Sven, Elena, Priya, Marcus, Thomas, and Alex.
