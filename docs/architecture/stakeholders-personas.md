# OmniDepot: Stakeholder Matrix & Persona Context Specification

> **Master Specification for OmniDepot Architecture & Development**
> This document defines the complete stakeholder landscape, Mendelow Power-Interest Matrix, persona profiles, and persona-driven acceptance criteria for **OmniDepot**. Use this specification during code generation, API design, UI/UX implementation, and test suite generation to ensure all technical decisions align with stakeholder requirements and SLAs.

---

## 1. Stakeholder Classification & Ecosystem Map

OmniDepot serves five distinct stakeholder groups spanning internal development teams, platform operations, enterprise executive leadership, cloud cost management, and the broader open-source ecosystem.

```text
                               OMNIDEPOT STAKEHOLDER ECOSYSTEM
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│ 1. ENTERPRISE LEADERSHIP & GOVERNANCE                                                        │
│    • Dr. Marcus Vance (Enterprise CTO & Principal Architect)                                │
│    • Thomas Dubois (OSPO Lead & Legal Counsel)                                              │
├─────────────────────────────────────────────────────────────────────────────────────────────┤
│ 2. PLATFORM OPERATIONS & SECURITY                                                           │
│    • Sven Lindqvist (Senior SRE & Platform Engineering Lead)                                │
│    • Elena Rostova (Lead DevSecOps Engineer & Security Auditor)                             │
├─────────────────────────────────────────────────────────────────────────────────────────────┤
│ 3. END-USER DEVELOPERS & EXTENDERS                                                          │
│    • Mateo Rossi (Senior Full-Stack Developer & Application Engineer)                       │
│    • Alex Mercer (Third-Party Format & Storage SPI Contributor)                             │
├─────────────────────────────────────────────────────────────────────────────────────────────┤
│ 4. FINANCIAL & INFRASTRUCTURE OWNERS                                                        │
│    • Priya Sharma (Cloud FinOps Lead & Infrastructure Cost Owner)                           │
├─────────────────────────────────────────────────────────────────────────────────────────────┤
│ 5. AUTOMATED SYSTEMS & TOOLING                                                              │
│    • CI/CD Pipeline Bots (GitHub Actions, GitLab CI, Jenkins runners)                      │
│    • Automated Dependency Managers (Renovate Bot, Dependabot)                              │
└─────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Mendelow Power-Interest Stakeholder Matrix

Stakeholders are mapped according to their **Influence (Power)** over system decisions and their **Interest** in daily operational outcomes.

```text
                                  POWER / INFLUENCE
                     LOW                                      HIGH
        ┌────────────────────────────────────┬────────────────────────────────────┐
        │ KEEP SATISFIED                     │ MANAGE CLOSELY                     │
        │                                    │                                    │
        │ • Priya Sharma (FinOps Lead)       │ • Dr. Marcus Vance (CTO)           │
        │   - Wants low S3/egress spend      │   - Wants zero vendor lock-in &    │
        │   - Demands high CAS deduplication │     long-term architectural agility│
        │                                    │                                    │
        │ • Thomas Dubois (OSPO & Legal)     │ • Sven Lindqvist (Platform Lead)   │
        │   - Wants clean open-source        │   - Demands 99.99% uptime, zero   │
        │     licensing & WCAG AA UI         │     deadlocks & zero-downtime boots│
        │                                    │                                    │
  I     │                                    │ • Elena Rostova (DevSecOps)        │
  N     │                                    │   - Demands zero credential leaks  │
  T     │                                    │     & sub-millisecond JWT auth     │
  E     ├────────────────────────────────────┼────────────────────────────────────┤
  R     │ MONITOR (INFORM)                   │ KEEP INFORMED                      │
  E     │                                    │                                    │
  S     │ • CI/CD Bots & Tooling             │ • Mateo Rossi (Developer)          │
  T     │   - Need fast, deterministic       │   - Demands sub-second local boots,│
        │     pull/push responses            │     Cmd+K search & Zero-Auth dev   │
        │                                    │                                    │
        │ • External Package Registries      │ • Alex Mercer (SPI Contributor)    │
        │   - Need protocol-compliant        │   - Wants clean Hexagonal DDD      │
        │     HTTP/OCI distribution calls    │     and package-private SPIs       │
        └────────────────────────────────────┴────────────────────────────────────┘
```

---

## 3. Detailed Stakeholder Personas

### Persona 1: Mateo Rossi — Senior Full-Stack Developer

- **Role:** Application Engineer building enterprise microservices and user interfaces.
- **Primary Objective:** Frictionless local setup, ultra-fast CI build loops, and intuitive artifact navigation.
- **Pain Points:**
  - Waiting minutes for large container layers or Maven dependencies to re-upload.
  - Friction when Docker CLI fails locally due to plain HTTP / SSL handshake errors.
  - Cluttered management web UIs that force tedious mouse clicking to find a package.
- **Core Requirements:**
  1. **Zero-Auth Local Mode (`repo.auth.mode=disabled`):** Boots OmniDepot locally without configuring OAuth or LDAP.
  2. **Sub-100ms Command Palette (`Cmd+K`):** Keyboard-driven global search across all repositories, tags, and settings.
  3. **Local Dev TLS Proxy:** Works out of the box with `docker-compose` and `mkcert` (`https://localhost:8443`) without modifying `/etc/docker/daemon.json` or adding `insecure-registries` flags.
- **Key ADR Alignment:** ADR-011, ADR-014, ADR-021, ADR-023, ADR-028, ADR-029.

---

### Persona 2: Sven Lindqvist — Senior SRE & Platform Lead

- **Role:** Platform Infrastructure Lead responsible for Kubernetes clusters, service availability, and CI/CD pipelines.
- **Primary Objective:** High availability (99.99%), zero database deadlocks, low memory consumption, and deterministic container restarts.
- **Pain Points:**
  - Kubernetes Liveness probes restarting healthy containers during transient database blips.
  - Database lock contention and deadlocks caused by background workers polling scheduled tasks.
  - High memory footprint ($\ge 500\text{ MB}$) when running repository sidecars in multi-tenant clusters.
- **Core Requirements:**
  1. **Decoupled Health Probes:** Port 9000 `/q/health/live` evaluates JVM deadlocks strictly; `/q/health/ready` evaluates DB/S3 connectivity.
  2. **Non-Blocking Multi-Node Outbox (`SKIP LOCKED`):** Clustered workers poll `outbox_events` using `FOR UPDATE SKIP LOCKED`, guaranteeing zero lock wait timeouts and zero duplicate event dispatches.
  3. **Sub-80 MB Native Memory Footprint:** GraalVM Native Image compilation delivering sub-second startup and lightweight pod footprints.
- **Key ADR Alignment:** ADR-001, ADR-002, ADR-010, ADR-017, ADR-026, ADR-031.

---

### Persona 3: Elena Rostova — Lead DevSecOps & Security Auditor

- **Role:** Enterprise Cyber Security Auditor and Supply Chain Governance Officer.
- **Primary Objective:** Zero supply chain vulnerabilities, zero credential leakage, strict role-based access control (RBAC), and immutable audit logs.
- **Pain Points:**
  - Upstream registry passwords leaking into client HTTP headers or application log files.
  - High latency caused by real-time vulnerability policy checks during CI build pulls.
  - Slow database lookups on every layer download request during authentication verification.
- **Core Requirements:**
  1. **Off-Heap JWT Signature Validation:** Short-lived JWT tokens validated in-memory off-heap in $\le 1.0\text{ ms}$ on layer streaming routes without database queries.
  2. **Shift-Left Governance Flags:** Pre-computed compliance bitmasks (`GovernanceFlags`) evaluated in $\le 0.1\text{ ms}$ during download requests.
  3. **Encrypted Upstream Credentials:** Upstream secrets encrypted at rest via AES-256-GCM and injected server-side without exposure to client responses.
- **Key ADR Alignment:** ADR-008, ADR-019, ADR-022, ADR-023, ADR-027.

---

### Persona 4: Priya Sharma — Cloud FinOps Lead

- **Role:** Cloud Infrastructure Cost Owner managing AWS, Azure, and GCP monthly spend.
- **Primary Objective:** Minimizing cloud storage footprint, S3 API request costs, and cross-region egress charges.
- **Pain Points:**
  - Exponential cloud spend growth caused by duplicate container layers stored across team repositories.
  - Excessive S3 API bill charges resulting from thousands of tiny chunk upload requests ($< 5\text{ MB}$).
  - High network egress costs from downstream clients continuously re-downloading proxy artifacts.
- **Core Requirements:**
  1. **Pure Content-Addressable Storage (CAS):** Multi-coordinate $N$-to-1 mapping yielding a $70\%+$ reduction in physical S3 storage via global SHA-256 deduplication.
  2. **S3 5 MB Off-Heap Aggregation Buffering:** Accumulates incoming upload chunks off-heap to guarantee all non-final S3 multipart uploads are $\ge 5\text{ MB}$, reducing S3 `UploadPart` API costs.
  3. **Proxy Cache Revalidation TTL:** Issues lightweight HTTP `HEAD` `If-None-Match` checks on expired proxy tags; streams local CAS binaries upon `304 Not Modified` with zero upstream egress transfer.
- **Key ADR Alignment:** ADR-003, ADR-015, ADR-017, ADR-025, ADR-027.

---

### Persona 5: Dr. Marcus Vance — Enterprise CTO & Principal Architect

- **Role:** Chief Technology Officer driving long-term technology roadmaps and corporate software architecture.
- **Primary Objective:** Long-term technology longevity, zero cloud vendor lock-in, low total cost of ownership (TCO), and clear software evolution paths.
- **Pain Points:**
  - Proprietary vendor lock-in from cloud-managed artifact repositories.
  - Uncontrolled architectural complexity caused by premature microservice decomposition.
  - Unstable open-source software with fragile migration paths across database upgrades.
- **Core Requirements:**
  1. **Evolutionary Modular Monolith:** Single deployable container built with hexagonal Bounded Contexts, ensuring simple deployment today with a clear extraction path if services need isolation later.
  2. **Pluggable Storage & Identity SPIs:** Decouples core business logic from cloud providers (AWS S3, RustFS, File System) and identity vendors (OIDC, LDAP).
  3. **Java 25 LTS Ecosystem Stability:** Leverages enterprise Java 25 LTS, Quarkus, and standard SQL specifications.
- **Key ADR Alignment:** ADR-001, ADR-003, ADR-005, ADR-023, ADR-030.

---

### Persona 6: Thomas Dubois — OSPO Lead & Legal Counsel

- **Role:** Open Source Program Office Lead and Intellectual Property Attorney.
- **Primary Objective:** Strict open-source license compliance, WCAG AA accessibility, and legal safety.
- **Pain Points:**
  - Proprietary UI components or restrictive third-party licenses polluting project repositories.
  - Inaccessible management web interfaces that fail corporate accessibility compliance audits.
- **Core Requirements:**
  1. **Permissive Licensing:** 100% permissively licensed dependency stack (Apache 2.0 / MIT).
  2. **Material Design 3 Accessibility:** Angular Material 3 design tokens guaranteeing WCAG AA contrast and screen-reader accessibility.
  3. **Trunk-Based Governance:** Transparent open-source contribution rules and automated CI testing gates.
- **Key ADR Alignment:** ADR-011, ADR-012, ADR-030.

---

### Persona 7: Alex Mercer — Third-Party SPI Extension Developer

- **Role:** Open-source contributor or enterprise developer adding custom storage backends or package formats (e.g., PyPI, Cargo, Helm).
- **Primary Objective:** Clear domain boundaries, intuitive Extension SPIs, and isolated package scopes.
- **Pain Points:**
  - Having to modify core database schemas or application code to add a new package format.
  - Tight coupling between format wire protocols and storage layer implementations.
- **Core Requirements:**
  1. **Hexagonal DDD Isolation:** Format modules (`repo-format-*`) depend strictly on `repo-core-api` interfaces with zero direct database or storage module access.
  2. **Package-Private SPI Implementations:** SPI concrete classes remain `package-private` and are registered dynamically via Quarkus `@LookupIfProperty`.
  3. **Automated Boundary Verification:** ArchUnit tests enforce package isolation automatically during local builds.
- **Key ADR Alignment:** ADR-003, ADR-005, ADR-009, ADR-020.

---

## 4. Persona-to-ADR Traceability Matrix

This table maps every stakeholder persona to their required architectural decisions, quality goals, and operational SLAs.

| Persona | Primary Focus Area | Associated ADRs | Measurable Quality Target / SLA |
| :--- | :--- | :--- | :--- |
| **Mateo Rossi** *(Developer)* | DX, Local Auth, Search & Pushes | ADR-011, ADR-014, ADR-021, ADR-023, ADR-028, ADR-029 | • Local dev boot $\le 1.0\text{s}$ (H2)<br>• Global search $\le 100\text{ ms}$ (`Cmd+K`)<br>• Cross-repo mount $\le 1.0\text{ ms}$ |
| **Sven Lindqvist** *(Platform/SRE)* | HA, Probes, Locks & Concurrency | ADR-001, ADR-002, ADR-010, ADR-017, ADR-026, ADR-031 | • Idle Native RAM $\le 80\text{ MB}$<br>• 0 SQL deadlocks in multi-node outbox<br>• 100% liveness decoupling on port 9000 |
| **Elena Rostova** *(DevSecOps)* | Security, JWT Auth & Governance | ADR-008, ADR-019, ADR-022, ADR-023, ADR-027 | • Off-heap JWT verification $\le 1.0\text{ ms}$<br>• Governance check $\le 0.1\text{ ms}$<br>• 0 upstream secret leaks in logs/headers |
| **Priya Sharma** *(FinOps)* | CAS Storage & Bandwidth Reduction | ADR-003, ADR-015, ADR-017, ADR-025, ADR-027 | • Storage redundancy ratio $= 1.0\times$ (70%+ S3 savings)<br>• S3 non-final parts $\ge 5\text{ MB}$<br>• Proxy $304\text{ Not Modified}$ egress $= 0\text{ bytes}$ |
| **Dr. Marcus Vance** *(CTO)* | Monolith Strategy & Vendor Decoupling | ADR-001, ADR-003, ADR-005, ADR-023, ADR-030 | • Zero cloud vendor SDKs in `repo-core-domain`<br>• Single deployable container<br>• Java 25 LTS ecosystem longevity |
| **Thomas Dubois** *(OSPO/Legal)* | Licensing & UI Accessibility | ADR-011, ADR-012, ADR-030 | • 100% Apache 2.0 / MIT permissively licensed stack<br>• WCAG AA contrast compliance in UI |
| **Alex Mercer** *(SPI Extender)* | Hexagonal Boundaries & Clean SPIs | ADR-003, ADR-005, ADR-009, ADR-020 | • 0 boundary violations in ArchUnit tests<br>• SPI implementations marked `package-private` |

---

## 5. Persona-Driven Acceptance & Testing Guidelines

When generating code, REST endpoints, UI components, or test suites, developer tools MUST enforce the following persona-specific acceptance criteria:

### 1. Developer DX Rules (Mateo Rossi)
- **Zero-Auth Support:** Ensure endpoints check `repo.auth.mode`. If set to `disabled`, synthesize a `DevAdminPrincipal` and bypass HTTP 401 challenges.
- **Local TLS Proxy Compatibility:** Do not force TLS configuration into application properties. Ensure local setup instructions use the Caddy sidecar on `https://localhost:8443`.
- **Command Palette Integration:** Provide REST endpoints returning lean JSON search results for `Cmd+K` global queries in $\le 100\text{ ms}$.

### 2. SRE & Platform Operations Rules (Sven Lindqvist)
- **Probe Isolation:** Never register readiness checks (e.g., PostgreSQL connection or S3 ping) under `/q/health/live`. Liveness checks belong exclusively on port 9000 checking JVM responsiveness.
- **Outbox Concurrency:** Ensure outbox database polling queries always append `FOR UPDATE SKIP LOCKED`.
- **Zero-GC Streaming:** Use Vert.x reactive streams (`Multi<Buffer>`) for file downloads. Do not copy byte arrays into Java heap memory.

### 3. Security Auditor Rules (Elena Rostova)
- **JWT Verification:** Validate JWT scopes off-heap using public key signatures without executing database queries during artifact downloads.
- **Credential Encryption:** Ensure any upstream credential fields in database entities are annotated with `AES-256-GCM` attribute converters.

### 4. FinOps Cost Optimization Rules (Priya Sharma)
- **CAS Deduplication:** Ensure physical storage writes always compute payload SHA-256 digests and store files under `/blobs/sha256/<hash>`. Duplicate uploads must reuse existing blobs.
- **S3 Part Buffering:** Ensure `S3BlobStore` accumulates incoming upload chunks in off-heap Netty memory buffers until size reaches $\ge 5,242,880\text{ bytes}$ before executing `s3Client.uploadPart()`.
