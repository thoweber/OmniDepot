# STORY-004: NPM Package Tarball Ingestion & Registry Metadata Adapter

> [!NOTE]
> **Context & Architecture Reference:** NPM Registry protocol adapter in `omnidepot-format-npm` (ADR-004, ADR-005).

---

## 1. User Story

As **[Alex Mercer (Third-Party SPI Extension Developer)](file:///home/developer/projects/OmniDepot/docs/architecture/stakeholders-personas.md#persona-7-alex-mercer--third-party-spi-extension-developer)**, I want **omnidepot** to accept NPM package publish payloads containing metadata and base64 tarball attachments, so that `npm`, `yarn`, and `pnpm` CLIs can publish and install Node.js packages.

---

## 2. Product Goal Contribution

- **Polyglot Node.js Ecosystem Expansion:** Completes the core polyglot triad (OCI, Maven, NPM) within omnidepot, enabling full frontend and backend JavaScript artifact governance inside the Evolutionary Modular Monolith.

---

## 3. Value Generation

- **Developer Experience:** Enables standard `npm publish` and `npm install` CLI operations with zero proprietary CLI plugins required.
- **Operational Reliability:** Atomic `dist-tags` updates and version immutability prevent corrupt NPM package resolution during concurrent CI deployments.

---

## 4. Scope & Sub-modules

* `omnidepot-format-npm`
* `omnidepot-core-domain`
* `omnidepot-storage-api`

---

## 5. REST Endpoints & Key Invariants

* `PUT /npm/{package}` (Publish package metadata & base64 tarball attachment)
* `GET /npm/{package}` (Retrieve full CouchDB package metadata document)
* `GET /npm/{package}/-/{tarball}` (Stream raw package tarball binary)
* **Dist-Tags & Conflict Guard:** Update `dist-tags` (`latest`) atomically and reject duplicate version publishes with HTTP 409 Conflict.

---

## 6. `test-manager` Protocol Verification Strategy

* **Level 1 (In-Memory Unit & Snapshot):**
  * `NpmMetadataTest.java`: `package.json` extraction, SHA-512/SHA-1 integrity hash verification, `dist-tags` JSON document updates.
* **Level 2 (API Contract & Schema Fuzzing):**
  * `NpmRegistryContractCT.java`: CouchDB-compatible NPM payload contract fuzzing, base64 attachment decoding, and 409 Conflict version collision handling.
* **Level 3 (Black-Box E2E Native CLI via Testcontainers):**
  * `NpmNativeCliIT.java`: Spawns a Node.js container executing raw `npm publish` and `npm install` against live `omnidepot-app`.

---

## 7. Acceptance Criteria

- [ ] Parse JSON publish payload and extract base64 encoded `.tgz` tarball attachment into CAS.
- [ ] Serve spec-compliant CouchDB package metadata documents via `GET /npm/{package}`.
- [ ] Enforce atomic `dist-tags` (`latest`) updates and HTTP 409 Conflict on duplicate version publish.
- [ ] JSpecify `@NullMarked` on all package boundaries.
- [ ] Sub-30-second local test suite execution.
