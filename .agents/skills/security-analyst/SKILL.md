---
name: security-analyst
description: Security analysis, secure data handling, input normalization before validation, zero-trust boundary validation, JWT token security, and OWASP compliance for OmniDepot.
version: 1.0.0
---

# Security Analyst & Data Handling Skill (`security-analyst`)

This skill governs security analysis, secure data handling, boundary validation, input normalization, and cryptographic hygiene across OmniDepot.

---

## 1. Core Security & Data Handling Principles

### A. Boundary-Only Validation & JSpecify Nullability Rule
* **Boundary Invariant:** We use **JSpecify** (`@NullMarked`, `@Nullable`) to define nullability contracts across all production packages.
* **Boundary Validation:** Null-checks, size checks, and payload validations MUST be performed ONLY at application boundaries (i.e., REST Controllers, Vert.x HTTP Handlers, Kafka Event Consumers, and Database Repositories).
* **Internal Domain Methods:** Internal domain methods within `@NullMarked` packages assume non-null parameters and MUST NOT pollute domain logic with redundant defensive null checks (`Objects.requireNonNull` or `if (param == null)`).

### B. Normalization-Before-Validation Rule
* **Mandatory Ordering:** When data processing involves normalization (e.g., path canonicalization, Unicode normalization, lowercasing hashes, stripping trailing slashes), **data MUST always be normalized FIRST and THEN validated**.
* **Discard Unnormalized Data:** Unnormalized raw inputs MUST be immediately discarded in favor of the canonicalized, normalized representation. Validation rules MUST execute exclusively against the normalized object.

```java
// Example: Digest Normalization & Boundary Validation
public static Sha256Digest parseBoundaryInput(String rawDigest) {
    // 1. Normalize first
    String normalized = rawDigest.trim().toLowerCase(Locale.ROOT);
    if (normalized.startsWith("sha256:")) {
        normalized = normalized.substring(7);
    }

    // 2. Validate normalized data
    if (!HEX_64_PATTERN.matcher(normalized).matches()) {
        throw new IllegalArgumentException("Invalid SHA-256 digest format: " + rawDigest);
    }

    // 3. Discard raw unnormalized input; return object wrapping normalized data
    return new Sha256Digest(normalized);
}
```

---

## 2. Secure Data Handling Invariants

### A. Path Traversal & CAS Security (ADR-015)
* **CAS Keying:** Content-Addressable Storage (CAS) paths are key-derived: `blobs/sha256/{digest[0..2]}/{digest[2..4]}/{digest}`.
* **Path Sanitization:** Reject any path containing relative directory traversal tokens (`..`, `%2e%2e`, `/./`), null bytes (`%00`), or illegal filesystem characters before passing to `BlobStore`.

### B. Hot-Path JWT Security & Zero-DB Auth (ADR-019)
* **In-Memory Cryptography:** Validate CLI-signed JWT bearer tokens in-memory off-heap (`quarkus-smallrye-jwt`) using public key certificates.
* **No DB Bottlenecks:** Never invoke database queries during high-throughput layer streaming endpoints (`/v2/<name>/blobs/<digest>`). Target SLA $\le 1.0\text{ ms}$ (P99).

### C. Secrets & Credential Hygiene
* **Zero Secret Leakage:** Never log raw bearer tokens, basic auth credentials, or private keys. Sanitize log outputs.
* **Encryption at Rest:** Store upstream proxy credentials and PAT tokens encrypted in `repo-infra-db`.
