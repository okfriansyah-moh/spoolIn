package com.spoolin.support.policy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class PolicyRulesTest {
    @Test
    fun `T006 public policy has exact recipient shape and stable revision`() {
        val policy = PolicySettings.resolve(ConfigurationSources())
        assertEquals(setOf("revision", "textBounds", "artwork", "requestLimits"), policy.projection().keys)
        assertEquals(
            setOf(
                "registrationName",
                "personName",
                "title",
                "originalTitle",
                "synopsis",
                "creditRole",
                "characterName",
                "biography",
                "email",
                "password",
                "search",
                "sourceUrl",
                "reuseBasis",
            ),
            policy.textBounds.keys,
        )
        assertEquals(TextBound(2, 100), policy.textBounds.getValue("registrationName"))
        assertEquals(TextBound(15, 128, normalization = "EXACT"), policy.textBounds.getValue("password"))
        assertEquals("5242880", policy.artwork.projection()["maxInputBytes"])
        assertEquals(24, policy.requestLimits["defaultPage"])
        assertEquals(64, policy.revision.length)
        assertEquals(
            policy.revision,
            PolicySettings.resolve(ConfigurationSources(mapOf("LOG_LEVEL" to "debug"))).revision,
        )
        assertNotEquals(
            policy.revision,
            PolicySettings.resolve(ConfigurationSources(mapOf("PERSON_NAME_MAX_CODE_POINTS" to "101"))).revision,
        )
        assertTrue(policy.agrees(listOf(policy.revision, policy.revision)))
        assertFalse(policy.agrees(listOf(policy.revision, "different")))
        assertFalse(policy.agrees(emptyList()))
    }

    private val policyRecipes =
        listOf(
            dynamicTest("ATDD-290 reversed text bounds") {
                assertThrows(ConfigurationViolation::class.java) {
                    PolicySettings.resolve(
                        ConfigurationSources(
                            mapOf("TITLE_MIN_CODE_POINTS" to "300"),
                        ),
                    )
                }
            },
            dynamicTest("ATDD-290 required min cannot zero") {
                assertThrows(ConfigurationViolation::class.java) {
                    PolicySettings.resolve(
                        ConfigurationSources(
                            mapOf("PERSON_NAME_MIN_CODE_POINTS" to "0"),
                        ),
                    )
                }
            },
            dynamicTest("ATDD-290 page default above max") {
                assertThrows(ConfigurationViolation::class.java) {
                    PolicySettings.resolve(
                        ConfigurationSources(
                            mapOf("REQUEST_MAX_PAGE" to "10"),
                        ),
                    )
                }
            },
            dynamicTest("ATDD-290 account capacity exceeded") {
                assertThrows(ConfigurationViolation::class.java) {
                    PolicySettings.resolve(
                        ConfigurationSources(
                            mapOf("REGISTRATION_NAME_MAX_CODE_POINTS" to "15000"),
                        ),
                    )
                }
            },
            dynamicTest("ATDD-290 increased password codepoints fits ASCII byte ceiling") {
                assertEquals(
                    512,
                    PolicySettings
                        .resolve(
                            ConfigurationSources(
                                mapOf("PASSWORD_MAX_CODE_POINTS" to "512"),
                            ),
                        ).textBounds
                        .getValue("password")
                        .maxCodePoints,
                )
            },
            dynamicTest("ATDD-290 password max beyond byte ceiling") {
                assertThrows(ConfigurationViolation::class.java) {
                    PolicySettings.resolve(
                        ConfigurationSources(
                            mapOf("PASSWORD_MAX_CODE_POINTS" to "513"),
                        ),
                    )
                }
            },
            *listOf("", "GIF", "JPEG,JPEG", "JPEG, PNG")
                .map { formats ->
                    dynamicTest("ATDD-290 formats rejected $formats") {
                        assertThrows(ConfigurationViolation::class.java) {
                            PolicySettings.resolve(
                                ConfigurationSources(
                                    mapOf("ARTWORK_FORMATS" to formats),
                                ),
                            )
                        }
                    }
                }.toTypedArray(),
            dynamicTest("ATDD-290 nonempty enabled subset") {
                assertEquals(
                    listOf("WEBP"),
                    PolicySettings
                        .resolve(
                            ConfigurationSources(
                                mapOf("ARTWORK_FORMATS" to "WEBP"),
                            ),
                        ).artwork.formats,
                )
            },
            dynamicTest("ATDD-290 artwork byte max plus one") {
                assertThrows(ConfigurationViolation::class.java) {
                    PolicySettings.resolve(
                        ConfigurationSources(
                            mapOf("ARTWORK_MAX_INPUT_BYTES" to "5242881"),
                        ),
                    )
                }
            },
            dynamicTest("ATDD-290 metadata cannot fit fields") {
                assertThrows(ConfigurationViolation::class.java) {
                    PolicySettings.resolve(
                        ConfigurationSources(
                            mapOf("REQUEST_METADATA_BYTES" to "10"),
                        ),
                    )
                }
            },
        )

    @Test
    fun `T006 stable deterministic policy serialization`() {
        assertEquals(
            "{\"a\":1,\"b\":[true,null,\"x\"]}",
            PolicyJson.encode(
                mapOf(
                    "b" to listOf(true, null, "x"),
                    "a" to 1,
                ),
            ),
        )
        assertEquals("\"a\\\"b\\\\c\\u000a\"", PolicyJson.encode("a\"b\\c\n"))
    }

    @TestFactory
    fun policyRecipes() = policyRecipes
}
