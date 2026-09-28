package com.mediscreen.assessment.architecture;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

@AnalyzeClasses(packages = "com.mediscreen.assessment", importOptions = ImportOption.DoNotIncludeTests.class)
class LayeringTest {

    @ArchTest
    static final ArchRule layersAreRespected = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Controller")
            .definedBy("com.mediscreen.assessment.controller..")
            .layer("Service")
            .definedBy("com.mediscreen.assessment.service..")
            .layer("Client")
            .definedBy("com.mediscreen.assessment.client..")
            .whereLayer("Controller")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("Service")
            .mayOnlyBeAccessedByLayers("Controller")
            .whereLayer("Client")
            .mayOnlyBeAccessedByLayers("Service");

    @ArchTest
    static final ArchRule controllersNeverCallTheClients = ArchRuleDefinition.noClasses()
            .that()
            .resideInAPackage("..controller..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..client..")
            .because("a controller goes through the service layer");

    @ArchTest
    static final ArchRule theRulesArePlainJava = ArchRuleDefinition.classes()
            .that()
            .resideInAPackage("com.mediscreen.assessment.domain..")
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage("com.mediscreen.assessment.domain..", "java..")
            .because("the risk rules depend on nothing but the JDK");

    @ArchTest
    static final ArchRule noCyclesBetweenPackages =
            slices().matching("com.mediscreen.assessment.(*)..").should().beFreeOfCycles();
}
