package com.spoolin.support.policy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

class OwnerDatabaseRulesTest : FullConfigurationFixture() {
    private fun values(
        owner: String,
        query: String,
    ): Map<String, String> {
        val prefix = owner.uppercase()
        return references +
            mapOf(
                "${prefix}_DB_URL" to "jdbc:postgresql://db:5432/spoolin_$owner?$query",
                "${prefix}_DB_USER" to "spoolin_${owner}_runtime",
                "${prefix}_DB_PASSWORD_FILE" to "/private/synthetic-file",
            )
    }

    @TestFactory
    fun ownerDatabaseOptions() =
        listOf("core", "catalog", "media").flatMap { owner ->
            listOf(
                "password=SYNTHETIC_PRIVATE_VALUE",
                "user=other",
                "sslfactory=org.postgresql.ssl.NonValidatingFactory",
                "ssl=false",
                "sslmode=disable",
                "sslrootcert",
                "connectTimeout=2&unknown=value",
            ).map { option ->
                dynamicTest("F-CONFIG $owner DB URL rejects unsupported option ${option.substringBefore('=')}") {
                    val input = values(owner, "sslmode=verify-full&$option")
                    val error =
                        assertThrows(ConfigurationViolation::class.java) {
                            OwnerConfiguration.validate(owner, ConfigurationSources(input))
                        }
                    assertEquals("${owner.uppercase()}_DB_URL", error.setting)
                    assertFalse(error.message.orEmpty().contains("SYNTHETIC_PRIVATE_VALUE"))
                }
            }
        }

    @TestFactory
    fun allowedOwnedDatabaseOptions() =
        listOf("core", "catalog", "media").map { owner ->
            dynamicTest("F-CONFIG $owner DB URL permits accepted TLS root and connection timeout options") {
                val input = values(owner, "sslmode=verify-full&sslrootcert=/private/root.crt&connectTimeout=2")
                OwnerConfiguration.validate(owner, ConfigurationSources(input))
            }
        }
}
