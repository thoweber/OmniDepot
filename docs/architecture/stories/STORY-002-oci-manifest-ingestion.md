# STORY-002: OCI Image Manifest Ingestion & Layer Indexing

> [!NOTE]
> **Context & Architecture Reference:** OCI Distribution V2 REST endpoints (`/v2/`) in `omnidepot-format-oci` and `omnidepot-core-domain` (ADR-004, ADR-015).

---

## 1. User Story

As **[Mateo Rossi (Senior Full-Stack Developer)](file:///home/developer/projects/OmniDepot/docs/architecture/stakeholders-personas.md#persona-1-mateo-rossi--senior-full-stack-developer)**, I want **omnidepot** to ingest, validate, and index OCI Image Manifest V2 Schema 2 payloads and layer blob references, so that container runtimes (Docker, Podman, Kubernetes) can reliably push and pull container images over the `/v2/` API.

---

## 2. Product Goal Contribution

- **Polyglot OCI Registry Foundation:** Establishes full OCI V2 Distribution Specification compliance, enabling omnidepot to serve as a high-performance primary container registry alongside Maven and NPM formats inside a single deployable container.

---

## 3. Value Generation

- **Developer Value:** Eliminates local container runtime errors and provides seamless `docker push` / `docker pull` workflows with zero configuration overhead.
- **Operational & Storage Value:** Enforces $N$-to-1 global Content-Addressable Storage (CAS) deduplication across manifest layers, reducing S3 storage costs by $70\%+$.

---

## 4. Scope & Sub-modules

* `omnidepot-format-oci`
* `omnidepot-core-domain`
* `omnidepot-core-api`

---

## 5. REST Endpoints & Key Invariants

* `PUT /v2/{name}/manifests/{reference}` (Upload and index manifest by tag or digest)
* `GET /v2/{name}/manifests/{reference}` (Retrieve manifest JSON document with spec headers)
* `HEAD /v2/{name}/manifests/{reference}` (Check manifest existence & `Docker-Content-Digest`)
* **Layer Existence Guard:** Verify all `layers[].digest` and `config.digest` exist in Content-Addressable Storage (`omnidepot-storage-api`) prior to persistence.

---

## 6. `test-manager` Protocol Verification Strategy

* **Level 1 (In-Memory Unit & Snapshot):**
  * `OciManifestTest.java`: In-memory JSON parsing of OCI Schema 2 (`application/vnd.oci.image.manifest.v1+json`) and Docker Schema 2 (`application/vnd.docker.distribution.manifest.v2+json`), canonical SHA-256 digest computation.
  * In-memory snapshot tests for indexed catalog database entities.
* **Level 2 (API Contract & Schema Fuzzing):**
  * `OciManifestContractCT.java`: Schema contract validation for `PUT /v2/{name}/manifests/{reference}` using spec headers (`Docker-Content-Digest`, `Content-Type`).
  * Fuzzing malformed JSON bodies, unsupported media types, invalid repository namespaces, and unresolvable layer digests (`MANIFEST_BLOB_UNKNOWN` HTTP 404).
* **Level 3 (Black-Box E2E Native CLI via Testcontainers):**
  * `OciNativeDockerIT.java`: Spawns an isolated Docker daemon container, tags an image (`docker tag alpine:latest localhost:8080/my-org/alpine:1.0.0`), executes `docker push`, and verifies un-mocked `docker pull` against live `omnidepot-app`.

---

## 7. Acceptance Criteria

- [ ] Parse and validate OCI Schema 2 and Docker V2 Schema 2 JSON manifests.
- [ ] Return HTTP 404 / 400 with `MANIFEST_BLOB_UNKNOWN` payload if any referenced layer blob is missing from CAS.
- [ ] Return spec-compliant `Docker-Content-Digest` and `Content-Type` headers on `GET`/`HEAD`/`PUT`.
- [ ] JSpecify `@NullMarked` on all package boundaries.
- [ ] Sub-30-second local test suite execution.
