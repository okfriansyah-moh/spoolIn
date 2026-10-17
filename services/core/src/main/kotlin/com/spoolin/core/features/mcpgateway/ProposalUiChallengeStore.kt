package com.spoolin.core.features.mcpgateway

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/** UI-only approval challenges keyed by proposal binding (never model-visible). */
class ProposalUiChallengeStore(
    private val clock: Clock = Clock.systemUTC(),
    private val ttl: Duration = Duration.ofMinutes(10),
) {
    private data class Entry(
        val contentHash: String,
        val challenge: String,
        val expiresAt: Instant,
    )

    private val entries = ConcurrentHashMap<String, Entry>()

    fun put(
        proposalId: String,
        revision: Int,
        contentHash: String,
        challenge: String,
    ) {
        if (challenge.isBlank()) return
        entries[key(proposalId, revision)] =
            Entry(contentHash, challenge, clock.instant().plus(ttl))
    }

    fun get(
        proposalId: String,
        revision: Int,
        contentHash: String,
    ): String? {
        purgeExpired()
        val entry = entries[key(proposalId, revision)] ?: return null
        if (clock.instant().isAfter(entry.expiresAt)) {
            entries.remove(key(proposalId, revision))
            return null
        }
        if (entry.contentHash != contentHash) return null
        return entry.challenge
    }

    private fun purgeExpired() {
        val now = clock.instant()
        entries.entries.removeIf { now.isAfter(it.value.expiresAt) }
    }

    private fun key(
        proposalId: String,
        revision: Int,
    ): String = "$proposalId:$revision"
}
