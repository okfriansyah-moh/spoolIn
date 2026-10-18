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
            input: spoolin.v1.Spoolin.ApprovalInput,
            policyRevision: String,
            now: Instant,
        ): ExactApprovalBinding = ExactApprovalBinding(
            UUID.fromString(input.proposalId), input.revision, claims.accountId, claims.credentialId,
            claims.clientKind ?: "", java.util.Base64.getDecoder().decode(input.contentHash), input.challenge,
            policyRevision, now,
        )
    }

    fun verify(row: ProposalRow, storedChallengeHash: ByteArray): Status? =
        identityProblem(row) ?: challengeProblem(storedChallengeHash) ?: reviewProblem(row)

    fun identityProblem(row: ProposalRow): Status? = when {
        row.id != proposalId || row.revision != revision ->
            Status.FAILED_PRECONDITION.withDescription("REVISION_MISMATCH")
        row.actorId != actorId || row.connectorId != connectorId || row.clientKind != clientKind ->
            Status.PERMISSION_DENIED.withDescription("ACTOR_BINDING_MISMATCH")
        !MessageDigest.isEqual(contentHash, row.contentHash) ->
            Status.FAILED_PRECONDITION.withDescription("CONTENT_HASH_MISMATCH")
        else -> null
    }

    fun challengeProblem(storedChallengeHash: ByteArray): Status? {
        val digest = MessageDigest.getInstance("SHA-256").digest(challenge.toByteArray(Charsets.UTF_8))
        return when {
            challenge.isBlank() -> Status.FAILED_PRECONDITION.withDescription("CHALLENGE_REQUIRED")
            !MessageDigest.isEqual(digest, storedChallengeHash) ->
                Status.FAILED_PRECONDITION.withDescription("CHALLENGE_MISMATCH")
            else -> null
        }
    }

    fun reviewProblem(row: ProposalRow): Status? {
        val boundPolicy = ProposalContent.policyRevision(ProposalContent.parseJson(row.contentJson))
        return when {
            policyRevision != boundPolicy -> Status.FAILED_PRECONDITION.withDescription("POLICY_REVISION_MISMATCH")
            !now.isBefore(row.expiresAt) -> Status.FAILED_PRECONDITION.withDescription("PROPOSAL_EXPIRED")
            row.state !in setOf("REVIEW", "CREATED") ->
                Status.FAILED_PRECONDITION.withDescription("PROPOSAL_NOT_REVIEWABLE")
            else -> null
        }
    }
}
