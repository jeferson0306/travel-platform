package com.travelplatform.flight.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Enforces the layering described in ARCHITECTURE.md: dependencies only ever point inward, and the
 * domain never depends on a framework type.
 */
@DisplayName("Hexagonal architecture rules")
class HexagonalArchitectureTest {

    private static final JavaClasses CLASSES =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages("com.travelplatform.flight");

    @Test
    void layersOnlyDependInward() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Domain")
                .definedBy("com.travelplatform.flight.domain..")
                .layer("Application")
                .definedBy("com.travelplatform.flight.application..")
                .layer("Infrastructure")
                .definedBy("com.travelplatform.flight.infrastructure..")
                .layer("Api")
                .definedBy("com.travelplatform.flight.api..")
                .whereLayer("Domain")
                .mayOnlyBeAccessedByLayers("Application", "Infrastructure", "Api")
                .whereLayer("Application")
                .mayOnlyBeAccessedByLayers("Infrastructure", "Api")
                .whereLayer("Infrastructure")
                .mayOnlyBeAccessedByLayers("Api")
                .check(CLASSES);
    }

    @Test
    void domainNeverDependsOnFrameworkTypes() {
        noClasses()
                .that()
                .resideInAPackage("com.travelplatform.flight.domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "io.quarkus..", "jakarta..", "org.bson..", "com.mongodb..", "io.smallrye..")
                .check(CLASSES);
    }
}
