package com.spoolin.support.policy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class PolicyConfigurationBoundsRulesTest {
    private val upperBounds =
        mapOf(
            "registrationName" to 10406,
            "personName" to 21842,
            "title" to 21843,
            "originalTitle" to 21842,
            "synopsis" to 21842,
            "creditRole" to 21842,
            "characterName" to 21842,
            "biography" to 21842,
            "email" to 254,
            "password" to 512,
            "search" to 21843,
            "sourceUrl" to 2048,
            "reuseBasis" to 4000,
        )
    private val configurableMinima =
        setOf(
            "registrationName",
            "personName",
            "title",
            "originalTitle",
            "synopsis",
            "creditRole",
            "characterName",
            "biography",
            "password",
        )

    @TestFactory
    fun maximumConfigurationBounds() =
        upperBounds.flatMap { (field, upper) ->
            listOf(0, 1, 2, upper - 1, upper, upper + 1).map { value ->
                dynamicTest("F-CONFIG $field configured maximum $value (representable upper $upper)") {
                    val prefix = field.replace(Regex("([a-z])([A-Z])"), "$1_$2").uppercase()
                    val input =
                        mapOf("${prefix}_MAX_CODE_POINTS" to value.toString()) +
                            if (field in configurableMinima) mapOf("${prefix}_MIN_CODE_POINTS" to "1") else emptyMap()
                    if (value in 1..upper) {
                        val actual = PolicySettings.resolve(ConfigurationSources(input)).textBounds.getValue(field)
                        assertEquals(value, actual.maxCodePoints)
                        val encoded = PolicyJson.encode(mapOf(field to "\u0001".repeat(value)))
                        assertTrue(encoded.toByteArray(Charsets.UTF_8).size <= 131072)
                    } else {
                        assertThrows(
                            ConfigurationViolation::class.java,
                        ) { PolicySettings.resolve(ConfigurationSources(input)) }
                    }
                }
            }
        }

    @TestFactory
    fun minimumConfigurationBounds() =
        configurableMinima.flatMap { field ->
            val upper = upperBounds.getValue(field)
            listOf(0, 1, 2, upper - 1, upper, upper + 1).map { value ->
                dynamicTest("F-CONFIG $field configured minimum $value with selected maximum $upper") {
                    val prefix = field.replace(Regex("([a-z])([A-Z])"), "$1_$2").uppercase()
                    val input =
                        mapOf(
                            "${prefix}_MIN_CODE_POINTS" to value.toString(),
                            "${prefix}_MAX_CODE_POINTS" to upper.toString(),
                        )
                    if (value in 1..upper) {
                        assertEquals(
                            value,
                            PolicySettings
                                .resolve(ConfigurationSources(input))
                                .textBounds
                                .getValue(field)
                                .minCodePoints,
                        )
                    } else {
                        assertThrows(
                            ConfigurationViolation::class.java,
                        ) { PolicySettings.resolve(ConfigurationSources(input)) }
                    }
                }
            }
        }

    @TestFactory
    fun presenceAndMalformedConfiguration() =
        upperBounds.keys.flatMap { field ->
            val key = field.replace(Regex("([a-z])([A-Z])"), "$1_$2").uppercase() + "_MAX_CODE_POINTS"
            listOf("", " ", "NaN", "1.5", "-1", "2147483648").map { value ->
                dynamicTest("F-CONFIG $key invalid present value cannot fall through: ${value.ifEmpty { "empty" }}") {
                    assertThrows(ConfigurationViolation::class.java) {
                        PolicySettings.resolve(ConfigurationSources(mapOf(key to value), mapOf(key to "10")))
                    }
                }
            }
        }

    @TestFactory
    fun requestPolicyParameters() =
        mapOf(
            "metadataBytes" to 2147483647,
            "uploadEnvelopeBytes" to 2147483647,
            "graphqlQueryBytes" to 2147483647,
            "maxDepth" to 2147483647,
            "maxFields" to 2147483647,
            "maxAliases" to 2147483647,
            "maxCost" to 2147483647,
            "defaultPage" to 100,
            "maxPage" to 100,
        ).flatMap { (field, upper) ->
            listOf(0L, 1L, 2L, upper.toLong() - 1, upper.toLong(), upper.toLong() + 1).map { value ->
                dynamicTest("F-CONFIG request $field decimal bound $value") {
                    val name = "REQUEST_" + field.replace(Regex("([a-z])([A-Z])"), "$1_$2").uppercase()
                    val source = ConfigurationSources(mapOf(name to value.toString()))
                    if (value in 1..upper.toLong()) {
                        assertEquals(value.toInt(), source.integer(name, 1, 1, upper).value)
                    } else {
                        assertThrows(ConfigurationViolation::class.java) { source.integer(name, 1, 1, upper) }
                    }
                }
            }
        }

    @Test
    fun `F-CONFIG maximum account name fits complete fixed plaintext envelope`() {
        val payload =
            mapOf(
                "name" to "\u0001".repeat(10406),
                "emailDisplay" to "\u0001".repeat(254),
                "emailNormalized" to "\u0001".repeat(254),
            )
        assertEquals(65534, PolicyJson.encode(payload).toByteArray(Charsets.UTF_8).size)
        val excessive = payload + ("name" to "\u0001".repeat(10407))
        assertEquals(65540, PolicyJson.encode(excessive).toByteArray(Charsets.UTF_8).size)
    }
}
