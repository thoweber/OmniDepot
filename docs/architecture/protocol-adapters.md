# Protocol Adapters & Wire Translators

OmniDepot isolates protocol-specific wire formats from core storage and catalog domain models using dedicated Hexagonal Protocol Adapters (ADR-004, ADR-005).

---

## 🔌 Supported Protocol Adapters

```text
               ┌────────────────────────┐
               │    Protocol Adapters   │
               └───────────┬────────────┘
                           │
    ┌──────────────────────┼──────────────────────┐
    │                      │                      │
┌───▼──────────────┐ ┌─────▼─────────────┐ ┌──────▼─────────────┐
│ repo-format-oci  │ │ repo-format-maven │ │  repo-format-npm  │
├──────────────────┤ ├───────────────────┤ ├───────────────────┤
│ • OCI V2 API     │ │ • Maven Layout    │ │ • NPM Registry    │
│ • Blob Mounts    │ │ • Auto Checksums  │ │ • Tarball Ingest  │
│ • Manifest Maps  │ │   (.sha1, .md5)   │ │ • Metadata Feeds  │
└──────────────────┘ └───────────────────┘ └───────────────────┘
```

---

## 📦 Protocol Specifications

### 1. OCI Distribution Adapter (`repo-format-oci`)
- Endpoint Prefix: `/v2/`
- Implements OCI Distribution Specification v2.0 for container images, Helm charts, and custom OCI artifacts.
- Supports **Cross-Repository Blob Mounting** (ADR-028) for zero-copy $O(1)$ layer aliasing across repositories.

### 2. Maven / Gradle Adapter (`repo-format-maven`)
- Endpoint Prefix: `/maven/`
- Serves standard Maven 2 repository layout structure (`/groupId/artifactId/version/file`).
- Synthesizes missing checksum files (`.sha1`, `.sha256`, `.md5`) dynamically from CAS digest values.

### 3. NPM Registry Adapter (`repo-format-npm`)
- Endpoint Prefix: `/npm/`
- Handles NPM package metadata JSON objects and compressed `.tgz` tarball storage.
