# OmniDepot Automated Testing & Quality Concept

This document defines the binding guidelines for the structure, naming, tools, architectural enforcement, and quality assurance of all automated tests in **OmniDepot**.

---

## 🎯 Goals & Core Principles

- **Target Coverage:** At least **80% branch coverage** across all modules (measured via JaCoCo & SonarQube quality gates).
- **Test Pyramid:** Heavy emphasis on ultra-fast unit tests, followed by component tests (`*CT`), and targeted integration tests (`*IT`).
- **Readability Above All:** Explicit Given/When/Then structure with `@DisplayName("When ... then ...")` method descriptions.
- **Fast Feedback:** Unit tests must **never** load a Quarkus context and must execute in $\le 100\text{ ms}$.
- **Exclusive Assertion Framework:** **AssertJ** is the single allowed assertion library. `org.junit.jupiter.api.Assertions` is strictly forbidden.

---

## 🔬 Scope & Tech Stack

| Domain | Technology / Tool |
| :--- | :--- |
| **Java Version** | Java 25 LTS (Virtual Threads, Records, Sealed Classes) |
| **Framework** | Quarkus 3.37+ & Eclipse Vert.x Reactive Engine |
| **Test Runner** | JUnit 5 (Jupiter) |
| **Assertions** | AssertJ 3.26+ (`org.assertj.core.api.Assertions`) |
| **Mocking** | Mockito 5+ (for external boundaries only) |
| **Architecture Guard** | ArchUnit 1.4.2 |
| **Coverage** | JaCoCo 0.8.15 (Per-module + Aggregated `repo-coverage-report`) |
| **Integration Containers** | Testcontainers (PostgreSQL 16, RustFS S3, Apache Kafka) |

---

## 📐 Test Types & Boundaries

```
                 / \
                / IT\       <-- Integration Tests (*IT) [@QuarkusTest + Testcontainers]
               /-----\
              /  CT   \     <-- Component Tests (*CT) [Multi-class, No Quarkus Context]
             /---------\
            /   Unit    \   <-- Unit Tests (*Test) [Pure Java / Mockito, < 100ms]
           /-------------\
```

### 1. Unit Tests (`*Test.java`)
- **Definition:** Testing individual classes/value objects in pure Java isolation.
- **Rules:** **Zero** Quarkus annotations (`@QuarkusTest`, `@Inject`, `@Mock`), zero DB, network, or filesystem I/O.
- **Goal:** Core domain logic, SHA-256 digest validation, record immutability, error handling.
- **Target Time:** $< 100\text{ ms}$ per test.

### 2. Component Tests (`*CT.java`)
- **Definition:** Testing small clusters of collaborating domain classes without starting a Quarkus container context.
- **Rules:** Collaborators wired manually or via ObjectMothers. Mocks used only for external boundaries (S3, DB, HTTP).
- **Goal:** Domain orchestration, SPI interactions, state machines.
- **Target Time:** $< 200\text{ ms}$ per test.

### 3. Integration Tests (`*IT.java`)
- **Definition:** Testing full application slices with Quarkus container context (`@QuarkusTest`) and real external infrastructure via Testcontainers.
- **Goal:** CDI wiring, Liquibase database migrations, REST endpoints, REST-Assured HTTP serialization, transactional outbox dispatching.
- **Execution:** Executed by `maven-failsafe-plugin` during the `integration-test` / `verify` phases.

---

## Naming & Structure Conventions

- **Directory Structure:** Mirrors production package structure 1-to-1.
- **Class Suffixes:**
  - Unit Tests: `*Test.java` (e.g. `Sha256DigestTest.java`)
  - Component Tests: `*CT.java` (e.g. `CatalogIngestionCT.java`)
  - Integration Tests: `*IT.java` (e.g. `OciDistributionResourceIT.java`)
- **Method Display Names:** Pattern `"When [condition] then [expected result]"`:

```java
@Test
@DisplayName("When discount is valid and cart is not empty, then final price is reduced")
void shouldApplyDiscount() {
    // Given
    var calc = new PriceCalculator();
    var cart = BlobObjectMother.sampleCart();

    // When
    var result = calc.calculateFinalPrice(cart);

    // Then
    assertThat(result).isEqualTo(80.0);
}
```

---

## ArchUnit Architectural Protection Rules

In omnidepot, architecture protection rules are defined in `repo-app` inside [ArchitectureBoundaryTest.java](file:///home/developer/projects/OmniDepot/repo-app/src/test/java/io/omnidepot/app/ArchitectureBoundaryTest.java):

```java
class ArchitectureBoundaryTest {

    @Test
    @DisplayName("Format modules MUST NOT depend on repo-storage-* or repo-infra-db-*")
    void formatModulesMustNotAccessStorageOrInfraDbDirectly() {
        ArchRule rule = classes()
                .that().resideInAPackage("..format..")
                .should().onlyDependOnClassesThat(
                        resideInAnyPackage("..format..", "..core.api..", "java..", "jakarta..", "io.quarkus..", "org.jspecify..")
                );
        rule.check(allClasses);
    }

    @Test
    @DisplayName("Test classes MUST use AssertJ assertions instead of JUnit Assertions")
    void testsMustUseAssertJAssertionsInsteadOfJUnitAssertions() {
        ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("Test")
                .or().haveSimpleNameEndingWith("Support")
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("org.junit.jupiter.api.Assertions");
        rule.check(allClasses);
    }

    @Test
    @DisplayName("Packages in production code MUST be marked with JSpecify @NullMarked")
    void packagesMustBeAnnotatedWithNullMarked() {
        ArchRule rule = classes()
                .that().resideInAPackage("io.omnidepot..")
                .and().haveSimpleName("package-info")
                .should().beAnnotatedWith(NullMarked.class);
        rule.check(allClasses);
    }
}
```

---

## Maven Build & Execution Configuration

```xml
<build>
    <plugins>
        <!-- Surefire runs Unit (*Test) and Component (*CT) tests -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.2.5</version>
            <configuration>
                <includes>
                    <include>**/*Test.java</include>
                    <include>**/*CT.java</include>
                </includes>
            </configuration>
        </plugin>

        <!-- Failsafe runs Integration (*IT) tests -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-failsafe-plugin</artifactId>
            <version>3.2.5</version>
            <configuration>
                <includes>
                    <include>**/*IT.java</include>
                </includes>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>integration-test</goal>
                        <goal>verify</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>

        <!-- JaCoCo generates per-module and aggregated coverage reports -->
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.15</version>
        </plugin>
    </plugins>
</build>
```

---

## Mandatory PR Checklist

- [x] All unit, component, and integration tests pass locally (`./mvnw clean verify`).
- [x] Every `@Test` method uses `@DisplayName("When ... then ...")`.
- [x] **Zero** imports of `org.junit.jupiter.api.Assertions` (AssertJ only).
- [x] No `@QuarkusTest` annotations on Unit or Component tests.
- [x] Correct suffixes applied (`*Test`, `*CT`, `*IT`).
- [x] Production packages include `package-info.java` with JSpecify `@NullMarked`.
- [x] JaCoCo branch coverage target ($\ge 80\%$) met across module reactor.
