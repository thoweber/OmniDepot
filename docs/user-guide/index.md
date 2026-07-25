# User Documentation

Welcome to the OmniDepot User Guide. This section provides step-by-step instructions for running OmniDepot, configuring local development environments, and interfacing with all supported package format endpoints.

---

## 📖 Topics Covered

- **[Getting Started](getting-started.md):** Prerequisites, Docker Compose startup, MinIO S3 configuration, and Caddy TLS proxy setup.
- **[OCI V2 Distribution Registry](oci-distribution.md):** Pushing and pulling container images using `docker`, `podman`, or `nerdctl`.
- **[Maven / Gradle Repository](maven-repository.md):** Deploying and resolving JARs, POMs, and checksums (`.sha1`, `.sha256`, `.md5`) with Apache Maven or Gradle.
- **[NPM Registry](npm-registry.md):** Publishing and downloading Node.js tarballs and package metadata using `npm` or `yarn`.

---

## 🛠️ Quick System Check

Before using OmniDepot, start the infrastructure services using Docker Compose:

```bash
docker-compose up -d
```

Verify system health:

```bash
curl -f http://localhost:8080/q/health
```
