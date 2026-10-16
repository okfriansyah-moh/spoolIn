package com.spoolin.support.identity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.security.KeyPairGenerator
import java.security.Signature
import java.time.Instant
import java.util.Base64
import java.util.UUID

class ActorAssertionsTest {
    private val keys = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
    private val now = Instant.ofEpochSecond(1800000000)
    private val actor = UUID.fromString("10000000-0000-0000-0000-000000000001")
    private val operation = UUID.fromString("20000000-0000-0000-0000-000000000002")
    private val method = "/spoolin.v1.CatalogService/WriteMovie"

    @Test
    fun `real signature verifies only exact receiver method actor and operation`() {
        val claims = claims()
        val token = ActorSigner("key_A", keys.private).sign(claims)
        val verifier = ActorVerifier(mapOf("key_A" to keys.public))
        assertEquals(claims, verifier.verify(token, ActorBinding("catalog", method, actor, operation), now))
        listOf(
            ActorBinding("media", method, actor, operation),
            ActorBinding("catalog", "$method-other", actor, operation),
            ActorBinding("catalog", method, UUID.randomUUID(), operation),
            ActorBinding("catalog", method, actor, UUID.randomUUID()),
        ).forEach { binding ->
            assertThrows(ActorRejected::class.java) { verifier.verify(token, binding, now) }
        }
    }

    @TestFactory
    fun `signed hostile claims and encoding are rejected`() =
        listOf(
            "issuer" to "\"issuer\":\"media\"",
            "duplicate issuer" to "\"issuer\":\"core\",\"issuer\":\"core\"",
            "algorithm" to "\"issuer\":\"core\",\"alg\":\"EdDSA\"",
            "epoch number" to "\"authEpoch\":5",
            "negative epoch" to "\"authEpoch\":\"-1\"",
            "zero prefixed epoch" to "\"authEpoch\":\"05\"",
            "unknown role" to "\"role\":\"ADMIN\"",
            "credit role" to "\"role\":\"Actor\"",
            "client kind" to "\"clientKind\":\"untrusted\"",
            "nonce short" to "\"nonce\":\"AA\"",
            "nonce padding" to "\"nonce\":\"AAAAAAAAAAAAAAAAAAAAAA==\"",
            "duration zero" to "\"expiresAt\":1800000000",
            "duration 31" to "\"expiresAt\":1800000031",
            "expired" to "\"issuedAt\":1799999967,\"expiresAt\":1799999997",
            "future" to "\"issuedAt\":1800000003,\"expiresAt\":1800000030",
        ).map { (name, replacement) ->
            dynamicTest(name) {
                val token = ActorSigner("key_A", keys.private).sign(claims())
                val json = String(Base64.getUrlDecoder().decode(token.split('.')[1]))
                val field = replacement.substringAfter('"').substringBefore('"')
                val altered = json.replace(Regex(""""$field":("[^"]*"|[0-9]+|null)"""), replacement)
                assertThrows(ActorRejected::class.java) { verify(raw(altered)) }
            }
        }

    @Test
    fun `signature key metadata and base64 alterations fail safely`() {
        val token = ActorSigner("key_A", keys.private).sign(claims())
        val foreign = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
        val verifier = ActorVerifier(mapOf("key_A" to foreign.public))
        assertThrows(ActorRejected::class.java) {
            verifier.verify(token, ActorBinding("catalog", method, actor, operation), now)
        }
        listOf("", "x".repeat(4097), token + "=", token.replace("key_A", "unknown"), "$token.extra").forEach {
            assertThrows(ActorRejected::class.java) { verify(it) }
        }
    }

    @Test
    fun `two second skew and thirty second lifetime boundaries are accepted`() {
        val token = ActorSigner("key_A", keys.private).sign(claims())
        val verifier = ActorVerifier(mapOf("key_A" to keys.public))
        val binding = ActorBinding("catalog", method, actor, operation)
        assertEquals(claims(), verifier.verify(token, binding, now.minusSeconds(2)))
        assertEquals(claims(), verifier.verify(token, binding, now.plusSeconds(32)))
        assertThrows(ActorRejected::class.java) { verifier.verify(token, binding, now.plusSeconds(33)) }
    }

    @Test
    fun `bounded verifier set startup and signed time failures are explicit`() {
        assertThrows(IllegalArgumentException::class.java) { ActorVerifier(emptyMap()) }
        assertThrows(IllegalArgumentException::class.java) {
            ActorVerifier((1..5).associate { "key$it" to keys.public })
        }
        val rsa = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        assertThrows(IllegalArgumentException::class.java) { ActorVerifier(mapOf("rsa" to rsa.public)) }
        assertThrows(IllegalArgumentException::class.java) { ActorSigner("rsa", rsa.private) }
        val expired = claims().copy(issuedAt = now.epochSecond - 33, expiresAt = now.epochSecond - 3)
        assertThrows(ActorRejected::class.java) { verify(ActorSigner("key_A", keys.private).sign(expired)) }
    }

    private fun verify(token: String) =
        ActorVerifier(
            mapOf("key_A" to keys.public),
        ).verify(token, ActorBinding("catalog", method, actor, operation), now)

    private fun raw(json: String): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val input = "key_A.${encoder.encodeToString(json.toByteArray())}"
        val signature = Signature.getInstance("Ed25519")
        signature.initSign(keys.private)
        signature.update(input.toByteArray())
        return "$input.${encoder.encodeToString(signature.sign())}"
    }

    private fun claims() =
        ActorClaims(
            "catalog",
            method,
            actor,
            UUID.fromString("30000000-0000-0000-0000-000000000003"),
            null,
            "MAINTAINER",
            5,
            now.epochSecond,
            now.epochSecond + 30,
            "AAAAAAAAAAAAAAAAAAAAAA",
            UUID.fromString("40000000-0000-0000-0000-000000000004"),
            operation,
        )
}
