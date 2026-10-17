package com.spoolin.catalog.features.proposal

import com.spoolin.support.identity.ActorClaims
import io.grpc.Status
import java.security.MessageDigest
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
    companion object {
        fun fromInput(
            claims: ActorClaims,
            proposalId: UUID,
            revision: Int,
            contentHash: ByteArray,
            challenge: String,
            policyRevision: String,
            now: Instant,
        ): ExactApprovalBinding =
            ExactApprovalBinding(
                proposalId = proposalId,
                revision = revision,
                actorId = claims.accountId,
                connectorId = claims.credentialId,
                clientKind = claims.clientKind ?: "",
                contentHash = contentHash,
                challenge = challenge,
                policyRevision = policyRevision,
                now = now,
            )
    }

    fun verify(
        row: ProposalRow,
        storedChallengeHash: ByteArray,
    ): Status? {
        if (row.id != proposalId || row.revision != revision) {
            return Status.FAILED_PRECONDITION.withDescription("REVISION_MISMATCH")
        }
        if (row.actorId != actorId || row.connectorId != connectorId || row.clientKind != clientKind) {
            return Status.PERMISSION_DENIED.withDescription("ACTOR_BINDING_MISMATCH")
        }
        if (!contentHash.contentEquals(row.contentHash)) {
            return Status.FAILED_PRECONDITION.withDescription("CONTENT_HASH_MISMATCH")
        }
        if (challenge.isBlank()) {
            return Status.FAILED_PRECONDITION.withDescription("CHALLENGE_REQUIRED")
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(challenge.toByteArray(Charsets.UTF_8))
        if (!digest.contentEquals(storedChallengeHash)) {
            return Status.FAILED_PRECONDITION.withDescription("CHALLENGE_MISMATCH")
        }
        val boundPolicy = ProposalContent.policyRevision(ProposalContent.parseJson(row.contentJson))
        if (policyRevision != boundPolicy) {
            return Status.FAILED_PRECONDITION.withDescription("POLICY_REVISION_MISMATCH")
        }
        if (!now.isBefore(row.expiresAt)) {
            return Status.FAILED_PRECONDITION.withDescription("PROPOSAL_EXPIRED")
        }
        if (row.state != "REVIEW" && row.state != "CREATED") {
            return Status.FAILED_PRECONDITION.withDescription("PROPOSAL_NOT_REVIEWABLE")
        }
        return null
    }
}
