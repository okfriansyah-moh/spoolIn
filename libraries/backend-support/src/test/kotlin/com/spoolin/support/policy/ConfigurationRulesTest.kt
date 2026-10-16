package com.spoolin.support.policy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class ConfigurationRulesTest {
    private val integers =
        listOf(
            IntegerSetting("RETRY_MAX_RETRIES", 2, 0, 9, "retries"),
            IntegerSetting("RETRY_BASE_DELAY_MS", 500, 1, 60000, "milliseconds"),
            IntegerSetting("RETRY_MAX_DELAY_MS", 5000, 1, 3600000, "milliseconds"),
            IntegerSetting("RETRY_DEADLINE_MS", 60000, 1000, 3600000, "milliseconds"),
            IntegerSetting("RETRY_MULTIPLIER", 2, 1, 10, "multiplier"),
            IntegerSetting("MCP_RATE_LIMIT_MAX_BINDINGS", 1024, 1, 100000, "bindings"),
        ) +
            listOf(
                "SIGNIN_ACCOUNT" to (10 to 900),
                "SIGNIN_SOURCE" to (60 to 900),
                "REGISTER" to (5 to 3600),
                "MUTATION" to (60 to 60),
                "READ" to (120 to 60),
            ).flatMap { (profile, values) ->
                listOf(
                    IntegerSetting("RATE_LIMIT_${profile}_MAX_REQUESTS", values.first, 1, 2147483647, "requests"),
                    IntegerSetting("RATE_LIMIT_${profile}_WINDOW_SECONDS", values.second, 1, 86400, "seconds"),
                )
            }

    @TestFactory
    fun numericRecipes() =
        integers.flatMap { spec ->
            listOf(
                spec.min.toLong() - 1,
                spec.min.toLong(),
                spec.min.toLong() + 1,
                spec.max.toLong() - 1,
                spec.max.toLong(),
                spec.max.toLong() + 1,
            ).distinct()
                .map { value ->
                    dynamicTest("ATDD-290 ${spec.name} boundary $value") {
                        val source = ConfigurationSources(mapOf(spec.name to value.toString()))
                        if (value in
                            spec.min.toLong()..spec.max.toLong()
                        ) {
                            assertEquals(
                                value.toInt(),
                                source.integer(spec.name, spec.default, spec.min, spec.max).value,
                            )
                        } else {
                            assertEquals(
                                spec.name,
                                assertThrows(ConfigurationViolation::class.java) {
                                    source.integer(spec.name, spec.default, spec.min, spec.max)
                                }.setting,
                            )
                        }
                    }
                } +
                listOf("", " ", "NaN", "1.5", "+1", "1e3", "2147483648", "-2147483649").map { value ->
                    dynamicTest("ATDD-290 ${spec.name} malformed ${value.ifEmpty { "empty" }}") {
                        assertThrows(ConfigurationViolation::class.java) {
                            ConfigurationSources(
                                mapOf(spec.name to value),
                            ).integer(spec.name, spec.default, spec.min, spec.max)
                        }
                    }
                }
        }

    private val precedenceRecipes =
        listOf(
            dynamicTest("ATDD-289 zero environment wins") {
                assertEquals(
                    SettingValue(0, ValueSource.ENVIRONMENT),
                    ConfigurationSources(
                        mapOf("R" to "0"),
                        mapOf("R" to "2"),
                    ).integer("R", 2, 0, 9),
                )
            },
            dynamicTest("ATDD-289 false environment wins") {
                assertEquals(
                    SettingValue(false, ValueSource.ENVIRONMENT),
                    ConfigurationSources(
                        mapOf("P" to "false"),
                        mapOf("P" to "true"),
                    ).boolean("P", true),
                )
            },
            dynamicTest("ATDD-289 omitted env uses file") {
                assertEquals(
                    SettingValue(3, ValueSource.FILE),
                    ConfigurationSources(
                        file =
                            mapOf("R" to "3"),
                    ).integer("R", 2, 0, 9),
                )
            },
            dynamicTest("ATDD-289 omitted sources uses declared default") {
                assertEquals(SettingValue(2, ValueSource.DEFAULT), ConfigurationSources().integer("R", 2, 0, 9))
            },
            dynamicTest("ATDD-289 invalid present never falls through") {
                assertThrows(ConfigurationViolation::class.java) {
                    ConfigurationSources(
                        mapOf("R" to ""),
                        mapOf("R" to "2"),
                    ).integer("R", 2, 0, 9)
                }
            },
            dynamicTest("ATDD-289 secret has no fallback") {
                assertThrows(ConfigurationViolation::class.java) { ConfigurationSources().required("CSRF_KEY_FILE") }
            },
            dynamicTest("ATDD-289 source print redacted") {
                assertFalse(
                    ConfigurationSources(
                        mapOf("secret" to "SYNTHETIC_PRIVATE_VALUE"),
                    ).toString().contains("SYNTHETIC_PRIVATE_VALUE"),
                )
            },
            dynamicTest("ATDD-290 reversed retry cap") {
                assertThrows(ConfigurationViolation::class.java) {
                    OperationalSettings.resolve(
                        ConfigurationSources(
                            mapOf("RETRY_MAX_DELAY_MS" to "1"),
                        ),
                    )
                }
            },
            dynamicTest("ATDD-290 defaults resolve") {
                assertEquals(
                    500,
                    OperationalSettings.resolve(ConfigurationSources()).getValue("RETRY_BASE_DELAY_MS").value,
                )
            },
        )

    private val modeRecipes =
        listOf(
            dynamicTest("ATDD-290 database and disabled tunnel allow empty adapters") {
                OperationalSettings.modes(
                    ConfigurationSources(
                        mapOf("TUNNEL_ENABLED" to "false", "TUNNEL_ID" to "", "MCP_UI_ASSERTION_KEY_FILE" to ""),
                    ),
                )
            },
            dynamicTest("ATDD-290 enabled tunnel requires references") {
                assertThrows(ConfigurationViolation::class.java) { OperationalSettings.modes(ConfigurationSources()) }
            },
            dynamicTest("ATDD-290 publication true requires own assertion reference") {
                assertEquals(
                    "MCP_UI_ASSERTION_KEY_FILE",
                    assertThrows(ConfigurationViolation::class.java) {
                        OperationalSettings.modes(
                            ConfigurationSources(
                                mapOf("TUNNEL_ENABLED" to "false", "MCP_PUBLICATION_ENABLED" to "true"),
                            ),
                        )
                    }.setting,
                )
            },
            *listOf("smtp", "slack", "google_chat", "unknown", "")
                .map { mode ->
                    dynamicTest("ATDD-290 enabled adapter $mode without required settings") {
                        assertThrows(ConfigurationViolation::class.java) {
                            OperationalSettings.modes(
                                ConfigurationSources(
                                    mapOf("NOTIFICATION_MODE" to mode, "TUNNEL_ENABLED" to "false"),
                                ),
                            )
                        }
                    }
                }.toTypedArray(),
            *listOf("TRUE", "False", "0", "1", "", " ")
                .map { value ->
                    dynamicTest("ATDD-290 boolean invalid $value") {
                        assertThrows(ConfigurationViolation::class.java) {
                            ConfigurationSources(
                                mapOf("P" to value),
                            ).boolean("P", false)
                        }
                    }
                }.toTypedArray(),
            *listOf("debug", "info")
                .map { level ->
                    dynamicTest("ATDD-290 log level $level") {
                        assertEquals(
                            level,
                            ConfigurationSources(
                                mapOf("LOG_LEVEL" to level),
                            ).choice("LOG_LEVEL", "info", setOf("debug", "info")).value,
                        )
                    }
                }.toTypedArray(),
            dynamicTest("ATDD-290 unsupported log level") {
                assertThrows(ConfigurationViolation::class.java) {
                    OperationalSettings.modes(
                        ConfigurationSources(
                            mapOf("LOG_LEVEL" to "trace", "TUNNEL_ENABLED" to "false"),
                        ),
                    )
                }
            },
        )

    @Test
    fun `ATDD-290 private invalid values never enter correction`() {
        val failure =
            assertThrows(ConfigurationViolation::class.java) {
                ConfigurationSources(
                    mapOf("RETRY_MAX_RETRIES" to "SYNTHETIC_PASSWORD_VALUE"),
                ).integer("RETRY_MAX_RETRIES", 2, 0, 9)
            }
        assertTrue(failure.message.orEmpty().contains("RETRY_MAX_RETRIES"))
        assertFalse(failure.message.orEmpty().contains("SYNTHETIC_PASSWORD_VALUE"))
    }

    @TestFactory
    fun precedenceRecipes() = precedenceRecipes

    @TestFactory
    fun modeRecipes() = modeRecipes
}
