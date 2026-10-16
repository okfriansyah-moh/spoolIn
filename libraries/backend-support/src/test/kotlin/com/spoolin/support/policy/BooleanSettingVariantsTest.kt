package com.spoolin.support.policy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

class BooleanSettingVariantsTest {
    private val defaults =
        mapOf("TUNNEL_ENABLED" to true, "MCP_PUBLICATION_ENABLED" to false, "SMTP_TLS_VERIFY" to true)
    private val baseline =
        mapOf(
            "TUNNEL_ID" to "synthetic-tunnel",
            "TUNNEL_KEY_FILE" to "/private/synthetic-file",
            "TUNNEL_RUNNER_PATH" to "/private/synthetic-runner",
            "MCP_UI_ASSERTION_KEY_FILE" to "/private/synthetic-file",
            "MCP_UI_ASSERTION_KEY_ID" to "ui-first",
            "NOTIFICATION_MODE" to "smtp",
            "SMTP_HOST" to "smtp.example.test",
            "SMTP_FROM" to "sender@example.test",
            "SMTP_TO" to "recipient@example.test",
        )

    private fun consumer(
        name: String,
        source: ConfigurationSources,
        expected: Boolean,
    ) {
        if (name == "SMTP_TLS_VERIFY") {
            if (expected) {
                NotificationSettings.resolve(source)
            } else {
                VariantAssertions.refuses(name) {
                    NotificationSettings.resolve(source)
                }
            }
        } else {
            val result = OperationalSettings.modes(source)
            val member = if (name == "TUNNEL_ENABLED") "TUNNEL_ID" else "MCP_UI_ASSERTION_KEY_ID"
            if (expected) assertNotNull(result[member]) else assertNull(result[member])
        }
    }

    private fun source(
        name: String,
        environment: Map<String, String>,
        file: Map<String, String>,
    ): ConfigurationSources {
        val selected = environment[name] ?: file[name] ?: defaults.getValue(name).toString()
        val disabledMembers =
            when {
                selected != "false" -> emptySet()
                name == "TUNNEL_ENABLED" -> setOf("TUNNEL_ID", "TUNNEL_KEY_FILE", "TUNNEL_RUNNER_PATH")
                name == "MCP_PUBLICATION_ENABLED" -> setOf("MCP_UI_ASSERTION_KEY_FILE", "MCP_UI_ASSERTION_KEY_ID")
                else -> emptySet()
            }
        val active =
            baseline.filterKeys { key ->
                key !in disabledMembers && (name == "SMTP_TLS_VERIFY" || key != "NOTIFICATION_MODE")
            }
        return ConfigurationSources(active + environment, file)
    }

    private fun sourceFile(
        name: String,
        variant: String,
    ): Map<String, String> =
        when (variant) {
            "file-true", "environment-false-over-true-file" -> mapOf(name to "true")
            "file-false", "environment-true" -> mapOf(name to "false")
            else -> emptyMap()
        }

    private fun sourceEnvironment(
        name: String,
        variant: String,
    ): Map<String, String> =
        when (variant) {
            "environment-true" -> mapOf(name to "true")
            "environment-false-over-true-file" -> mapOf(name to "false")
            else -> emptyMap()
        }

    private fun expected(
        variant: String,
        default: Boolean,
    ): Boolean =
        when (variant) {
            "omitted-default" -> default
            "file-true", "environment-true" -> true
            else -> false
        }

    private fun sourceOrigin(
        environment: Map<String, String>,
        file: Map<String, String>,
    ): ValueSource =
        when {
            environment.isNotEmpty() -> ValueSource.ENVIRONMENT
            file.isNotEmpty() -> ValueSource.FILE
            else -> ValueSource.DEFAULT
        }

    @TestFactory
    fun namedSources() =
        defaults.flatMap { (name, default) ->
            listOf(
                "omitted-default",
                "file-true",
                "file-false",
                "environment-true",
                "environment-false-over-true-file",
            ).map { variant ->
                dynamicTest("F-CONFIG $name source $variant") {
                    val file = sourceFile(name, variant)
                    val environment = sourceEnvironment(name, variant)
                    val expected = expected(variant, default)
                    val origin = sourceOrigin(environment, file)
                    val actual = source(name, environment, file)
                    assertEquals(SettingValue(expected, origin), actual.boolean(name, default))
                    consumer(name, actual, expected)
                }
            }
        }

    @TestFactory
    fun namedInvalidPresence() =
        defaults.flatMap { (name, default) ->
            mapOf(
                "empty" to "",
                "whitespace" to " ",
                "NEL" to "\u0085",
                "uppercase" to "TRUE",
                "mixed-case" to "False",
                "zero" to "0",
                "one" to "1",
                "newline" to "true\n",
                "private" to "SYNTHETIC_PRIVATE_VALUE",
            ).map { (variant, value) ->
                dynamicTest("F-CONFIG $name invalid-present $variant over valid file") {
                    val actual = source(name, mapOf(name to value), mapOf(name to default.toString()))
                    VariantAssertions.refuses(name) {
                        if (name ==
                            "SMTP_TLS_VERIFY"
                        ) {
                            NotificationSettings.resolve(actual)
                        } else {
                            OperationalSettings.modes(actual)
                        }
                    }
                }
            }
        }
}
