package com.spoolin.core.features.mcpgateway

import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

private const val PROPOSAL_TTL_MINUTES = 10L
private const val BINDING_TOKEN_BYTES = 32

/** UI-only approval challenges keyed by proposal binding (never model-visible). */
class ProposalUiChallengeStore(
    private val clock: Clock = Clock.systemUTC(),
    private val ttl: Duration = Duration.ofMinutes(PROPOSAL_TTL_MINUTES),
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

    fun matchesBoundChallenge(
        proposalId: String,
        revision: Int,
        contentHash: String,
        challenge: String,
    ): Boolean =
        if (challenge.isBlank()) {
            false
        } else {
            val entry = liveEntry(proposalId, revision)
            entry != null && entry.contentHash == contentHash && constantTimeEquals(entry.challenge, challenge)
        }

    fun getChallenge(
        proposalId: String,
        revision: Int,
        contentHash: String,
        bindingToken: String,
    ): String? =
        if (bindingToken.isBlank()) {
            null
        } else {
            liveEntry(proposalId, revision)
                ?.takeIf { it.contentHash == contentHash && constantTimeEquals(it.bindingToken, bindingToken) }
                ?.challenge
        }

    private fun liveEntry(
        proposalId: String,
        revision: Int,
    ): Entry? {
        purgeExpired()
        val binding = key(proposalId, revision)
        val entry = entries[binding] ?: return null
        return if (clock.instant().isBefore(entry.expiresAt)) {
            entry
        } else {
            entries.remove(binding, entry)
            null
        }
    }

    private fun purgeExpired() {
        val now = clock.instant()
        entries.entries.removeIf { !now.isBefore(it.value.expiresAt) }
    }

    private fun randomBindingToken(): String {
        val bytes = ByteArray(BINDING_TOKEN_BYTES).also(random::nextBytes)
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
