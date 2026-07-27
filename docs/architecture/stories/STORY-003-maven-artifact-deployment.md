# STORY-003: Maven Artifact Deployment & Layout Resolution Endpoint

> [!NOTE]
> **Context & Architecture Reference:** Maven/Gradle protocol adapter in `omnidepot-format-maven` (ADR-004, ADR-005).

---

## 1. User Story

As **[Priya Sharma (Cloud FinOps Lead)](file:///home/developer/projects/OmniDepot/docs/architecture/stakeholders-personas.md#persona-4-priya-sharma--cloud-finops-lead)**, I want **omnidepot** to accept Maven layout artifact deployments (JARs, POMs, source packages) and synthesize missing checksums (`.sha1`, `.sha256`, `.md5`), so that Apache Maven and Gradle builds can publish and resolve dependencies without build warnings.

---

## 2. Product Goal Contribution

- **Enterprise JVM Ecosystem Integration:** Fulfills omnidepot's polyglot commitment for Java/Kotlin/Scala ecosystems, unifying Maven artifact storage with OCI container image storage in a single CAS repository.

---

## 3. Value Generation

- **FinOps & Egress Value:** Eliminates duplicate JAR downloads across build agents via SHA-256 CAS deduplication and in-memory proxy cache revalidation.
- **Developer Velocity:** Automatic checksum synthesis (`.sha1`, `.sha256`, `.md5`) prevents broken build warnings and failed CI resolution steps.

---

## 4. Scope & Sub-modules

* `omnidepot-format-maven`
* `omnidepot-core-domain`
* `omnidepot-storage-api`

---

## 5. REST Endpoints & Key Invariants

* `PUT /maven/{releases|snapshots}/{path:.+}` (Deploy artifact or checksum file)
* `GET /maven/{releases|snapshots}/{path:.+}` (Download artifact or auto-synthesize checksum)
* `HEAD /maven/{releases|snapshots}/{path:.+}` (Verify artifact existence)
* **Checksum Synthesis:** Automatically compute missing `.sha1`, `.sha256`, `.md5` files on-the-fly when requested.

---

## 6. `test-manager` Protocol Verification Strategy

* **Level 1 (In-Memory Unit & Snapshot):**
  * `MavenLayoutTest.java`: In-memory GAV coordinate parsing (`groupId/artifactId/version/file`), layout path calculation, and checksum hash generation.
* **Level 2 (API Contract & Schema Fuzzing):**
  * `MavenRepositoryContractCT.java`: REST API contract testing for `PUT /maven/{path}` and `GET /maven/{path}`.
  * Schema contract fuzzing for missing checksum files, malformed GAV paths, and snapshot vs release repository policy enforcement.
* **Level 3 (Black-Box E2E Native CLI via Testcontainers):**
  * `MavenNativeCliIT.java`: Spawns a Maven container executing raw `mvn deploy:deploy-file` against live `omnidepot-app`, followed by dependency resolution via `mvn dependency:get`.

---

## 7. Acceptance Criteria

- [ ] Support GAV path deployment for `.jar`, `.pom`, `.war`, and checksum files via `PUT`.
- [ ] Synthesize missing `.sha1`, `.sha256`, and `.md5` checksums dynamically during `GET` requests.
- [ ] Enforce snapshot vs release repository immutability policies.
- [ ] JSpecify `@NullMarked` on all package boundaries.
- [ ] Sub-30-second local test suite execution.
