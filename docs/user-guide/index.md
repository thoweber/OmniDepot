# User Documentation

Welcome to the omnidepot user guide. This section provides step-by-step instructions for running omnidepot, configuring local development environments, and interfacing with all supported package format endpoints.

---

## Topics Covered

- **[Getting Started](file:///home/developer/projects/omnidepot/docs/user-guide/getting-started.md):** Prerequisites, Docker Compose startup, RustFS S3 configuration, and Caddy TLS proxy setup.
- **[OCI V2 Distribution Registry](file:///home/developer/projects/omnidepot/docs/user-guide/oci-distribution.md):** Pushing and pulling container images using `docker`, `podman`, or `nerdctl`.
- **[Maven / Gradle Repository](file:///home/developer/projects/omnidepot/docs/user-guide/maven-repository.md):** Deploying and resolving JARs, POMs, and checksums (`.sha1`, `.sha256`, `.md5`) with Apache Maven or Gradle.
- **[NPM Registry](file:///home/developer/projects/omnidepot/docs/user-guide/npm-registry.md):** Publishing and downloading Node.js tarballs and package metadata using `npm` or `yarn`.

---

## Quick System Check

Before using omnidepot, start the infrastructure services using Docker Compose:

```bash
docker-compose up -d
```

Verify system health:

```bash
curl -f http://localhost:8080/q/health
```
