# OmniDepot Coding Standards & Patterns

## 1. Java 25 & Quarkus Conventions
* Use modern Java 25 constructs: `record` for immutable DTOs/Value Objects, pattern matching in `switch` expressions, and `sealed` interfaces for domain events.
* Use `@ApplicationScoped` for CDI singletons and `@RequestScoped` strictly when request context state is mandatory.
* Annotate Panache entities with Hibernate 6 `@JdbcTypeCode(SqlTypes.JSON)` for complex JSONB fields (`attributes`, `provider_state`).

## 2. Vert.x Reactive & Zero-GC Rules (ADR-002, ADR-006)
* Handle binary streams reactively using Mutiny `Multi<Buffer>` or Vert.x `ReadStream<Buffer>`.
* Pass Netty direct byte buffers directly from network sockets to storage channels without copying bytes onto the Java heap.
* Never call blocking code on Vert.x event loops. Offload blocking execution using `Uni.createFrom().item(...).runSubscriptionOn(Infrastructure.getDefaultWorkerPool())`.

## 3. Angular 17+ Signals & Material Design 3 (ADR-011-014)
* Use fine-grained Angular `signal()`, `computed()`, and `effect()` primitives for UI state. Avoid legacy RxJS `BehaviorSubject` where signals suffice.
* Presentational components must receive input via `input()` signals and emit changes via `output()` events, maintaining zero HTTP dependencies.
* Style UI components using Material Design 3 design tokens meeting WCAG AA accessibility contrast standards.

## 4. Liquibase Database Migration Rules (ADR-023)
* Standard ANSI SQL changes must use built-in Liquibase tags (`<createTable>`, `<addColumn>`).
* Dialect-specific features (`JSONB`, `USING GIN`, `FOR UPDATE SKIP LOCKED`) must be qualified with `dbms="postgresql"` or `dbms="h2"`.
* Every `<changeSet>` MUST contain an explicit `<rollback>` definition.
