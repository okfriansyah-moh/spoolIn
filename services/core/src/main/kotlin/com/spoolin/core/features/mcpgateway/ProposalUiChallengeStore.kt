package com.spoolin.core.features.mcpgateway

import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

/** UI-only approval challenges keyed by proposal binding (never model-visible). */
class ProposalUiChallengeStore(
    private val clock: Clock = Clock.systemUTC(),
    private val ttl: Duration = Duration.ofMinutes(10),
    private val random: SecureRandom = SecureRandom(),
) {
    private data class Entry(
        val contentHash: String,
        val challenge: String,
        val bindingToken: String,
        val expiresAt: Instant,
    )

    private val entries = ConcurrentHashMap<String, Entry>()

    fun put(
        proposalId: String,
        revision: Int,
        contentHash: String,
        challenge: String,
    ): String {
        if (challenge.isBlank()) return ""
        val bindingToken = randomBindingToken()
        entries[key(proposalId, revision)] =
            Entry(contentHash, challenge, bindingToken, clock.instant().plus(ttl))
        return bindingToken
    }

    fun getChallenge(
        proposalId: String,
        revision: Int,
        contentHash: String,
        bindingToken: String,
    ): String? {
        if (bindingToken.isBlank()) return null
        purgeExpired()
        val entry = entries[key(proposalId, revision)] ?: return null
        if (clock.instant().isAfter(entry.expiresAt)) {
            entries.remove(key(proposalId, revision))
            return null
        }
        if (entry.contentHash != contentHash) return null
        if (!constantTimeEquals(entry.bindingToken, bindingToken)) return null
        return entry.challenge
    }

    private fun purgeExpired() {
        val now = clock.instant()
        entries.entries.removeIf { now.isAfter(it.value.expiresAt) }
    }

    private fun randomBindingToken(): String {
        val bytes = ByteArray(32).also(random::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun constantTimeEquals(
        expected: String,
        actual: String,
    ): Boolean {
        val left = expected.toByteArray(Charsets.UTF_8)
        val right = actual.toByteArray(Charsets.UTF_8)
        if (left.size != right.size) {
            MessageDigest.isEqual(left, left)
            return false
        }
        return MessageDigest.isEqual(left, right)
    }

    private fun key(
        proposalId: String,
        revision: Int,
    ): String = "$proposalId:$revision"
}
