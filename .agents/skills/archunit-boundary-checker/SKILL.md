---
name: archunit-boundary-checker
description: Audits and enforces Java package visibility, CDI scope rules, and Hexagonal layer isolation using ArchUnit.
---

# ArchUnit Boundary Checker Skill (`/check-boundaries`)

When checking or generating boundary verification tests (ADR-005, ADR-009):

## 1. Rules to Enforce (Java 25 LTS & Quarkus 3.37.4)
* **Rule A (Domain Isolation):** Classes in `io.omnidepot.format.**` must NOT access `io.omnidepot.storage.impl.**` or `io.omnidepot.infrastructure.**`.
* **Rule B (Package Privacy):** Classes implementing SPI interfaces in infrastructure or format modules MUST be package-private.
* **Rule C (Core Independence):** Classes in `repo-core-domain` must NOT import Vert.x HTTP, Quarkus REST, or Jackson annotations.
* **Rule D (CDI Scope):** Storage and identity providers MUST be annotated with `@ApplicationScoped` and `@LookupIfProperty`.
* **Rule E (AssertJ Enforcement):** Test classes MUST NOT import or depend on `org.junit.jupiter.api.Assertions` (AssertJ `assertThat` is mandatory).
* **Rule F (Nullability Guard):** All production packages in `io.omnidepot..` MUST contain a `package-info.java` annotated with JSpecify `@NullMarked`.

## 2. Standard ArchUnit Verification Class
```java
package io.omnidepot.app;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureBoundaryTest {

    private final JavaClasses allClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
            .importPackages("io.omnidepot");

    @Test
    @DisplayName("When format modules are checked, then they must not access storage or DB directly")
    void formatModulesMustNotAccessStorageOrInfraDbDirectly() {
        ArchRule rule = classes()
                .that().resideInAPackage("..format..")
                .should().onlyDependOnClassesThat(
                        resideInAnyPackage("..format..", "..core.api..", "java..", "jakarta..", "io.quarkus..", "org.jspecify..", "org.projectlombok..")
                );
        rule.check(allClasses);
    }

    @Test
    @DisplayName("When test classes are checked, then they must use AssertJ instead of JUnit Assertions")
    void testsMustUseAssertJAssertionsInsteadOfJUnitAssertions() {
        ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("Test")
                .or().haveSimpleNameEndingWith("Support")
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("org.junit.jupiter.api.Assertions");
        rule.check(allClasses);
    }

    @Test
    @DisplayName("When production packages are checked, then package-info must be annotated with @NullMarked")
    void packagesMustBeAnnotatedWithNullMarked() {
        ArchRule rule = classes()
                .that().resideInAPackage("io.omnidepot..")
                .and().haveSimpleName("package-info")
                .should().beAnnotatedWith(NullMarked.class);
        rule.check(allClasses);
    }
}
```
