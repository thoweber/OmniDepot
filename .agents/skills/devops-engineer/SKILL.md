---
name: devops-engineer
description: Containerization, Kubernetes manifests, Helm charts, Docker multistage builds, GitHub Actions workflows, matrix builds, RustFS/S3 storage integration, and CI/CD automation for OmniDepot.
version: 1.0.0
---

# DevOps Engineer Skill (`devops-engineer`)

This skill governs containerization, Kubernetes manifest orchestration, Helm charts, Docker multistage builds, GitHub Actions CI/CD workflows, and production cloud infrastructure automation for OmniDepot.

---

## 1. Core DevOps Philosophy & Directives

### A. Sub-30-Second Local Feedback & Ephemeral Environments
* **Fast Feedback Loops:** CI/CD and local development pipelines must execute verification checks in under 30 seconds for unit/component tests and under 5 minutes for full end-to-end integration tests.
* **Ephemeral Infrastructure:** Use Testcontainers or ephemeral Docker Compose stacks (`postgres:16`, `rustfs/rustfs:latest`) that spin up automatically, execute isolated tests, and tear down cleanly without leaving residual state side-effects.

### B. Rootless & Security Hardened Containers
* **Distroless Base Images:** Use minimal distroless or Alpine Linux runtime images for Quarkus JVM and Native container builds.
* **Non-Root User Enforcement:** Containers MUST run under unprivileged non-root user `1001:1001` (`USER 1001`).
* **Zero Secret Hardcoding:** Secrets (JWT private keys, S3 access keys, database passwords) MUST be injected strictly via environment variables (`env`), Kubernetes Secrets, or GitHub Actions Encrypted Secrets.

---

## 2. Docker & Containerization Standards

### Multi-Stage Quarkus JVM/Native Dockerfile Pattern (`src/main/docker/Dockerfile.jvm`)
```dockerfile
# Stage 1: Build Java 25 reactor with Maven
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /workspace
COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY repo-core-api repo-core-api
COPY repo-core-domain repo-core-domain
COPY repo-storage-api repo-storage-api
COPY repo-storage-fs repo-storage-fs
COPY repo-storage-s3 repo-storage-s3
COPY repo-infra-db repo-infra-db
COPY repo-infra-outbox repo-infra-outbox
COPY repo-domain-iam repo-domain-iam
COPY repo-format-oci repo-format-oci
COPY repo-format-maven repo-format-maven
COPY repo-format-npm repo-format-npm
COPY repo-ui repo-ui
COPY repo-app repo-app
COPY repo-coverage-report repo-coverage-report
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime Container
FROM eclipse-temurin:25-jre-alpine
ENV LANGUAGE='en_US:en'
WORKDIR /deployments
COPY --from=builder /workspace/repo-app/target/quarkus-app/lib/ /deployments/lib/
COPY --from=builder /workspace/repo-app/target/quarkus-app/*.jar /deployments/
COPY --from=builder /workspace/repo-app/target/quarkus-app/app/ /deployments/app/
COPY --from=builder /workspace/repo-app/target/quarkus-app/quarkus/ /deployments/quarkus/
EXPOSE 8080 9000
USER 1001:1001
ENTRYPOINT ["java", "-jar", "/deployments/quarkus-run.jar"]
```

---

## 3. GitHub Actions CI/CD Pipeline Standards (`.github/workflows/ci.yml`)

### Pipeline Best Practices
1. **Caching:** Cache Maven local repository (`~/.m2/repository`) using `actions/cache` keyed on `pom.xml` hash.
2. **Matrix Builds:** Execute automated matrix builds across JDK 25 and supported OS environments (Linux `ubuntu-latest`).
3. **Automated Verification:**
   - `./mvnw spotless:check` (Auto-format compliance).
   - `./mvnw test` (Fast unit & component tests).
   - `./mvnw verify` (Failsafe integration tests against PostgreSQL 16 & RustFS service containers).
4. **Artifact Uploads:** Upload JaCoCo test coverage reports and JaCoCo badge artifacts.

```yaml
name: OmniDepot CI Build Pipeline

on:
  push:
    branches: [ main, 'feature/*' ]
  pull_request:
    branches: [ main ]

jobs:
  build-and-verify:
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_DB: omnidepot
          POSTGRES_USER: omnidepot
          POSTGRES_PASSWORD: omnidepot_password
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

      rustfs:
        image: rustfs/rustfs:latest
        env:
          RUSTFS_ACCESS_KEY: omnidepot_rustfs
          RUSTFS_SECRET_KEY: omnidepot_rustfs_secret
        ports:
          - 9000:9000

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 25
        uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'temurin'
          cache: maven

      - name: Code Formatting Check
        run: ./mvnw spotless:check

      - name: Build & Verify Reactor
        run: ./mvnw clean verify
```

---

## 4. Kubernetes & Helm Manifest Standards

### Kubernetes Health Probes Protocol
* **Liveness Probe:** `GET /q/health/live` on port `9000` (Management Port).
* **Readiness Probe:** `GET /q/health/ready` on port `9000` (Verifies DB connection pool and RustFS S3 accessibility).

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: omnidepot
  namespace: omnidepot
  labels:
    app.kubernetes.io/name: omnidepot
spec:
  replicas: 3
  selector:
    matchLabels:
      app.kubernetes.io/name: omnidepot
  template:
    metadata:
      labels:
        app.kubernetes.io/name: omnidepot
    spec:
      containers:
        - name: omnidepot
          image: omnidepot/repo-app:1.0.0
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8080
              name: http
            - containerPort: 9000
              name: management
          resources:
            requests:
              memory: "256Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "1000m"
          livenessProbe:
            httpGet:
              path: /q/health/live
              port: 9000
            initialDelaySeconds: 5
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /q/health/ready
              port: 9000
            initialDelaySeconds: 10
            periodSeconds: 5
          securityContext:
            runAsNonRoot: true
            runAsUser: 1001
            readOnlyRootFilesystem: true
            allowPrivilegeEscalation: false
```
