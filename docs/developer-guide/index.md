# OmniDepot Developer Onboarding & Environment Guide

Welcome to the **OmniDepot Developer Onboarding Guide**. This document outlines the prerequisites, infrastructure container setup, database migration workflows, local feedback loops, and live development commands for contributing to OmniDepot.

---

## 1. Local Development Topology

The following diagram illustrates the interaction between your local IDE/CLI, the Quarkus live development server, containerized infrastructure, and the autonomous AI harness agent:

```mermaid
graph TD
    subgraph Developer Workstation
        IDE["IDE / Editor (IntelliJ / VS Code)"]
        AGY["Antigravity CLI Agent (agy-env.sh)"]
        QUARKUS["Quarkus App Live Dev (repo-app: 8080)"]
    end

    subgraph Docker Infrastructure
        PG["PostgreSQL 16 (Port 5432)"]
        RUSTFS["RustFS S3 CAS Storage (Port 9000)"]
    end

    subgraph Cloud Services
        SONAR["SonarCloud Analysis (OmniDepot)"]
        GH["GitHub Actions CI / PR Checks"]
    end

    IDE -->|Edit Code & Spotless| QUARKUS
    AGY -->|Execute Fast Feedback Loops| QUARKUS
    QUARKUS -->|Liquibase / Hibernate SQL| PG
    QUARKUS -->|S3 CAS Stream Ingestion| RUSTFS
    AGY -->|Upload Static Analysis| SONAR
    AGY -->|Push Feature Branches & PRs| GH
```

---

## 2. Prerequisites

Ensure the following tools are installed on your workstation prior to setting up the environment:

| Requirement | Supported Version | Purpose |
| :--- | :--- | :--- |
| **Java JDK** | **Java 25 LTS** (Temurin / OpenJDK) | Production runtime & reactor build (`ADR-001`) |
| **Maven** | 3.9+ (or `./mvnw` wrapper) | Multi-module reactor build engine |
| **Container Engine** | Docker 24+ or Podman 4+ | Infrastructure service containers (`docker compose`) |
| **Node.js & npm** | Node.js 20+ / npm 10+ | Front-end SPA (`@omni-depot/ui`) & MCP proxies |
| **Git** | 2.40+ | Version control & feature branch workflows |

---

## 3. Step-by-Step Developer Setup

### Step 1: Clone Repository & Create Local Secrets
Clone the repository and instantiate your local environment credentials file (`agy.env`):

```bash
git clone https://github.com/thoweber/OmniDepot.git
cd OmniDepot

# Copy example environment configuration to agy.env
cp agy.env.example agy.env
```

Open `agy.env` in your text editor and populate your credentials:
```bash
GITHUB_TOKEN=ghp_yourPersonalAccessToken
SONAR_TOKEN=your_sonarcloud_token
```

---

### Step 2: Start Infrastructure Containers
Bring up the ephemeral PostgreSQL 16 database and RustFS Content-Addressable Storage (CAS) S3 containers:

```bash
# Start PostgreSQL 16 and RustFS S3 containers in background
docker compose up -d postgres rustfs

# Verify PostgreSQL readiness
docker compose exec -T postgres pg_isready -U omnidepot -d omnidepot
```

---

### Step 3: Run Database Migrations
Execute Liquibase changelog updates and rollback validations against the local development database:

```bash
# Validate dual-dialect XML changelogs against DB
./mvnw compile liquibase:updateTestingRollback -pl repo-infra-db
```

---

### Step 4: Execute Local Verification Loops ($< 30\text{ s}$)
To iterate fast without waiting for remote CI/CD, run the sub-30-second local verification loops enforced by `AGENTS.md`:

```bash
# 1. Compile & Fast Unit Tests (< 10 s)
./mvnw test -Dtest=*Test

# 2. Component & ArchUnit Boundary Tests (< 30 s)
./mvnw test -Dtest=*CT,ArchitectureBoundaryTest

# 3. Apply Code Auto-Formatting & Full Reactor Build (< 60 s)
./mvnw spotless:apply && ./mvnw clean verify
```

---

### Step 5: Start Quarkus Live Development Mode
Launch the application in Quarkus Live Coding mode (`repo-app`). Code changes recompile automatically on hot-reload:

```bash
# Start Quarkus Live Dev server
./mvnw quarkus:dev -pl repo-app
```

* **Application HTTP Port:** `http://localhost:8080`
* **Quarkus Dev UI:** `http://localhost:8080/q/dev/`
* **Health Endpoint:** `http://localhost:8080/q/health`
* **Management Port:** `http://localhost:9000`

---

### Step 6: Launch Antigravity AI Agent Harness
To run the autonomous AI pair programmer equipped with all local credentials:

```bash
# Launch agy with agy.env variables automatically loaded
./agy-env.sh
```

---

## 4. Key Repository Guidelines & Invariants

* ** Jakarta Validation & `@NullMarked`:** Enforce JSpecify `@NullMarked` in `package-info.java` across production packages.
* ** Strongly-Typed Domain Exceptions:** Never throw raw `RuntimeException` or `Exception` in production code. Throw domain-appropriate exceptions (`StorageException`, `BlobWriteException`, `OciProtocolException`).
* ** 1-to-1 Target-Class Test Mapping:** Every production class must have a dedicated 1-to-1 unit test class (`<ClassName>Test.java`).
* ** Pre-sized `StringBuilder` Capacity:** Hot-path URI/header formatting must allocate explicit capacity to eliminate dynamic array resizing.
* ** Architecture Isolation:** Concrete implementations remain `package-private`. Only interfaces in `repo-core-api` are `public`.
