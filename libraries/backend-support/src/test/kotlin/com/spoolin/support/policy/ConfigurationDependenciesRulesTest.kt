package com.spoolin.support.policy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class ConfigurationDependenciesRulesTest {
    private val smtp =
        mapOf(
            "NOTIFICATION_MODE" to "smtp",
            "SMTP_HOST" to "smtp.example.test",
            "SMTP_FROM" to "sender@example.test",
            "SMTP_TO" to "recipient@example.test",
        )

    @Test
    fun `ATDD-290 active adapters accept exactly complete selected credential groups`() {
        assertEquals(
            setOf("SMTP_HOST", "SMTP_FROM", "SMTP_TO"),
            NotificationSettings.resolve(ConfigurationSources(smtp)).keys,
        )
        val slack =
            mapOf(
                "NOTIFICATION_MODE" to "slack",
                "SLACK_BOT_TOKEN_FILE" to "/private/test-token",
                "SLACK_CHANNEL" to "selected",
            )
        assertEquals(
            setOf("SLACK_BOT_TOKEN_FILE", "SLACK_CHANNEL"),
            NotificationSettings.resolve(ConfigurationSources(slack)).keys,
        )
        assertThrows(ConfigurationViolation::class.java) {
            NotificationSettings.resolve(
                ConfigurationSources(slack + ("SLACK_WEBHOOK_FILE" to "/private/test-webhook")),
            )
        }
        val chat =
            mapOf(
                "NOTIFICATION_MODE" to "google_chat",
                "GOOGLE_CHAT_CREDENTIAL_FILE" to "/private/test-credential",
                "GOOGLE_CHAT_SPACE" to "selected",
            )
        assertEquals(
            setOf("GOOGLE_CHAT_CREDENTIAL_FILE", "GOOGLE_CHAT_SPACE"),
            NotificationSettings.resolve(ConfigurationSources(chat)).keys,
        )
        assertThrows(ConfigurationViolation::class.java) {
            NotificationSettings.resolve(
                ConfigurationSources(chat - "GOOGLE_CHAT_SPACE"),
            )
        }
        assertThrows(ConfigurationViolation::class.java) {
            NotificationSettings.resolve(
                ConfigurationSources(smtp + ("SMTP_PASSWORD_FILE" to "/private/test-password")),
            )
        }
    }

    @TestFactory
    fun smtpPortBounds() =
        listOf(0, 1, 2, 65534, 65535, 65536).map { port ->
            dynamicTest("F-CONFIG SMTP_PORT boundary $port") {
                val source = ConfigurationSources(smtp + ("SMTP_PORT" to port.toString()))
                if (port in 1..65535) {
                    assertTrue(NotificationSettings.resolve(source).isNotEmpty())
                } else {
                    assertThrows(ConfigurationViolation::class.java) { NotificationSettings.resolve(source) }
                }
            }
        }

    @TestFactory
    fun protectedFileDescriptors() =
        listOf(
            Triple("0600", false, 32),
            Triple("0644", false, 32),
            Triple("0600", true, 32),
            Triple("0600", false, 31),
            Triple("0600", false, 33),
        ).map { (mode, symlink, bytes) ->
            dynamicTest("ATDD-290 protected descriptor mode=$mode symlink=$symlink bytes=$bytes") {
                val reference = ProtectedReference("/private/synthetic", mode, symlink, bytes, "PII")
                if (mode == "0600" && !symlink && bytes == 32) {
                    reference.validate("PII_KEY_FILE", "PII")
                } else {
                    assertThrows(ConfigurationViolation::class.java) { reference.validate("PII_KEY_FILE", "PII") }
                }
                assertFalse(reference.toString().contains("/private/synthetic"))
            }
        }

    @Test
    fun `ATDD-290 independent purposes and references fail without revealing material`() {
        val first = ByteArray(32) { 1 }
        val second = ByteArray(32) { 2 }
        CompatibilityFacts.independentPurposeKeys(mapOf("PII" to first, "DRAFT" to second))
        assertThrows(ConfigurationViolation::class.java) {
            CompatibilityFacts.independentPurposeKeys(
                mapOf("PII" to first, "DRAFT" to first.copyOf()),
            )
        }
        assertThrows(ConfigurationViolation::class.java) {
            CompatibilityFacts.independentPurposeKeys(
                mapOf("PII" to ByteArray(31)),
            )
        }
        assertThrows(ConfigurationViolation::class.java) {
            ProtectedReference("/private/synthetic", "0600", false, 32, "DRAFT").validate("PII_KEY_FILE", "PII")
        }
        assertThrows(ConfigurationViolation::class.java) {
            ConfigurationSources(mapOf("PRIVATE_FILE" to "\u0085 ")).required("PRIVATE_FILE")
        }
    }

    @Test
    fun `ATDD-290 enabled tunnel and publication require complete independent references`() {
        val values =
            mapOf(
                "TUNNEL_ENABLED" to "true",
                "TUNNEL_ID" to "test",
                "TUNNEL_KEY_FILE" to "/private/key",
                "TUNNEL_RUNNER_PATH" to "/private/runner",
                "MCP_PUBLICATION_ENABLED" to "true",
                "MCP_UI_ASSERTION_KEY_FILE" to "/private/ui-key",
                "MCP_UI_ASSERTION_KEY_ID" to "ui-v1",
            )
        val actual = OperationalSettings.modes(ConfigurationSources(values))
        for (name in listOf(
            "TUNNEL_ID",
            "TUNNEL_KEY_FILE",
            "TUNNEL_RUNNER_PATH",
            "MCP_UI_ASSERTION_KEY_FILE",
            "MCP_UI_ASSERTION_KEY_ID",
        )) {
            assertEquals(values[name], actual[name])
            assertEquals(
                name,
                assertThrows(ConfigurationViolation::class.java) {
                    OperationalSettings.modes(
                        ConfigurationSources(values - name),
                    )
                }.setting,
            )
        }
    }
}
