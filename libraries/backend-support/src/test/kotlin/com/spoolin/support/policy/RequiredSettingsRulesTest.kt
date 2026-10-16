package com.spoolin.support.policy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

class RequiredSettingsRulesTest {
    private val names =
        listOf(
            "CORE_DB_URL",
            "CORE_DB_USER",
            "CORE_DB_PASSWORD_FILE",
            "CATALOG_DB_URL",
            "CATALOG_DB_USER",
            "CATALOG_DB_PASSWORD_FILE",
            "MEDIA_DB_URL",
            "MEDIA_DB_USER",
            "MEDIA_DB_PASSWORD_FILE",
            "BROWSER_ORIGIN",
            "SERVICE_CERT_FILE",
            "SERVICE_KEY_FILE",
            "SERVICE_CA_FILE",
            "PII_KEY_FILE",
            "DRAFT_KEY_FILE",
            "EMAIL_LOOKUP_KEY_FILE",
            "OPERATION_HMAC_KEY_FILE",
            "CURSOR_KEY_FILE",
            "CSRF_KEY_FILE",
            "PII_KEY_ID",
            "DRAFT_KEY_ID",
            "EMAIL_LOOKUP_KEY_ID",
            "OPERATION_HMAC_KEY_ID",
            "ACTOR_SIGNING_PRIVATE_KEY_FILE",
            "ACTOR_SIGNING_KEY_ID",
            "ACTOR_VERIFY_KEYS_FILE",
            "VIEWER_IDENTITY_FILE",
            "VIEWER_PASSWORD_FILE",
            "MAINTAINER_IDENTITY_FILE",
            "MAINTAINER_PASSWORD_FILE",
            "manifest.manifestVersion",
            "manifest.snapshotSHA256",
            "RATE_LIMIT_KEY_FILE",
        )

    @TestFactory
    fun requiredPrecedenceRows() =
        names.flatMap { name ->
            listOf("environment", "file", "missing", "empty", "space", "NEL", "newline", "NUL").map { recipe ->
                dynamicTest("F-CONFIG $name required presence $recipe") {
                    val valid = "synthetic-value"
                    val explicit =
                        mapOf(
                            "empty" to "",
                            "space" to " ",
                            "NEL" to "\u0085",
                            "newline" to "bad\nline",
                            "NUL" to "bad\u0000value",
                        )
                    val source =
                        when (recipe) {
                            "environment" ->
                                ConfigurationSources(
                                    mapOf(name to valid),
                                    mapOf(name to "different-file-value"),
                                )
                            "file" -> ConfigurationSources(file = mapOf(name to valid))
                            "missing" -> ConfigurationSources()
                            else -> ConfigurationSources(mapOf(name to explicit.getValue(recipe)), mapOf(name to valid))
                        }
                    if (recipe in setOf("environment", "file")) {
                        assertEquals(valid, source.required(name).value)
                        val origin = if (recipe == "environment") ValueSource.ENVIRONMENT else ValueSource.FILE
                        assertEquals(origin, source.required(name).source)
                    } else {
                        val error = assertThrows(ConfigurationViolation::class.java) { source.required(name) }
                        assertEquals(name, error.setting)
                        assertFalse(error.message.orEmpty().contains("bad"))
                    }
                }
            }
        }
}
