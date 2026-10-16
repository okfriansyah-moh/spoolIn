package com.spoolin.support.policy

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

class ConfigurationKeyRulesTest {
    @TestFactory
    fun keyIdentifiers() =
        listOf(
            dynamicTest("ATDD-290 actor key ID permits leading underscore and hyphen") {
                assertDoesNotThrow { CompatibilityFacts.keyId("_actor-v1") }
                assertDoesNotThrow { CompatibilityFacts.keyId("-key") }
            },
            dynamicTest("ATDD-290 actor key ID excludes dot slash space and excessive length") {
                for (value in listOf("", "actor.v1", "actor/key", "actor key", "x".repeat(65))) {
                    assertThrows(ConfigurationViolation::class.java) { CompatibilityFacts.keyId(value) }
                }
            },
        )
}
