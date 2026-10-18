package com.spoolin.catalog.features.lifecycle

import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class ImpactTokenRulesTest {
    private val now = Instant.parse("2026-10-18T00:00:00Z")
    private val key = ByteArray(32) { 71 }

    private fun signer(at: Instant = now) = ImpactToken("test", key, Clock.fixed(at, ZoneOffset.UTC))

    private fun snapshot(
        credits: List<ImpactCredit> = emptyList(),
        version: Long = 3,
        impact: Long = 7,
    ) = ImpactSnapshot.fromCredits(UUID(0, 1), version, impact, "ARCHIVED", credits)

    @Test
    fun `signed token binds person versions complete tuples and exact five minute expiry`() {
        val credit = ImpactCredit(UUID(0, 10), 1, UUID(0, 20), 4, "Title", "MOVIE", "ACTIVE")
        val original = snapshot(listOf(credit))
        val token = signer().issue(original)
        assertEquals(now.plusSeconds(300), token.expires)
        assertTrue(signer(now.plusSeconds(299)).matches(token.token, original))
        assertFalse(signer(now.plusSeconds(300)).matches(token.token, original))
        assertFalse(signer().matches(token.token, snapshot(listOf(credit.copy(version = 2)))))
        assertFalse(signer().matches(token.token, snapshot(listOf(credit.copy(titleVersion = 5)))))
        assertFalse(signer().matches(token.token, snapshot(listOf(credit.copy(title = UUID(0, 21))))))
        assertFalse(signer().matches(token.token, snapshot(listOf(credit), version = 4)))
        assertFalse(signer().matches(token.token, snapshot(listOf(credit), impact = 8)))
        assertFalse(signer().matches(token.token.dropLast(1) + "!", original))
        assertFalse(ImpactToken("test", ByteArray(32) { 72 }, Clock.fixed(now, ZoneOffset.UTC)).matches(token.token, original))
    }

    @Test
    fun `full credit count is distinct from title count and tuple ordering is deterministic`() {
        val credits = (1..25).map { ImpactCredit(UUID(0, it.toLong()), 1, UUID(0, 30), 1, "Same title", "MOVIE", "ACTIVE") }
        val original = snapshot(credits)
        assertEquals(25L, original.creditCount)
        assertEquals(1, original.affectedTitles.size)
        assertTrue(signer().matches(signer().issue(original).token, snapshot(credits.reversed())))
        assertFalse(signer().matches(signer().issue(original).token, snapshot(credits.dropLast(1))))
    }

    @Test
    fun `GraphQL Int count admits maximum and rejects overflow without wrapping`() {
        assertEquals(Int.MAX_VALUE, ImpactSnapshot.exactCount(2147483647))
        assertEquals(
            Status.Code.RESOURCE_EXHAUSTED,
            assertThrows(StatusRuntimeException::class.java) {
                ImpactSnapshot.exactCount(2147483648)
            }.status.code,
        )
    }
}
