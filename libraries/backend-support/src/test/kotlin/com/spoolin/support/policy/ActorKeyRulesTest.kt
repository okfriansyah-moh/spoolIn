package com.spoolin.support.policy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.spec.NamedParameterSpec

class ActorKeyRulesTest {
    private fun pair() =
        KeyPairGenerator
            .getInstance("Ed25519")
            .apply {
                val random = SecureRandom.getInstance("SHA1PRNG").apply { setSeed(ByteArray(32) { 7 }) }
                initialize(NamedParameterSpec.ED25519, random)
            }.generateKeyPair()

    @Test
    fun `F-CONFIG actor signing and verifier formats use supplied DER bytes`() {
        val pair = pair()
        assertEquals("PKCS#8", ActorKeyRules.signing(pair.private.encoded).format)
        assertEquals(
            "X.509",
            ActorKeyRules.verifiers(mapOf("actor-v1" to pair.public.encoded)).getValue("actor-v1").format,
        )
        for (invalid in listOf(ByteArray(0), pair.public.encoded)) {
            val error = assertThrows(ConfigurationViolation::class.java) { ActorKeyRules.signing(invalid) }
            assertEquals("ACTOR_SIGNING_PRIVATE_KEY_FILE", error.setting)
            assertFalse(
                error.message.orEmpty().contains(
                    java.util.Base64
                        .getEncoder()
                        .encodeToString(pair.public.encoded),
                ),
            )
        }
        assertThrows(
            ConfigurationViolation::class.java,
        ) { ActorKeyRules.verifiers(mapOf("actor" to pair.private.encoded)) }
    }

    @Test
    fun `F-CONFIG verifier entries are bounded and key IDs obey source syntax`() {
        val key = pair().public.encoded
        for (size in listOf(0, 1, 2, 3, 4, 5)) {
            val input = (1..size).associate { "key-$it" to key }
            if (size in 1..4) {
                assertEquals(size, ActorKeyRules.verifiers(input).size)
            } else {
                assertThrows(ConfigurationViolation::class.java) { ActorKeyRules.verifiers(input) }
            }
        }
        assertThrows(ConfigurationViolation::class.java) { ActorKeyRules.verifiers(mapOf("actor.invalid" to key)) }
    }
}
