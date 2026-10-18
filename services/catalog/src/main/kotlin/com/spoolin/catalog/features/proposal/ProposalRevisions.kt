package com.spoolin.catalog.features.proposal

import com.spoolin.support.identity.ActorClaims
import com.spoolin.support.receipts.SafeResult
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import io.grpc.Status
import spoolin.v1.Spoolin
import java.security.MessageDigest
import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

private const val MAX_PENDING = 20
private const val PROPOSAL_SECONDS = 600L

internal object ProposalRevisions {
    fun insert(conn: Connection, request: Spoolin.Proposal, claims: ActorClaims, challenge: String, now: Instant): SafeResult {
        val previous = previous(conn, request, claims, now)
        val id = previous?.id ?: UUID.randomUUID()
        val revision = previous?.revision?.plus(1) ?: 1
        previous?.let { supersede(conn, it) }
        enforceQuota(conn, claims.accountId, now)
        val content = ProposalContent.fromProposal(request)
        conn.executeBound(
            "INSERT INTO movie_proposal " +
                "(id,revision,actor_id,connector_id,client_kind,content,content_hash," +
                "challenge_hash,state,operation_id,expires_at) " +
                "VALUES (?,?,?,?,?,?::jsonb,?,?,'REVIEW',?,?)",
            id, revision, claims.accountId, claims.credentialId, claims.clientKind,
            DeterministicContentSerializer.json(content), DeterministicContentSerializer.digest(content),
            MessageDigest.getInstance("SHA-256").digest(challenge.toByteArray(Charsets.UTF_8)),
            previous?.operationId ?: UUID.fromString(request.context.operationId),
            Timestamp.from(now.plusSeconds(PROPOSAL_SECONDS).truncatedTo(ChronoUnit.MICROS)),
        )
        return SafeResult(id, revision.toLong())
    }

    private fun previous(conn: Connection, request: Spoolin.Proposal, claims: ActorClaims, now: Instant): ProposalRow? {
        require(request.hasProposalId() == request.hasExpectedRevision()) { "Revision target required" }
        if (!request.hasProposalId()) return null
        require(request.expectedRevision in 1 until Int.MAX_VALUE) { "Revision invalid" }
        val id = UUID.fromString(request.proposalId).also { require(it.toString() == request.proposalId) }
        val row = ProposalStore.latestForUpdate(conn, id)
            ?: throw Status.NOT_FOUND.withDescription("PROPOSAL_NOT_FOUND").asRuntimeException()
        assertOwner(row, claims)
        if (row.revision != request.expectedRevision) {
            throw Status.ABORTED.withDescription("REVISION_MISMATCH").asRuntimeException()
        }
        requireReview(row, now)
        return row
    }

    fun assertOwner(row: ProposalRow, claims: ActorClaims) {
        if (row.actorId != claims.accountId || row.connectorId != claims.credentialId || row.clientKind != claims.clientKind) {
            throw Status.PERMISSION_DENIED.withDescription("ACTOR_BINDING_MISMATCH").asRuntimeException()
        }
    }

    fun requireReview(row: ProposalRow, now: Instant) {
        if (!now.isBefore(row.expiresAt)) {
            throw Status.FAILED_PRECONDITION.withDescription("PROPOSAL_EXPIRED").asRuntimeException()
        }
        if (row.state != "REVIEW") {
            throw Status.FAILED_PRECONDITION.withDescription("PROPOSAL_NOT_REVIEWABLE").asRuntimeException()
        }
    }

    private fun supersede(conn: Connection, row: ProposalRow) {
        val count = conn.executeBound(
            "UPDATE movie_proposal SET state='SUPERSEDED' WHERE id=? AND revision=? AND state='REVIEW'",
            row.id, row.revision,
        )
        check(count == 1) { "Proposal revision changed" }
    }

    private fun enforceQuota(conn: Connection, actor: UUID, now: Instant) {
        val count = conn.queryBound(
            "SELECT count(*) FROM movie_proposal WHERE actor_id=? AND state='REVIEW' AND expires_at>?",
            listOf(actor, Timestamp.from(now)),
        ) { it.getInt(1) }.single()
        if (count >= MAX_PENDING) throw Status.RESOURCE_EXHAUSTED.asRuntimeException()
    }
}
