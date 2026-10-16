package com.spoolin.support.policy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

class ChoiceSettingVariantsTest {
    private val baseline =
        mapOf(
            "TUNNEL_ENABLED" to "false",
            "SMTP_HOST" to "smtp.example.test",
            "SMTP_FROM" to "sender@example.test",
            "SMTP_TO" to "recipient@example.test",
            "SLACK_WEBHOOK_FILE" to "/private/synthetic-slack",
            "GOOGLE_CHAT_WEBHOOK_FILE" to "/private/synthetic-chat",
        )
    private val choices =
        listOf(
            Triple("NOTIFICATION_MODE", "database", "smtp"),
            Triple("ARTWORK_FORMATS", "JPEG,PNG,WEBP", "PNG"),
            Triple("LOG_LEVEL", "info", "debug"),
        )

    private fun read(
        name: String,
        source: ConfigurationSources,
    ): String =
        when (name) {
            "NOTIFICATION_MODE" -> {
                val result = NotificationSettings.resolve(source)
                val selected = requireNotNull(source.value(name, "database")).value
                val expected = if (selected == "smtp") setOf("SMTP_HOST", "SMTP_FROM", "SMTP_TO") else emptySet()
                assertEquals(expected, result.keys)
                selected
            }
            "ARTWORK_FORMATS" ->
                PolicySettings
                    .resolve(source)
                    .artwork.formats
                    .joinToString(",")
            else -> {
                OperationalSettings.modes(source)
                requireNotNull(source.value(name, "info")).value
            }
        }

    @TestFactory
    fun namedSources() =
        choices.flatMap { (name, default, alternate) ->
            listOf("omitted-default", "file", "environment-over-file").map { variant ->
                dynamicTest("F-CONFIG $name source $variant") {
                    val file = if (variant == "omitted-default") emptyMap() else mapOf(name to alternate)
                    val environment =
                        baseline + if (variant == "environment-over-file") mapOf(name to default) else emptyMap()
                    val source = ConfigurationSources(environment, file)
                    val expected = if (variant == "file") alternate else default
                    val origin =
                        when (variant) {
                            "file" -> ValueSource.FILE
                            "omitted-default" -> ValueSource.DEFAULT
                            else -> ValueSource.ENVIRONMENT
                        }
                    assertEquals(expected, read(name, source))
                    assertEquals(SettingValue(expected, origin), source.value(name, default))
                }
            }
        }

    @TestFactory
    fun namedInvalidPresence() =
        choices.flatMap { (name, default, _) ->
            mapOf(
                "empty" to "",
                "whitespace" to " ",
                "NEL" to "\u0085",
                "unsupported" to "SYNTHETIC_PRIVATE_VALUE",
            ).map { (variant, value) ->
                dynamicTest("F-CONFIG $name invalid-present $variant over valid file") {
                    val source = ConfigurationSources(baseline + (name to value), mapOf(name to default))
                    VariantAssertions.refuses(name) { read(name, source) }
                }
            }
        }
}
