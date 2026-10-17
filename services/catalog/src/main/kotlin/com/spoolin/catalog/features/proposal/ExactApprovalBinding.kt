package com.spoolin.catalog.features.proposal

import java.time.Instant
import java.util.UUID

data class ExactApprovalBinding(
    val proposalId: UUID,
    val revision: Int,
    val actorId: UUID,
    val connectorId: UUID,
    val clientKind: String,
    val contentHash: ByteArray,
    val challenge: String,
    val policyRevision: String,
    val now: Instant,
) {
    fun eligible(contentHash: ByteArray, expiresAt: Instant): Boolean {
        if (!this.contentHash.contentEquals(contentHash)) return false
        return now.isBefore(expiresAt)
    }
}
