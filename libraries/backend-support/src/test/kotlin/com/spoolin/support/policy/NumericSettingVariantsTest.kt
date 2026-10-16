package com.spoolin.support.policy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

class NumericSettingVariantsTest {
    private val smtp =
        mapOf(
            "NOTIFICATION_MODE" to "smtp",
            "SMTP_HOST" to "smtp.example.test",
            "SMTP_FROM" to "sender@example.test",
            "SMTP_TO" to "recipient@example.test",
        )
    private val malformed =
        mapOf(
            "empty" to "",
            "whitespace" to " ",
            "NEL" to "\u0085",
            "fraction" to "1.5",
            "NaN" to "NaN",
            "overflow" to "2147483648",
            "negative-overflow" to "-2147483649",
            "alphabetic" to "SYNTHETIC_PRIVATE_VALUE",
            "plus" to "+1",
            "exponent" to "1e3",
            "newline" to "1\n",
            "NUL" to "1\u0000",
        )

    private fun sources(
        spec: SelectedNumber,
        environment: Map<String, String>,
        file: Map<String, String>,
    ) = ConfigurationSources((if (spec.mode == "smtp") smtp else emptyMap()) + environment, file)

    @TestFactory
    fun namedSources() =
        SelectedNumbers.all.flatMap { spec ->
            listOf("omitted-default", "file", "environment-over-file").map { variant ->
                dynamicTest("F-CONFIG ${spec.name} source $variant") {
                    val file =
                        if (variant ==
                            "omitted-default"
                        ) {
                            emptyMap()
                        } else {
                            mapOf(spec.name to spec.alternate.toString())
                        }
                    val environment =
                        if (variant ==
                            "environment-over-file"
                        ) {
                            mapOf(spec.name to spec.default.toString())
                        } else {
                            emptyMap()
                        }
                    val expected = if (variant == "file") spec.alternate else spec.default
                    val origin =
                        when (variant) {
                            "file" -> ValueSource.FILE
                            "omitted-default" -> ValueSource.DEFAULT
                            else -> ValueSource.ENVIRONMENT
                        }
                    assertEquals(SettingValue(expected, origin), spec.read(sources(spec, environment, file)))
                }
            }
        }

    @TestFactory
    fun namedInvalidPresence() =
        SelectedNumbers.all.flatMap { spec ->
            malformed.map { (variant, value) ->
                dynamicTest("F-CONFIG ${spec.name} invalid-present $variant over valid file") {
                    VariantAssertions.refuses(spec.name) {
                        spec.read(sources(spec, mapOf(spec.name to value), mapOf(spec.name to spec.default.toString())))
                    }
                }
            }
        }

    @TestFactory
    fun namedRejectedBounds() =
        SelectedNumbers.all.flatMap { spec ->
            listOf(
                "lower-minus-one" to spec.minimum.toLong() - 1,
                "upper-plus-one" to spec.maximum.toLong() + 1,
            ).map { (variant, value) ->
                dynamicTest("F-CONFIG ${spec.name} rejection $variant with safe correction") {
                    VariantAssertions.refuses(spec.name) {
                        spec.read(sources(spec, mapOf(spec.name to value.toString()), emptyMap()))
                    }
                }
            }
        }

    @TestFactory
    fun namedZeroAndNegativePresence() =
        SelectedNumbers.all.flatMap { spec ->
            listOf("negative" to -1, "zero" to 0).map { (variant, value) ->
                dynamicTest("F-CONFIG ${spec.name} explicit $variant classification") {
                    val actual =
                        sources(
                            spec,
                            mapOf(spec.name to value.toString()),
                            mapOf(spec.name to spec.default.toString()),
                        )
                    if (value in spec.minimum..spec.maximum) {
                        assertEquals(SettingValue(value, ValueSource.ENVIRONMENT), spec.read(actual))
                    } else {
                        VariantAssertions.refuses(spec.name) { spec.read(actual) }
                    }
                }
            }
        }

    @TestFactory
    fun exactArtworkAdjacentBounds() =
        listOf(0, 1, 2, 5242879, 5242880, 5242881).map { value ->
            dynamicTest("F-CONFIG ARTWORK_MAX_INPUT_BYTES adjacent $value") {
                val source = ConfigurationSources(mapOf("ARTWORK_MAX_INPUT_BYTES" to value.toString()))
                if (value in 1..5242880) {
                    assertEquals(value.toLong(), PolicySettings.resolve(source).artwork.maxInputBytes)
                } else {
                    VariantAssertions.refuses("ARTWORK_MAX_INPUT_BYTES") { PolicySettings.resolve(source) }
                }
            }
        }
}
