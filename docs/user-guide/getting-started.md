# Getting Started with OmniDepot

This guide walks you through setting up and running OmniDepot on your local machine.

---

## 📋 Prerequisites

- **Java Development Kit (JDK):** Java 25 LTS
- **Build Tool:** Apache Maven 3.9+ (or included `./mvnw` wrapper)
- **Container Engine:** Docker & Docker Compose

---

## 🚀 Building & Running Locally

### 1. Build the Monolith Reactor

To clean, compile, run all tests, and package the application:

```bash
./mvnw clean verify
```

### 2. Start Infrastructure Dependencies

OmniDepot requires PostgreSQL 16+, RustFS S3 object storage, and Caddy reverse proxy:

| Service | Default Port | Internal Endpoint / Health Check |
| :--- | :--- | :--- |
| **Quarkus App** | `8080` (HTTP) / `9000` (Management) | `http://localhost:8080` |
| **Caddy TLS Sidecar** | `8443` (HTTPS) | `https://localhost:8443` |
| **PostgreSQL 16** | `5432` | `localhost:5432` (User: `omnidepot` / Pass: `omnidepot_secret`) |
| **RustFS S3** | `9000` / `9001` | `http://localhost:9000` (User: `omnidepot_rustfs` / Pass: `omnidepot_rustfs_secret`) |
| **Caddy Proxy** | `8443` | `https://localhost:8443/` |

### 3. Launch OmniDepot in Quarkus Dev Mode

Start OmniDepot with live coding and instant hot reload:

```bash
./mvnw quarkus:dev -pl repo-app
```

Navigate to the Quarkus Dev UI at `http://localhost:8080/q/dev/`.
