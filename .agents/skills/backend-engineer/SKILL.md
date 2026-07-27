---
name: backend-engineer
description: Back-end architecture, Hexagonal DDD, Java 25 LTS, Quarkus 3.37, Vert.x Mutiny reactive streaming, Panache ORM, and SPI implementation standards for omnidepot.
version: 1.0.0
tech_stack:
  jdk: Java 25 LTS
  framework: Quarkus 3.37+
  reactive_engine: Eclipse Vert.x & Mutiny (Multi<Buffer>)
  orm: Hibernate ORM with Panache (Jackson / JSONB @JdbcTypeCode)
  migration: Liquibase (Dual Dialect: PostgreSQL 16 & H2)
  architecture: Hexagonal DDD / Modular Monolith
---

# Back-End Architecture & Engineering Skill: omnidepot

This document defines the core standards, reactive streaming mechanics, domain boundary rules, database mapping patterns, and SPI implementation protocols for omnidepot backend services.

---

## 1. Domain & Boundary Protection Invariants

### A. Boundary Rules
1. **Format Module Isolation (ADR-005):** `omnidepot-format-*` modules MUST depend ONLY on `omnidepot-core-api`. They MUST NEVER import directly from `omnidepot-storage-*`, `omnidepot-infra-db-*`, or `omnidepot-domain-iam`.
2. **CDI Dynamic Selection:** Inject storage and identity backends via Quarkus `@LookupIfProperty` on `@ApplicationScoped` beans:

```java
@ApplicationScoped
@LookupIfProperty(name = "repo.storage.provider", stringValue = "s3")
class S3BlobStore implements BlobStore { ... }
```

---

## 2. Java 25 LTS & Vert.x Mutiny Reactive Rules

### A. Non-Blocking Event-Loop Rules (ADR-002, ADR-006)
- **Reactive Stream Buffers:** Process binary streams using Mutiny `Multi<Buffer>` or Vert.x `ReadStream<Buffer>`.
- **Zero-GC Off-Heap Transfers:** Pass direct Netty `ByteBuf` instances directly from network sockets to storage channels without transferring byte arrays into the Java heap.
- **Worker Offloading:** Never execute blocking I/O (e.g., synchronous disk/database operations) on Vert.x event loops. Offload blocking execution explicitly:

```java
public Uni<Response> handleBlockingIngest(String digest) {
    return Uni.createFrom().item(() -> processSync(digest))
              .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
}
```

### B. Modern Java 25 Constructs
- **Immutable Value Objects:** Use `record` for DTOs, value objects, and domain events.
- **Sealed Interfaces:** Use `sealed` interfaces for domain events and command results.

---

## 3. Database Mapping & Persistence Standards

### A. Panache Entity JSONB Mapping
Annotate complex JSON fields (`attributes`, `provider_state`) using Hibernate 6 `@JdbcTypeCode(SqlTypes.JSON)`:

```java
@Entity
@Table(name = "upload_sessions")
public class UploadSessionEntity extends PanacheEntityBase {

    @Id
    @Column(name = "id", nullable = false)
    public UUID id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "provider_state", columnDefinition = "jsonb")
    public Map<String, Object> providerState = new HashMap<>();
}
```

### B. Non-Blocking Outbox Queries (ADR-026)
Background worker outbox queries polling `outbox_events` MUST append `FOR UPDATE SKIP LOCKED` to guarantee zero lock wait timeouts across application nodes.

---

## 4. Operational & Health Probe Rules (ADR-010)

Management endpoints operate on isolated port **9000**:
- **Liveness (`/q/health/live`):** Checks JVM thread deadlocks and responsiveness only. NEVER execute database or network I/O.
- **Readiness (`/q/health/ready`):** Evaluates database connectivity, S3 storage access, and messaging brokers.
