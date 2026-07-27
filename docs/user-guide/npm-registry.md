# NPM Registry Adapter

The omnidepot repository provides NPM registry endpoints for publishing and retrieving Node.js modules and tarballs.

---

## Setting Up NPM Config

Set omnidepot as your target NPM registry:

```bash
npm config set registry http://localhost:8080/npm/
```

Or for a scoped package:

```bash
npm config set @omnidepot:registry http://localhost:8080/npm/
```

---

## Publishing Packages

Authenticate and publish:

```bash
npm login --registry=http://localhost:8080/npm/
npm publish
```
