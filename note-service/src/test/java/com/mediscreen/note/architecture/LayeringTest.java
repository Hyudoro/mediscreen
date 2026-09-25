package com.mediscreen.note.architecture;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.mediscreen.note.domain.Note;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

@AnalyzeClasses(packages = "com.mediscreen.note", importOptions = ImportOption.DoNotIncludeTests.class)
class LayeringTest {

    @ArchTest
    static final ArchRule layersAreRespected = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Controller")
            .definedBy("com.mediscreen.note.controller..")
            .layer("Service")
            .definedBy("com.mediscreen.note.service..")
            .layer("Repository")
            .definedBy("com.mediscreen.note.repository..")
            .whereLayer("Controller")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("Service")
            .mayOnlyBeAccessedByLayers("Controller")
            .whereLayer("Repository")
            .mayOnlyBeAccessedByLayers("Service");

    @ArchTest
    static final ArchRule controllersNeverTouchRepositories = ArchRuleDefinition.noClasses()
            .that()
            .resideInAPackage("..controller..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..repository..")
            .because("a controller goes through the service layer");

    @ArchTest
    static final ArchRule documentsNeverLeaveTheServiceLayer = ArchRuleDefinition.noClasses()
            .that()
            .resideInAPackage("..controller..")
            .should()
            .dependOnClassesThat()
            .areAssignableTo(Note.class)
            .because("controllers see DTOs only");

    @ArchTest
    static final ArchRule noCyclesBetweenPackages =
            slices().matching("com.mediscreen.note.(*)..").should().beFreeOfCycles();
}
