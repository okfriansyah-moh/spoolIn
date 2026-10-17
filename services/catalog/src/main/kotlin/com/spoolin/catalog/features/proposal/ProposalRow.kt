package com.spoolin.catalog.features.proposal

import java.time.Instant
import java.util.UUID

data class ProposalRow(
    val id: UUID,
    val revision: Int,
    val actorId: UUID,
    val connectorId: UUID,
    val clientKind: String,
    val contentJson: String,
    val contentHash: ByteArray,
    val challengeHash: ByteArray,
    val state: String,
    val operationId: UUID,
    val movieId: UUID?,
    val expiresAt: Instant,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ProposalRow
        return id == other.id &&
            revision == other.revision &&
            contentHash.contentEquals(other.contentHash) &&
            challengeHash.contentEquals(other.challengeHash)
    }

    override fun hashCode(): Int = id.hashCode() * 31 + revision
}
