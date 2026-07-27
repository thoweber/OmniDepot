# OmniDepot: AI Harness & Agent System Context Specification

> **Master Specification for OmniDepot AI Governance & `antigravity-cli`**
> This document defines the complete AI Harness specification (`AGENTS.md`, `.agents/` directory structure, configuration schema, coding rules, quality goals, custom agent skills, and MCP server integrations) for **OmniDepot**. Use this specification to initialize, configure, and govern automated code generation, refactoring, test creation, and architectural auditing within `antigravity-cli`.

---

## 1. AI Harness Directory Topology

The AI Harness consists of `AGENTS.md` in the repository root and the `.agents/` governance directory.

```text
AGENTS.md                              # Root repository agent guidelines & TDD protocol
.agents/
├── config.json                        # Single Source of Truth for SLAs, ports, modules & MCP servers
├── rules/                             # Invariant rules loaded into prompt context
│   ├── 01-architecture-invariants.md  # Core domain & architectural boundary rules
│   ├── 02-coding-standards.md         # Java 25, Vert.x, Angular Signals & Liquibase patterns
│   └── 03-quality-and-slas.md         # Verification procedures & performance thresholds
├── skills/                            # Executable agent skills (/commands)
│   ├── security-analyst/              # Boundary-only validation, normalization-first, JWT security
│   ├── tdd-runner/                    # Red-Green-Refactor loop & fast sub-30s verification
│   ├── sonar-remediation/             # Static analysis, code smells & Sonar quality gates
│   ├── test-manager/                  # Quality strategy, protocol verification (L1/L2/L3)
│   ├── test-engineer/                 # 3-tier test pyramid (*Test/*CT/*IT), AssertJ & ArchUnit
│   ├── backend-engineer/              # Back-end engineering, Java 25, Quarkus, Mutiny & DDD
│   ├── frontend-engineer/             # Front-end engineering, Angular M3, Signals & Storybook
│   ├── tech-writer/                   # Documentation standards & Mermaid diagram authoring
│   ├── format-adapter-scaffolder/    # Scaffolds new package format modules
│   ├── liquibase-changelog/           # Generates dual-dialect Liquibase changelogs
│   ├── archunit-boundary-checker/    # Audits package visibility & DDD rules
│   ├── sla-benchmark-runner/          # Generates k6/Gatling load test scripts
│   └── persona-e2e-generator/        # Generates persona-driven acceptance tests
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
    "tombstoneGracePeriodHours": 48,
    "localFeedbackSlaSeconds": 30,
    "targetBranchCoveragePercent": 80
  },
  "database": {
    "productionDialect": "postgresql",
    "developmentDialect": "h2",
    "migrationEngine": "liquibase",
    "changelogMasterPath": "repo-infra-db/src/main/resources/db/changelog/db.changelog-master.xml"
  },
  "skills": {
    "tddRunner": ".agents/skills/tdd-runner/SKILL.md",
    "sonarRemediation": ".agents/skills/sonar-remediation/SKILL.md",
    "testManager": ".agents/skills/test-manager/SKILL.md",
    "testEngineer": ".agents/skills/test-engineer/SKILL.md",
    "backendEngineer": ".agents/skills/backend-engineer/SKILL.md",
    "frontendEngineer": ".agents/skills/frontend-engineer/SKILL.md",
    "techWriter": ".agents/skills/tech-writer/SKILL.md",
    "formatAdapterScaffolder": ".agents/skills/format-adapter-scaffolder/SKILL.md",
    "liquibaseChangelog": ".agents/skills/liquibase-changelog/SKILL.md",
    "archunitBoundaryChecker": ".agents/skills/archunit-boundary-checker/SKILL.md",
    "slaBenchmarkRunner": ".agents/skills/sla-benchmark-runner/SKILL.md",
    "personaE2eGenerator": ".agents/skills/persona-e2e-generator/SKILL.md"
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
      "description": "IntelliJ IDEA Model Context Protocol Server for IDE context inspection and navigation",
      "env": {
        "INTELLIJ_PORT": "64343"
      }
    },
    "github": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-github"],
      "description": "GitHub Model Context Protocol Server for repository issues, PRs, and workflow inspection",
      "env": {
        "GITHUB_PERSONAL_ACCESS_TOKEN": "${GITHUB_TOKEN}"
      }
    }
  }
}
```

---

## 3. Custom Agent Skills (`.agents/skills/`)

- **`security-analyst`:** Boundary-only null checks & validations via JSpecify `@NullMarked`, mandatory input normalization-first rules, raw data discarding, hot-path zero-DB JWT cryptographic validation, and path traversal prevention.
- **`tdd-runner`:** Enforces the Red (failing test) $\rightarrow$ Green (minimal production code) $\rightarrow$ Refactor (clean format) execution loop with sub-30-second local feedback loops.
- **`sonar-remediation`:** Static analysis code smell remediation, nullability checks, resource leak prevention, and 80%+ branch coverage enforcement.
- **`test-manager`:** Overlooks quality strategy, protocol verification levels (Level 1 Snapshot, Level 2 Fuzzing, Level 3 Native Client E2E), mutation score thresholds ($\ge 80\%$), zero flakiness enforcement, and feedback speed protection.
- **`test-engineer`:** 3-tier test pyramid (*Test/*CT/*IT), AssertJ exclusive assertion rules, `@DisplayName("Given ... - when ... - then ...")` pattern, ArchUnit boundary rules, and JaCoCo 80%+ branch coverage enforcement.
- **`backend-engineer`:** Java 25 LTS, Quarkus 3.37+, Vert.x Mutiny reactive streams (`Multi<Buffer>`), Hexagonal DDD encapsulation, Panache `@JdbcTypeCode(SqlTypes.JSON)` mappings, and off-heap zero-GC streaming.
- **`frontend-engineer`:** Angular 18+, Material Design 3 (M3), Signals, `@omni-depot/ui` presentational component rules, and Storybook theme switcher setup.
- **`/docs` (`tech-writer`):** Documentation generation adhering to User vs Architecture isolation rules and Mermaid standards.
- **`/add-format` (`format-adapter-scaffolder`):** Scaffolds new package format modules (`repo-format-<name>`) with package-private routes and ArchUnit boundaries.
- **`/new-migration` (`liquibase-changelog`):** Generates versioned XML changelogs with `dbms="postgresql"` and `dbms="h2"` qualifiers and mandatory `<rollback>` blocks.
- **`/check-boundaries` (`archunit-boundary-checker`):** Audits package visibility, CDI `@LookupIfProperty` annotations, and DDD encapsulation.
- **`/gen-benchmark` (`sla-benchmark-runner`):** Generates automated k6 benchmark scripts validating SLA thresholds from `config.json`.
- **`persona-e2e-generator`:** Generates persona-driven acceptance tests for Mateo, Sven, Elena, Priya, Marcus, Thomas, and Alex.
- **`devops-engineer`:** Containerization, Kubernetes manifests, Helm charts, Docker multistage builds, GitHub Actions workflows, matrix builds, RustFS/S3 storage integration, and CI/CD automation.
