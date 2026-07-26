---
name: sonar-remediation
description: Static analysis audit and SonarQube / SonarScanner code smell remediation skill enforcing zero-bug, zero-vulnerability, and 80%+ branch coverage rules for OmniDepot.
version: 1.0.0
---

# SonarQube Static Analysis & Code Smell Remediation Skill (`sonar-remediation`)

This skill governs static analysis checks, code smell remediation, security vulnerability fixes, SonarCloud MCP Server integration, and SonarQube quality gate compliance.

---

## 1. Mandatory Task Workflow Directives

* **Mandatory Pre-Completion Audit:** Before concluding any feature, bug fix, or refactoring task, the agent MUST inspect SonarCloud for open code smells, bugs, security hotspots, or Quality Gate regressions using the `sonarcloud` MCP server (or `./mvnw sonar:sonar`).
* **Zero Open Regressions:** The agent MUST resolve any critical/major code smells or bugs introduced during the task before declaring completion.

---

## 2. Quality Gate Thresholds

* **Vulnerabilities & Security Hotspots:** $0$ allowed (Blocker/Critical).
* **Bugs & Reliability Rating:** Grade A ($0$ open bugs).
* **Maintainability Code Smells:** Grade A.
* **Branch Coverage Target:** $\ge 80\%$ aggregated across all 15 reactor modules (`repo-coverage-report`).
* **Duplicated Lines Density:** $< 3.0\%$.

---

## 3. Common Remediation Patterns

### A. Nullability Guardrails
Use JSpecify `@NullMarked` and `@Nullable` annotations. Avoid returning raw `null` from SPI methods; return `Optional<T>` or custom result objects.

### B. Exception Handling & Logging
Do not swallow exceptions or log and rethrow the same exception. Log structured context or wrap in domain exceptions:

```java
try {
    processBlob(digest);
} catch (IOException e) {
    throw new StorageException("Failed to ingest CAS blob for digest: " + digest, e);
}
```

### C. Resource & Memory Leak Prevention
Ensure all Netty `ByteBuf` streams and Vert.x file channels are closed or released in `doFinally` or `try-with-resources` blocks.

---

## 4. Local Execution & SonarCloud MCP Commands

* **SonarCloud MCP Inspection (`sonarcloud` server in `.agents/config.json`):**
  Uses `SONAR_TOKEN` environment variable to query open issues, security hotspots, and Quality Gate status directly for project `OmniDepot`.
* **Run Spotless Auto-Formatter:**  
  `./mvnw spotless:apply`
* **Run Aggregated Coverage & Verification:**  
  `./mvnw clean verify`
* **Run SonarCloud Analysis:**  
  `./mvnw -B clean verify sonar:sonar -Dsonar.qualitygate.wait=true`
* **Inspect Aggregated JaCoCo Report:**  
  `repo-coverage-report/target/site/jacoco-aggregate/index.html`
