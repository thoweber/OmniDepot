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

OmniDepot requires PostgreSQL 16+, MinIO S3 object storage, and Caddy reverse proxy:

```bash
docker-compose up -d
```

| Service | Port | Endpoint / Console |
| :--- | :--- | :--- |
| **PostgreSQL** | `5432` | `jdbc:postgresql://localhost:5432/omnidepot` |
| **MinIO S3** | `9000` / `9001` | `http://localhost:9001` (User: `minioadmin` / Pass: `minioadmin`) |
| **Caddy Proxy** | `8443` | `https://localhost:8443/` |

### 3. Launch OmniDepot in Quarkus Dev Mode

Start OmniDepot with live coding and instant hot reload:

```bash
./mvnw quarkus:dev -pl repo-app
```

Navigate to the Quarkus Dev UI at `http://localhost:8080/q/dev/`.
