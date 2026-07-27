# Deployment Topologies & Operational Matrix

OmniDepot supports three validated deployment matrix topologies ranging from local developer setups to clustered high-availability enterprise environments (ADR-010, ADR-029, ADR-031).

---

## Supported Topologies

| Topology | Database | Storage Backend | Identity / Auth | TLS Termination | Eventing |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **1. Dev Machine** | Embedded H2 | Local Filesystem | Zero-Auth (`disabled`) | Caddy Sidecar (`:8443`) | Vert.x EventBus |
| **2. Enterprise Standard** | PostgreSQL 16+ | RustFS / AWS S3 | TokenBroker (PAT/JWT) | Ingress / Gateway | Vert.x EventBus |
| **3. Clustered HA** | PostgreSQL 16+ (Clustered) | RustFS / AWS S3 | TokenBroker (OIDC/PAT) | Ingress / Service Mesh | Apache Kafka |

---

## 🩺 Decoupled Health Probes (ADR-010)

Management endpoints operate on isolated port **9000**:
- **Liveness Probe (`/q/health/live`):** Evaluates JVM responsiveness and deadlock detection only. Never executes database or network I/O.
- **Readiness Probe (`/q/health/ready`):** Evaluates database pool connectivity, S3 storage access, and messaging broker status.

---

## 🔒 Local TLS Proxy (ADR-029)

Local development uses a lightweight Caddy reverse proxy sidecar terminating trusted HTTPS on port `8443` (`mkcert`) and forwarding traffic to Quarkus port `8080`.
