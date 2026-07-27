---
name: devops-engineer
description: Containerization, Kubernetes manifests, Helm charts, Docker multistage builds, GitHub Actions workflows, matrix builds, RustFS/S3 storage integration, and CI/CD automation for omnidepot.
version: 1.0.0
---

# DevOps Engineer Skill (`devops-engineer`)

This skill governs containerization, Kubernetes manifest orchestration, Helm charts, Docker multistage builds, GitHub Actions CI/CD workflows, and production cloud infrastructure automation for omnidepot.

---

## 1. Core Philosophy

* **Fast feedback:** unit and component tests must complete in under 30 seconds; full end-to-end integration checks in under 5 minutes.
* **Ephemeral infrastructure:** use Testcontainers or ephemeral Docker Compose stacks (`postgres:16`, `rustfs/rustfs:latest`) that spin up, execute, and tear down without leaving residual state.
* **Zero secret hardcoding:** secrets (JWT private keys, S3 access keys, database passwords) MUST be injected via environment variables, Kubernetes Secrets, or GitHub Actions Encrypted Secrets — never inlined as plaintext in workflow or config files.

---

## 2. Docker & Container Standards

* **Multi-stage builds:** stage 1 builds the full Maven reactor with `eclipse-temurin:25-jdk-alpine`; stage 2 copies only the Quarkus fast-jar layout into a minimal `eclipse-temurin:25-jre-alpine` runtime image.
* **Module copy order:** copy modules in dependency order (`omnidepot-core-api` → `omnidepot-core-domain` → storage → infra → format → `omnidepot-app`) to maximise Docker layer caching.
* **Exposed ports:** `8080` (application), `9000` (management/health).
* **Non-root user:** containers MUST run as unprivileged user `1001:1001` (`USER 1001`).
* **Distroless / Alpine runtime:** use minimal base images; never ship a JDK in the runtime stage.
* **Build flag:** skip tests during Docker build (`-DskipTests`); testing is the CI pipeline's responsibility.

---

## 3. GitHub Actions CI/CD Pipeline

> Canonical pipeline definitions: [ci.yml](file:///home/developer/projects/OmniDepot/.github/workflows/ci.yml) and [e2e-matrix.yml](file:///home/developer/projects/OmniDepot/.github/workflows/e2e-matrix.yml).

* **Triggers:** `push` to `main` and `pull_request` targeting `main`.
* **Maven cache:** use `actions/cache` keyed on `pom.xml` hash, targeting `~/.m2/repository`.
* **JDK:** `actions/setup-java@v4` with `java-version: '25'` and `distribution: 'temurin'`.
* **Step order:**
  1. `./mvnw spotless:check` — format compliance gate.
  2. `./mvnw -B clean verify sonar:sonar -Dsonar.qualitygate.wait=true` — build, test, and SonarCloud quality gate.
* **Service containers:** use `postgres:16-alpine` and `rustfs/rustfs:latest` as GitHub Actions service containers; inject credentials via `${{ secrets.* }}` only.
* **Artifact uploads:** upload JaCoCo aggregated coverage reports as pipeline artifacts.

---

## 4. Kubernetes & Helm Standards

* **Health probes:** liveness → `GET /q/health/live` on port `9000`; readiness → `GET /q/health/ready` on port `9000`.
* **Liveness scope:** checks JVM thread deadlocks only. MUST NOT probe database or S3 connectivity.
* **Readiness scope:** evaluates PostgreSQL connection pool and RustFS S3 bucket accessibility.
* **Security context:** `runAsNonRoot: true`, `runAsUser: 1001`, `readOnlyRootFilesystem: true`, `allowPrivilegeEscalation: false`.
* **Resource requests/limits:** define explicit CPU and memory requests and limits on every container spec.
* **Replicas:** minimum 3 replicas for production deployments to support rolling updates without downtime.
* **Image pull policy:** `IfNotPresent` for versioned tags; `Always` only for mutable floating tags (avoid floating tags in production).
