---
name: lead-architect
description: Enforces Evolutionary Modular Monolith Hexagonal DDD architecture, SOLID/CUPID principles, Java 25 coding conventions, and ArchUnit boundary invariants for omnidepot.
---

# Lead Architect & Design Principles Skill (`lead-architect`)

This skill governs the high-level software architecture, Domain-Driven Design (DDD), Hexagonal Architecture boundaries, SOLID/CUPID design principles, Java 25 coding conventions, and architectural invariants across **omnidepot**.

> [!IMPORTANT]
> **Mandatory Story Planning Skill:** The `lead-architect` skill **MUST** be activated during story planning, feature design, code generation, and refactorings to ensure all proposed capabilities align with omnidepot's Evolutionary Modular Monolith Hexagonal DDD invariants.

---

## 1. Modular Monolith & Hexagonal DDD Architecture (`ADR-001`, `ADR-004`)

The repository is structured as an **Evolutionary Modular Monolith** across **15 Maven reactor sub-modules** enforcing strict **Hexagonal Architecture (Ports and Adapters)**:

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│                             PROTOCOL ADAPTERS                               │
│      omnidepot-format-oci │ omnidepot-format-maven │ omnidepot-format-npm  │
└───────────────────────────────────┬─────────────────────────────────────────┘
                                    │ (Depends ONLY on API)
┌───────────────────────────────────▼─────────────────────────────────────────┐
│                          omnidepot-core-api (SPIs)                          │
└───────────────────────────────────▲─────────────────────────────────────────┘
                                    │ (Implements / Consumes)
┌───────────────────────────────────┴─────────────────────────────────────────┐
│                          omnidepot-core-domain                              │
├───────────────────────────────────┬─────────────────────────────────────────┤
│      omnidepot-storage-fs │ omnidepot-storage-s3   │ omnidepot-infra-db      │
└───────────────────────────────────┴─────────────────────────────────────────┘
```

### Encapsulation & Visibility Invariants
1. **Public API Surface:** Only interfaces, SPIs, tagging interfaces, and Value Objects in `omnidepot-core-api` are `public`.
2. **Package-Private Implementations:** All concrete provider implementations (`FileSystemBlobStore`, `S3BlobStore`, `OciDistributionResource`, `MavenRepositoryResource`) **MUST** remain `package-private` to enforce compilation-level isolation (`ADR-004`).
3. **Zero Circular Dependencies:** Protocol adapters (`omnidepot-format-*`) depend strictly on `omnidepot-core-api` and `omnidepot-storage-api`. They MUST NEVER import directly from concrete storage or DB modules.

---

## 2. SOLID Design Principles

* **Single Responsibility Principle (SRP):** Keep classes tightly focused on one domain responsibility. Decouple REST protocol mappers, catalog domain services, storage SPIs, and transactional outbox publishers.
* **Open/Closed Principle (OCP):** Extend functionality (new package formats or storage backends) by implementing SPI interfaces in `omnidepot-core-api` without modifying core domain code.
* **Liskov Substitution Principle (LSP):** Concrete SPI implementations must adhere strictly to interface contract behavior and throw domain-specific exceptions (`StorageException`), never unhandled runtime exceptions.
* **Interface Segregation Principle (ISP):** Author fine-grained, targeted SPI interfaces (`BlobStore`, `TokenBroker`, `UploadSessionRepository`) rather than monolithic fat interfaces.
* **Dependency Inversion Principle (DIP):** High-level domain logic depends strictly on SPI abstractions, injecting implementations via Quarkus `@LookupIfProperty` on `@ApplicationScoped` beans.

---

## 3. CUPID Principles (Modern Joyful Software)

* **Composable:** Build small, stateless, reusable domain functions and immutable Value Objects.
* **Unix Philosophy:** Each module and class does one thing well with explicit inputs and outputs.
* **Predictable:** Zero surprises. Pure functions, immutable records, explicit `Optional` chains, and static nullability checks.
* **Idiomatic:** Native utilization of Java 25 LTS features (`record`, `sealed` interfaces, pattern matching, Virtual Threads) and Quarkus 3.37 CDI patterns.
* **Domain-Based:** Code structure, package layouts, and class names reflect the domain's ubiquitous language (`Sha256Digest`, `CasPath`, `BlobSize`, `GAV`, `dist-tags`).

---

## 4. Coding Conventions & Invariants

### A. Primitive Obsession Prevention
Encapsulate all domain primitives into strongly-typed Java 25 `record` Value Objects implementing tagging interfaces:
* **`Sha256Digest`**: Validated 64-character hexadecimal SHA-256 hash.
* **`CasPath`**: Calculated Content-Addressable Storage path (`blobs/sha256/xx/yy/...`).
* **`BlobSize`**: Non-negative byte length with shared static `BlobSize.ZERO` singleton.
* **`UploadSessionId`**: Unique upload session identifier.
* **`OciRepositoryName`**: Normalized, validated OCI namespace.

### B. Nullability Guardrails
* **JSpecify `@NullMarked`:** All production packages require `@NullMarked` in `package-info.java`.
* **Static Null Checks:** Never use raw `== null` or `!= null`. Statically import and use `isNull(val)` or `nonNull(val)`.
* **No `null` Returns:** Query methods return `Optional<T>`. Collection methods return immutable empty collections (`List.of()`, `Set.of()`).

### C. Functional Optional Chains over Ternary Branching
Avoid ternary conditional branching (`a ? b : c`). Use functional `Optional` pipelines for parameter normalization and fallback handling (`Optional.ofNullable().filter().orElseThrow()`).

### D. Zero Raw Exceptions Rule
Prohibit throwing or catching raw `RuntimeException` or `Exception` in production code. Throw domain-appropriate exceptions (`StorageException`, `BlobWriteException`, `OciProtocolException`).

---

## 5. Architectural Quality Gates

* **ArchUnit Boundary Enforcement:** Run `./mvnw test -Dtest=ArchitectureBoundaryTest` to verify zero visibility or layer boundary violations.
* **1-to-1 Unit Test Coverage:** Every production class MUST have a corresponding unit test class (`<ClassName>Test.java`).
