package com.spoolin.foundation;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.spoolin.foundation.fixture")
class FixtureArchitectureTest {
    @ArchTest
    static final ArchRule fixtureHasNoServiceInternals = noClasses().should().dependOnClassesThat()
            .resideInAnyPackage("com.spoolin.core..", "com.spoolin.catalog..", "com.spoolin.media..");
}
