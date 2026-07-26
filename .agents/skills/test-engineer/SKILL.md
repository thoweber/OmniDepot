---
name: test-engineer
description: Test engineering standards, 3-tier test pyramid (*Test/*CT/*IT), AssertJ assertion rules, ArchUnit boundary protection, JaCoCo branch coverage targets, and Testcontainers protocols for OmniDepot.
version: 1.0.0
---

# Test Engineering Skill (`test-engineer`)

This skill governs unit, component, integration, and architecture testing standards across OmniDepot.

---

## 1. 3-Tier Test Pyramid Standards

| Test Tier | File Naming | SLA / Execution Target | Focus & Scope |
| :--- | :--- | :--- | :--- |
| **Tier 1: Unit Tests** | `*Test.java` | $< 10\text{ ms}$ per test | Isolated unit logic, domain record validation, Value Object invariants. **1-to-1 Target Class Mapping:** Every production class MUST have its own dedicated test class (`<TargetClassName>Test.java`). Never group tests for multiple classes into a single test file. |
| **Tier 2: Component Tests** | `*CT.java` | $< 500\text{ ms}$ per test | Sub-system slice, Mockito/CDI mock integration, ArchUnit boundary tests. |
| **Tier 3: Integration Tests** | `*IT.java` | $< 5\text{ s}$ per test | Native client CLI black-box execution against Testcontainers (PostgreSQL 16, RustFS). |

---

## 2. Test Execution & Assertion Invariants

1. **AssertJ Exclusive Rule:** All assertions MUST use AssertJ (`assertThat()`, `assertThatThrownBy()`). Imports of `org.junit.jupiter.api.Assertions` are strictly forbidden and enforced by ArchUnit.
2. **Given-When-Then Display Name Structure:** All `@Test` methods MUST use `@DisplayName("Given [precondition] - when [action/trigger] - then [expected outcome]")`.
3. **1-to-1 Target Class Unit Test Mapping:** Never create generic multi-class test files (e.g. `StorageValueObjectsTest.java`). Every production class must have a dedicated matching test class (`CasPathTest.java`, `BlobSizeTest.java`, `UploadSessionIdTest.java`, `Sha256DigestTest.java`, `OciRepositoryNameTest.java`, `OciDigestTest.java`).
4. **Zero Flakiness:** Tests must be deterministic and self-cleaning.
