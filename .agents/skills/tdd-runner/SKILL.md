---
name: tdd-runner
description: Autonomous Test-Driven Development (TDD) execution skill enforcing the Red-Green-Refactor loop, AssertJ rules, and sub-30-second local feedback loops for omnidepot.
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

## 2. Hermetic Execution

* **Zero Flakiness:** Tests must be hermetic and repeatable without depending on thread execution order or random ports.


