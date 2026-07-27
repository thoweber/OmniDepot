package io.omnidepot.app;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Hybrid Boundary & Code Quality Enforcement Test (ADR-009).
 * Verifies strict domain boundary rules and module isolation across the modular monolith,
 * as well as testing conventions such as enforcing AssertJ assertions over JUnit,
 * and JSpecify nullability annotations across packages.
 */
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
                        resideInAnyPackage(
                                "..format..",
                                "..core.api..",
                                "java..",
                                "javax..",
                                "jakarta..",
                                "org.jboss..",
                                "io.quarkus..",
                                "io.smallrye..",
                                "org.eclipse.microprofile..",
                                "org.jspecify..",
                                "org.projectlombok..",
                                "com.fasterxml.."
                        )
                )
                .allowEmptyShould(true);

        rule.check(allClasses);
    }

    @Test
    @DisplayName("Given test classes across all modules - when auditing assertions - then they must use AssertJ instead of JUnit Assertions")
    void testsMustUseAssertJAssertionsInsteadOfJUnitAssertions() {
        ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("Test")
                .or().haveSimpleNameEndingWith("Support")
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("org.junit.jupiter.api.Assertions")
                .allowEmptyShould(true);

        rule.check(allClasses);
    }

    @Test
    @DisplayName("Given production code packages - when auditing nullability annotations - then package-info must be annotated with @NullMarked")
    void packagesMustBeAnnotatedWithNullMarked() {
        ArchRule rule = classes()
                .that().resideInAPackage("io.omnidepot..")
                .and().haveSimpleName("package-info")
                .should().beAnnotatedWith(NullMarked.class)
                .allowEmptyShould(true);

        rule.check(allClasses);
    }
}
