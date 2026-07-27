# Transactional Outbox Engine & Event-Driven Architecture

OmniDepot guarantees atomic event publishing and data consistency using the **Transactional Outbox Pattern** (ADR-016, ADR-026).

---

## 🔄 Transactional Outbox Flow

```mermaid
sequenceDiagram
    participant Client as Protocol Client
    participant App as Core Service
    participant DB as PostgreSQL Database
    participant Worker as Outbox Worker
    participant EventBus as Vert.x EventBus / Kafka

    Client->>App: Publish Package / Layer
    activate App
    App->>DB: BEGIN Transaction
    App->>DB: INSERT into coordinates
    App->>DB: INSERT into outbox_events (Status: PENDING)
    App->>DB: COMMIT Transaction
    deactivate App

    loop Background Outbox Poller
        Worker->>DB: SELECT FOR UPDATE SKIP LOCKED
        Worker->>EventBus: Dispatch Event
        Worker->>DB: UPDATE outbox_events SET status = 'PROCESSED'
    end
```

---

## Key Features

1. **At-Least-Once Delivery Guarantee:**
   - Events are persisted to `outbox_events` within the same database transaction as the domain entity update, ensuring zero lost events during container failures.

2. **Non-Blocking Multi-Node Polling (ADR-026):**
   - Polling workers query pending outbox records using `FOR UPDATE SKIP LOCKED`.
   - Prevents duplicate event dispatches and SQL deadlocks in multi-pod application clusters.

3. **Dual-Mode Event Routing:**
   - **Single Container Mode:** Local delivery via Vert.x EventBus.
   - **Clustered Enterprise Mode:** Distributed streaming via Apache Kafka topics.
