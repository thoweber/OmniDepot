---
name: test-manager
description: Overlooks quality strategy, test architecture, protocol verification levels (L1/L2/L3), mutation testing, contract coverage, and native CLI Testcontainers enforcement for omnidepot.
version: 1.0.0
---

# Test Manager & Quality Strategy Skill (`test-manager`)

This document defines the high-level quality strategy, test architecture governance, protocol testing taxonomy, and quality gates for omnidepot backend services, protocol adapters, and presentation layers.

---

## 1. Core Philosophy & Principles

* **Real-World Fidelity over Heavy Mocking:** Prefer testing against real dependencies or native client CLIs inside ephemeral environments (e.g., Testcontainers) over fragile, mock-heavy integration layers.
* **Shift-Left & Fast Feedback:** Push verification as close to code commit as possible. Units and adapter logic must execute in milliseconds; integration checks must run in under five minutes.
* **Zero Flakiness Rule:** A non-deterministic test is a failing test. Tests that intermittently fail without code changes are quarantined immediately and marked as blocking until resolved.
* **Hermetic & Ephemeral Execution:** Every test suite must bring up its own isolated environment, seed its own data, and tear down completely without leaving state side effects.

---

## 2. Test Architecture & Taxonomy

```text
                   / \
                  /   \       Level 3: E2E Native CLI Validation
                 /     \      (Real `npm`, `mvn`, `docker` via Testcontainers)
                /-------\
               /         \    Level 2: API Contract & Schema Fuzzing
              /           \   (Schemathesis, OpenAPI, WireMock)
             /-------------\
            /               \ Level 1: In-Memory Adapter Snapshot & Unit Tests
           /_________________\ (Domain logic, CAS hashing, metadata mapping)
```

### Level 1: In-Memory Unit & Adapter Snapshot Testing

* **Scope:** Protocol adapter translation, metadata extraction, Content-Addressed Storage (CAS) hashing, EVR/SemVer parsing, and internal domain logic.
* **Execution:** Fast in-memory runs alongside standard unit test execution (millisecond SLA).
* **Strategy:** Use **Snapshot Testing** (e.g., ApprovalTests, `insta`) to verify generated responses or database records against static fixtures. Catch regressions instantly without booting network servers.

### Level 2: API Contract & Schema Fuzzing

* **Scope:** Protocol compliance, REST/gRPC endpoint structures, HTTP status codes, and edge-case payload validation.
* **Execution:** Automated API contract runners and schema fuzzers (e.g., Schemathesis).
* **Strategy:** Point fuzzers directly at live service instances using OpenAPI definitions to generate boundary-condition tests, malformed inputs, and security edge cases automatically.

### Level 3: Black-Box E2E Integration (Native Client Suites)

* **Scope:** Full-stack workflows (Publish $\rightarrow$ Index $\rightarrow$ Promote $\rightarrow$ Resolve $\rightarrow$ Download) across all supported client interfaces (`docker`, `mvn`, `npm`, `pip`, `cargo`, `apt`, `dnf`).
* **Execution:** Ephemeral container environments orchestrated via Testcontainers.
* **Strategy:** Zero proxy recording. Execute raw, un-mocked native binaries against the target service. Assert both command execution success (exit code 0) and post-condition storage/database states.

---

## 3. Execution & Environment Strategy

| Environment | Purpose | Target Execution Time | Trigger |
| --- | --- | --- | --- |
| **Local Developer (TDD)** | Level 1 Snapshots & Unit Tests | $< 10$ seconds | Pre-commit / On-save |
| **Pull Request Gate** | Level 1 + Level 3 Smoke (core formats) | $< 5$ minutes | PR creation / Commit |
| **Nightly / Release Gate** | Full Level 2 Fuzzing + Level 3 Matrix (all formats) | $< 30$ minutes | Scheduled / Pre-release |

---

## 4. Governance, Metrics & Quality Gates

* **Contract Coverage:** 100% of exposed protocol endpoints must have a corresponding Level 1 snapshot test and a Level 3 E2E test.
* **Mutation Score:** Key adapter modules and version-sorting engines must pass mutation testing thresholds ($\ge 80\%$) to verify test suite quality.
* **Flakiness Threshold:** $0\%$ tolerance in production pipelines. Test failures require immediate triage: fix, quarantine, or delete.
* **Environment Independence:** Tests must pass equally on local developer machines (WSL2/Linux/macOS) and in headless CI runners without configuration changes.

---

## 5. Test Manager Responsibilities & Directives

When evaluating architecture proposals, pull requests, or quality plans, the Test Manager skill must enforce the following rules:

1. **Reject Fragile Mocks:** Block testing plans that rely heavily on manual HTTP recording proxies, dynamic regex string matching, or hardcoded mock session tokens.
2. **Mandate Native Validation:** Require every new format or adapter to include at least one automated native CLI integration test in Testcontainers.
3. **Protect Feedback Speed:** Ensure developer feedback loops remain under 5 minutes for PR builds by parallelizing containerized test execution.
4. **Audit Test Value:** Continuously prune redundant tests that mirror lower-level coverage to keep the suite lean and maintainable.
