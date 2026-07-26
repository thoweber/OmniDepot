---
name: test-engineer
description: Test engineering standards, 3-tier test pyramid (*Test/*CT/*IT), AssertJ assertion rules, ArchUnit boundary protection, JaCoCo branch coverage targets, and Testcontainers protocols for OmniDepot.
version: 1.0.0
tech_stack:
  jdk: Java 25 LTS
  framework: JUnit 5 (Jupiter)
  assertions: AssertJ 3.27+ (Exclusive assertion library)
  architecture_testing: ArchUnit 1.4.2
  coverage: JaCoCo 0.8.15 (Aggregated report in repo-coverage-report)
  containers: Testcontainers (PostgreSQL 16, RustFS S3, Kafka)
---

# Test Engineering Skill: OmniDepot

This document defines the binding guidelines for structure, naming, tools, assertion rules, and quality gates for automated testing across the OmniDepot reactor codebase.

---

## 1. Test Pyramid & Suffix Conventions

```text
                 / \
                / IT\       <-- Integration Tests (*IT) [@QuarkusTest + Testcontainers]
               /-----\
              /  CT   \     <-- Component Tests (*CT) [Multi-class, No Quarkus Context]
             /---------\
            /   Unit    \   <-- Unit Tests (*Test) [Pure Java / Mockito, < 100ms]
           /-------------\
```

### A. Suffix & Execution Mapping

| Suffix | Class Type | Context | Runner Plugin | Target Execution Time |
| :--- | :--- | :--- | :--- | :--- |
| **`*Test.java`** | Unit Test | Zero Quarkus Context (Pure Java) | `maven-surefire-plugin` | $< 100\text{ ms}$ |
| **`*CT.java`** | Component Test | Multi-class cluster (No Quarkus) | `maven-surefire-plugin` | $< 200\text{ ms}$ |
| **`*IT.java`** | Integration Test | `@QuarkusTest` + Testcontainers | `maven-failsafe-plugin` | Integration / Verify phase |

---

## 2. Assertion & Display Name Rules

### A. AssertJ Exclusive Assertion Framework
- **AssertJ ONLY:** Use `assertThat()`, `assertThatThrownBy()`, and `assertThatCode()`.
- **FORBIDDEN:** `org.junit.jupiter.api.Assertions` imports are strictly forbidden and enforced by ArchUnit rules.

### B. Display Name Standard Pattern
Every `@Test` method MUST be annotated with a display name following the **Given - When - Then** structure:  
`@DisplayName("Given [precondition] - when [action/trigger] - then [expected outcome]")`

```java
@Test
@DisplayName("Given a valid sha256 digest - when resolving CAS path - then correct tiered storage location is returned")
void shouldComputeCasStoragePath() {
    // Given
    var digest = Sha256Digest.of("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");

    // When
    var path = CasPathResolver.resolve(digest);

    // Then
    assertThat(path).isEqualTo("blobs/sha256/e3/b0/c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
}
```

### C. Parameterized Testing Pattern
```java
@ParameterizedTest(name = "Case {index}: input digest {0}")
@ValueSource(strings = { "invalid-hash", "", "   ", "sha256:too-short" })
@DisplayName("Given an invalid input digest format - when creating Sha256Digest - then IllegalArgumentException is thrown")
void shouldThrowExceptionForInvalidDigests(String invalidDigest) {
    assertThatThrownBy(() -> Sha256Digest.of(invalidDigest))
            .isInstanceOf(IllegalArgumentException.class);
}
```

---

## 3. ArchUnit Architectural Boundary Tests

All architecture protection tests are located in `repo-app` inside [ArchitectureBoundaryTest.java](file:///home/developer/projects/OmniDepot/repo-app/src/test/java/io/omnidepot/app/ArchitectureBoundaryTest.java):

```java
class ArchitectureBoundaryTest {

    private static JavaClasses allClasses;

    @BeforeAll
    static void setup() {
        allClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
                .importPackages("io.omnidepot");
    }

    @Test
    @DisplayName("Given reactor format modules - when auditing package boundaries - then they must not access storage or database directly")
    void formatModulesMustNotAccessStorageOrInfraDbDirectly() {
        ArchRule rule = classes()
                .that().resideInAPackage("..format..")
                .should().onlyDependOnClassesThat(
                        resideInAnyPackage("..format..", "..core.api..", "java..", "jakarta..", "io.quarkus..", "org.jspecify..", "org.projectlombok..")
                );
        rule.check(allClasses);
    }

    @Test
    @DisplayName("Given test classes across all modules - when auditing assertions - then they must use AssertJ instead of JUnit Assertions")
    void testsMustUseAssertJAssertionsInsteadOfJUnitAssertions() {
        ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("Test")
                .or().haveSimpleNameEndingWith("Support")
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("org.junit.jupiter.api.Assertions");
        rule.check(allClasses);
    }

    @Test
    @DisplayName("Given production code packages - when auditing nullability annotations - then package-info must be annotated with @NullMarked")
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

## 4. Code Coverage Quality Gate

- **Target Threshold:** Branch coverage $\ge 80\%$ per module (JaCoCo 0.8.15).
- **Aggregated Report:** Multi-module report generated at `repo-coverage-report/target/site/jacoco-aggregate/index.html`.
- **Verification Command:** `./mvnw clean verify`.
