---
name: tdd-runner
description: Autonomous Test-Driven Development (TDD) execution skill enforcing the Red-Green-Refactor loop, AssertJ rules, and sub-30-second local feedback loops for OmniDepot.
version: 1.0.0
---

# Test-Driven Development (TDD) Runner Skill (`tdd-runner`)

This skill governs the autonomous TDD execution flow within `antigravity-cli` (`agy`).

---

## 1. The TDD Execution Loop

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│                             1. RED PHASE                                    │
│  - Create or update test class (*Test.java or *CT.java)                    │
│  - Use Given-When-Then @DisplayName structure                              │
│  - Assert behavior using AssertJ (assertThat, assertThatThrownBy)          │
│  - Run: `./mvnw test -Dtest=TargetTest` -> Confirm FAIL with stack trace     │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            2. GREEN PHASE                                   │
│  - Implement minimal production code to satisfy failing assertions          │
│  - Run: `./mvnw test -Dtest=TargetTest` -> Confirm PASS                      │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           3. REFACTOR PHASE                                 │
│  - Format code via `./mvnw spotless:apply`                                  │
│  - Verify ArchUnit boundaries: `./mvnw test -Dtest=ArchitectureBoundaryTest`│
│  - Verify overall reactor build: `./mvnw test`                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Assertion & Verification Rules

* **AssertJ Exclusive:** Only use `org.assertj.core.api.Assertions.assertThat`. Standard JUnit assertions (`org.junit.jupiter.api.Assertions`) are strictly forbidden and blocked by ArchUnit.
* **Given-When-Then Naming:** Every test method must be annotated with `@DisplayName("Given [precondition] - when [action] - then [expected outcome]")`.
* **Zero Flakiness:** Tests must be hermetic and repeatable without depending on thread execution order or random ports.

---

## 3. Fast Verification Commands

* **Single Test Class ($< 5\text{ s}$):**  
  `./mvnw test -Dtest=MyTargetTest`
* **Unit Tests Suite ($< 10\text{ s}$):**  
  `./mvnw test -Dtest=*Test`
* **Architecture Boundary Audit ($< 5\text{ s}$):**  
  `./mvnw test -Dtest=ArchitectureBoundaryTest`
