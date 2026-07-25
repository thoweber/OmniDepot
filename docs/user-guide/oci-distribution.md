# OCI V2 Distribution Registry

OmniDepot implements the official **OCI Distribution Specification (v2.0)** for container images, Helm charts, and arbitrary OCI artifacts.

---

## ⚡ Supported Endpoints

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/v2/` | Base API check (returns `Docker-Distribution-API-Version: registry/2.0`) |
| `HEAD` / `GET` | `/v2/{name}/blobs/{digest}` | Retrieve blob by SHA-256 digest |
| `POST` | `/v2/{name}/blobs/uploads/` | Initiate resumable upload session |
| `PUT` | `/v2/{name}/blobs/uploads/{uuid}?digest={digest}` | Complete blob upload |
| `HEAD` / `GET` | `/v2/{name}/manifests/{reference}` | Get image manifest by tag or digest |
| `PUT` | `/v2/{name}/manifests/{reference}` | Push image manifest |

---

## 🐋 Using with Docker CLI

### 1. Login to OmniDepot

```bash
docker login localhost:8080 -u devuser -p devpassword
```

### 2. Tag & Push an Image

```bash
docker tag alpine:latest localhost:8080/my-org/alpine:1.0.0
docker push localhost:8080/my-org/alpine:1.0.0
```

### 3. Pull an Image

```bash
docker pull localhost:8080/my-org/alpine:1.0.0
```
