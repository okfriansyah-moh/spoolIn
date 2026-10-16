package com.spoolin.support.policy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

class ReferenceSettingVariantsTest {
    @TestFactory
    fun activeNamedSources() =
        SelectedReferences.all.flatMap { spec ->
            listOf("environment", "file", "environment-over-file").map { variant ->
                dynamicTest("F-CONFIG ${spec.name} active source $variant") {
                    val env = if (variant == "file") emptyMap() else mapOf(spec.name to spec.environmentValue)
                    val file = if (variant == "environment") emptyMap() else mapOf(spec.name to spec.fileValue)
                    val source = spec.sources(env, file)
                    val actual = spec.read(source).getValue(spec.name)
                    val expected = if (variant == "file") spec.fileValue else spec.environmentValue
                    val origin = if (variant == "file") ValueSource.FILE else ValueSource.ENVIRONMENT
                    assertTrue(actual == expected, "Selected active reference must come from the expected source.")
                    assertEquals(SettingValue(expected, origin), source.value(spec.name))
                }
            }
        }

    @TestFactory
    fun activeNamedRefusals() =
        SelectedReferences.all.flatMap { spec ->
            mapOf(
                "missing" to null,
                "empty" to "",
                "whitespace" to " ",
                "NEL" to "\u0085",
                "newline" to "SYNTHETIC_PRIVATE_VALUE\n",
                "NUL" to "SYNTHETIC_PRIVATE_VALUE\u0000",
            ).map { (variant, value) ->
                dynamicTest("F-CONFIG ${spec.name} active refusal $variant") {
                    val environment = if (value == null) emptyMap() else mapOf(spec.name to value)
                    val file = if (variant == "missing") emptyMap() else mapOf(spec.name to spec.fileValue)
                    VariantAssertions.refuses(spec.name) { spec.read(spec.sources(environment, file)) }
                }
            }
        }

    @TestFactory
    fun inactiveNamedEmptyValues() =
        SelectedReferences.all.map { spec ->
            dynamicTest("F-CONFIG ${spec.name} inactive explicit empty allowed") {
                val result = spec.read(spec.disabled())
                assertTrue(
                    result[spec.name] == null,
                    "Inactive adapter reference must not become a required active credential.",
                )
            }
        }
}
