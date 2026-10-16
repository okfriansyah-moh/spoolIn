package com.spoolin.support.receipts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class IntentRulesTest {
    @Test
    fun `matching identity replays while changed content conflicts and actor stays hidden`() {
        val id = UUID.randomUUID()
        val intent = OperationIntent(id, "account:A", "WriteMovie", "p1", listOf("name" to "movie"))
        val hashes = IntentHashes(mapOf("v1" to ByteArray(32) { 7 }), "v1")
        val digest = hashes.hash(intent, "v1")
        assertEquals(Comparison.REPLAY, hashes.compare(intent, "account:A", "WriteMovie", digest, "v1"))
        assertEquals(
            Comparison.HIDDEN,
            hashes.compare(intent.copy(actorScope = "account:B"), "account:A", "WriteMovie", digest, "v1"),
        )
        assertEquals(
            Comparison.CONFLICT,
            hashes.compare(intent.copy(policyRevision = "p2"), "account:A", "WriteMovie", digest, "v1"),
        )
        assertEquals(
            Comparison.CONFLICT,
            hashes.compare(intent.copy(fields = listOf("name" to "other")), "account:A", "WriteMovie", digest, "v1"),
        )
    }

    @Test
    fun `expiry is strict and retained keys never fall back to active`() {
        val cutoff = Instant.parse("2026-10-01T00:00:00Z")
        assertTrue(eligible(cutoff.minusNanos(1000), cutoff))
        assertFalse(eligible(cutoff, cutoff))
        assertFalse(eligible(cutoff.plusNanos(1000), cutoff))
        val hashes = IntentHashes(mapOf("new" to ByteArray(32) { 9 }), "new")
        assertThrows(IllegalStateException::class.java) {
            hashes.hash(OperationIntent(UUID.randomUUID(), "a", "k", "p", emptyList()), "old")
        }
    }

    @Test
    fun `registration binds only normalized identity and ephemeral submission nonce`() {
        val id = UUID.fromString("404e89b2-dab6-4c38-a835-2ef4d54c336a")
        val nonce = UUID.fromString("26a3fbad-c8cb-48ae-a39c-eed6e7cb456a")
        val registration = RegistrationIntent.create(id, "anonymous:fixture", "p1", "synthetic@example.test", nonce)
        assertEquals(
            listOf("identity" to "synthetic@example.test", "submissionNonce" to nonce.toString()),
            registration.fields,
        )
        val hashes = IntentHashes(mapOf("v1" to ByteArray(32) { 23 }), "v1")
        val next = RegistrationIntent.create(id, "anonymous:fixture", "p1", "synthetic@example.test", UUID.randomUUID())
        assertFalse(hashes.hash(registration, "v1").contentEquals(hashes.hash(next, "v1")))
        assertFalse(hashes.hash(registration, "v1").contentEquals(hashes.hash(registration, null)))
    }
}
