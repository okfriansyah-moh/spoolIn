package com.spoolin.core.recovery.crypto

import com.spoolin.core.features.crypto.AccountCryptography
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import org.junit.jupiter.api.Test
import java.nio.file.Path

class AccountArchitectureTest {
    private val classes =
        ClassFileImporter().importPath(
            Path.of(
                AccountCryptography::class.java.protectionDomain.codeSource.location
                    .toURI(),
            ),
        )

    @Test
    fun reusableCryptoDoesNotDependOnAccountPersistence() {
        noClasses()
            .that()
            .resideInAPackage("com.spoolin.core.features.crypto..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("com.spoolin.core.features.account..")
            .check(classes)
    }

    @Test
    fun accountAndCryptoFeaturesHaveNoDependencyCycle() {
        slices()
            .matching("com.spoolin.core.features.(*)..")
            .should()
            .beFreeOfCycles()
            .check(classes)
    }
}
