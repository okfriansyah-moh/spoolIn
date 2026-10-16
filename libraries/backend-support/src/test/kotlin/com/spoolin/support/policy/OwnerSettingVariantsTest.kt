package com.spoolin.support.policy

import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class OwnerSettingVariantsTest : FullConfigurationFixture() {
    private fun values(owner: String) =
        references +
            mapOf(
                "${owner.uppercase()}_DB_URL" to "jdbc:postgresql://db:5432/spoolin_$owner?sslmode=verify-full",
                "${owner.uppercase()}_DB_USER" to "spoolin_${owner}_runtime",
                "${owner.uppercase()}_DB_PASSWORD_FILE" to "/private/synthetic-file",
            )

    @TestFactory
    fun nonemptyWrongOwnerRoles() =
        listOf("core", "catalog", "media").map { owner ->
            val name = "${owner.uppercase()}_DB_USER"
            dynamicTest("F-CONFIG $name nonempty wrong-owner principal refused") {
                val wrong = if (owner == "core") "catalog" else "core"
                VariantAssertions.refuses(name) {
                    OwnerConfiguration.validate(
                        owner,
                        ConfigurationSources(values(owner) + (name to "spoolin_${wrong}_runtime")),
                    )
                }
            }
        }

    @TestFactory
    fun namedMigratorRoles() =
        listOf("core", "catalog", "media").map { owner ->
            val name = "${owner.uppercase()}_DB_USER"
            dynamicTest("F-CONFIG $name same-owner migrator accepted") {
                OwnerConfiguration.validate(
                    owner,
                    ConfigurationSources(values(owner) + (name to "spoolin_${owner}_migrator")),
                )
            }
        }

    @Test
    fun `F-CONFIG BROWSER_ORIGIN nonempty mismatched origin refused`() {
        VariantAssertions.refuses("BROWSER_ORIGIN") {
            OwnerConfiguration.validate(
                "core",
                ConfigurationSources(values("core") + ("BROWSER_ORIGIN" to "https://other.example.test")),
            )
        }
    }

    @TestFactory
    fun namedMalformedIdentifiers() =
        listOf(
            "PII_KEY_ID",
            "DRAFT_KEY_ID",
            "EMAIL_LOOKUP_KEY_ID",
            "OPERATION_HMAC_KEY_ID",
            "ACTOR_SIGNING_KEY_ID",
        ).flatMap { name ->
            listOf("synthetic.bad", "synthetic/slash", "synthetic space", "x".repeat(65)).mapIndexed { index, value ->
                dynamicTest("F-CONFIG $name malformed identifier variant-$index") {
                    VariantAssertions.refuses(name) {
                        OwnerConfiguration.validate("core", ConfigurationSources(values("core") + (name to value)))
                    }
                }
            }
        }
}
