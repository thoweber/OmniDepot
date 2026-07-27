# OmniDepot Coding Standards & Patterns

## 1. Java 25 & Quarkus Conventions
* Use modern Java 25 constructs: `record` for immutable DTOs/Value Objects, pattern matching in `switch` expressions, and `sealed` interfaces for domain events.
* Use `@ApplicationScoped` for CDI singletons and `@RequestScoped` strictly when request context state is mandatory.
* Annotate Panache entities with Hibernate 6 `@JdbcTypeCode(SqlTypes.JSON)` for complex JSONB fields (`attributes`, `provider_state`).

## 2. Strongly-Typed Value Objects & Nullability Rules
* Encapsulate primitives (`String`, `Long`) into strongly-typed `record` Value Objects implementing core tagging interfaces (`RepositoryPath`, `ArtifactCoordinate`) to prevent primitive obsession.
* **Manual Null Checks:** Use `isNull(val)` or `nonNull(val)` with static imports (`import static java.util.Objects.isNull; import static java.util.Objects.nonNull;`) for manual null checks (never raw `== null` or `!= null`).
* **Optional Return Types:** Use `Optional<T>` for any query or getter method return type that may not produce a value.
* **Empty Collections:** Never return `null` for collection return types — always return an immutable empty collection (`List.of()`, `Set.of()`, `Map.of()`).
* **Functional Optional Chains over Ternaries:** Avoid ternary operators (`a ? b : c`) whenever possible. Prefer functional `Optional` chains (`Optional.ofNullable(val).map(...).orElse(...)`) for parameter normalization and default fallbacks.

## 3. Strongly-Typed Domain Exceptions (ADR-005, ADR-008)
* **No Raw Exception / RuntimeException:** Production code MUST NEVER throw raw `RuntimeException` or `Exception`. Always throw domain-appropriate, strongly-typed exception classes (`StorageException`, `BlobWriteException`, `OciProtocolException`).
* Catch low-level technical exceptions (`IOException`, `SQLException`, `S3Exception`) at system boundaries and wrap them inside domain-appropriate exception hierarchies with rich diagnostic context.

## 4. Hot-Path Performance & Zero-GC Rules (ADR-002, ADR-006, ADR-019)
* Handle binary streams reactively using Mutiny `Multi<Buffer>` or Vert.x `ReadStream<Buffer>`.
* Pass Netty direct byte buffers directly from network sockets to storage channels without copying bytes onto the Java heap.
* Never call blocking code on Vert.x event loops. Offload blocking execution using `Uni.createFrom().item(...).runSubscriptionOn(Infrastructure.getDefaultWorkerPool())`.

## 5. Test Engineering & 1-to-1 Mapping Rules (`test-engineer`)
* **Dedicated 1-to-1 Unit Test Files:** Every production class MUST have its own dedicated 1-to-1 unit test class (`<TargetClassName>Test.java`). Never group unit tests for different production classes into a single multi-target test file.
* **Given-When-Then Display Names:** All `@Test` methods MUST use `@DisplayName("Given [precondition] - when [action/trigger] - then [expected outcome]")`.
* **AssertJ Assertions:** Use AssertJ (`assertThat()`, `assertThatThrownBy()`) exclusively. `org.junit.jupiter.api.Assertions` is strictly forbidden.

## 6. Jakarta Validation & JSpecify Boundary Rules (`security-analyst`)
* Use JSpecify (`@NullMarked`, `@Nullable`) for package nullability annotations.
* Enforce boundary checks using **Jakarta Validation** (`jakarta.validation.constraints.*`: `@NotNull`, `@NotBlank`, `@Size`, `@Pattern`, `@Valid`) on REST controllers, Kafka consumers, and DB repositories.
* Normalize data first (lowercase, canonicalize paths, strip prefixes) before validation. Discard unnormalized inputs in favor of normalized representations.
* Internal domain methods within `@NullMarked` packages assume non-null parameters and MUST NOT pollute domain logic with redundant defensive null checks.

## 7. Angular 17+ Signals & Material Design 3 (ADR-011-014)
* Use fine-grained Angular `signal()`, `computed()`, and `effect()` primitives for UI state. Avoid legacy RxJS `BehaviorSubject` where signals suffice.
* Presentational components must receive input via `input()` signals and emit changes via `output()` events, maintaining zero HTTP dependencies.
* Style UI components using Material Design 3 design tokens meeting WCAG AA accessibility contrast standards.

## 8. Liquibase Database Migration Rules (ADR-023)
* Standard ANSI SQL changes must use built-in Liquibase tags (`<createTable>`, `<addColumn>`).
* Dialect-specific features (`JSONB`, `USING GIN`, `FOR UPDATE SKIP LOCKED`) must be qualified with `dbms="postgresql"` or `dbms="h2"`.
* Every `<changeSet>` MUST contain an explicit `<rollback>` definition.
