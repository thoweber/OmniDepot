---
name: test-engineer
description: Test engineering standards, 3-tier test pyramid (*Test/*CT/*IT), JaCoCo branch coverage targets, and Testcontainers protocols for omnidepot.
version: 1.0.0
---

# Test Engineering Skill (`test-engineer`)

This skill governs unit, component, integration, and architecture testing standards across omnidepot.

---

## 1. 3-Tier Test Pyramid Standards

| Test Tier | File Naming | SLA / Execution Target | Focus & Scope |
| :--- | :--- | :--- | :--- |
| **Tier 1: Unit Tests** | `*Test.java` | $< 10\text{ ms}$ per test | Isolated unit logic, domain record validation, Value Object invariants. |
| **Tier 2: Component Tests** | `*CT.java` | $< 500\text{ ms}$ per test | Sub-system slice, Mockito/CDI mock integration, ArchUnit boundary tests. |
| **Tier 3: Integration Tests** | `*IT.java` | $< 5\text{ s}$ per test | Native client CLI black-box execution against Testcontainers (PostgreSQL 16, RustFS). |

---

## 2. Execution Invariants

1. **Zero Flakiness:** Tests must be deterministic and self-cleaning.
