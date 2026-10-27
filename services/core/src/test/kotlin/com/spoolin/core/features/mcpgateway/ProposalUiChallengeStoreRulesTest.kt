package com.spoolin.core.features.mcpgateway

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class ProposalUiChallengeStoreRulesTest {
    private val proposal = "a8cfa155-c207-4af2-95bd-8cdf5429ee25"
    private val hash = "synthetic-content-hash"
    private val challenge = "synthetic-private-challenge"

    @Test
    fun `exact expiry denies both lookup paths and clock rewind cannot revive evicted challenge`() {
        val clock = MutableClock(Instant.ofEpochSecond(1000))
        val store = ProposalUiChallengeStore(clock, Duration.ofMinutes(10))
        val binding = store.put(proposal, 1, hash, challenge)
        clock.current = Instant.ofEpochSecond(1599)
        assertEquals(challenge, store.getChallenge(proposal, 1, hash, binding))
        assertTrue(store.matchesBoundChallenge(proposal, 1, hash, challenge))
        clock.current = Instant.ofEpochSecond(1600)
        assertNull(store.getChallenge(proposal, 1, hash, binding))
        assertFalse(store.matchesBoundChallenge(proposal, 1, hash, challenge))
        clock.current = Instant.ofEpochSecond(1599)
        assertNull(store.getChallenge(proposal, 1, hash, binding))
        assertFalse(store.matchesBoundChallenge(proposal, 1, hash, challenge))
    }

    @Test
    fun `challenge matching expires at equality before any binding lookup`() {
        val clock = MutableClock(Instant.ofEpochSecond(1000))
        val store = ProposalUiChallengeStore(clock, Duration.ofMinutes(10))
        store.put(proposal, 1, hash, challenge)
        clock.current = Instant.ofEpochSecond(1600)
        assertFalse(store.matchesBoundChallenge(proposal, 1, hash, challenge))
    }

    @Test
    fun `proposal revision hash token and challenge stay bound without consuming valid entry`() {
        val store = ProposalUiChallengeStore(Clock.fixed(Instant.ofEpochSecond(1000), ZoneOffset.UTC))
        val binding = store.put(proposal, 1, hash, challenge)
        assertNull(store.getChallenge("other-proposal", 1, hash, binding))
        assertNull(store.getChallenge(proposal, 2, hash, binding))
        assertNull(store.getChallenge(proposal, 1, "other-hash", binding))
        assertNull(store.getChallenge(proposal, 1, hash, "other-token"))
        assertNull(store.getChallenge(proposal, 1, hash, ""))
        assertFalse(store.matchesBoundChallenge(proposal, 1, "other-hash", challenge))
        assertFalse(store.matchesBoundChallenge(proposal, 1, hash, "other-challenge"))
        assertFalse(store.matchesBoundChallenge(proposal, 1, hash, ""))
        assertEquals(challenge, store.getChallenge(proposal, 1, hash, binding))
        assertTrue(store.matchesBoundChallenge(proposal, 1, hash, challenge))
    }

    @Test
    fun `replacing a revision invalidates previous binding and challenge`() {
        val store = ProposalUiChallengeStore(Clock.fixed(Instant.ofEpochSecond(1000), ZoneOffset.UTC))
        val oldBinding = store.put(proposal, 1, hash, challenge)
        val newBinding = store.put(proposal, 1, "updated-hash", "updated-challenge")
        assertNull(store.getChallenge(proposal, 1, hash, oldBinding))
        assertFalse(store.matchesBoundChallenge(proposal, 1, hash, challenge))
        assertEquals("updated-challenge", store.getChallenge(proposal, 1, "updated-hash", newBinding))
    }
}

private class MutableClock(
    var current: Instant,
) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = Clock.fixed(current, zone)

    override fun instant(): Instant = current
}
