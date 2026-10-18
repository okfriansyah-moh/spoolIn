package com.spoolin.catalog.features.proposal

import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import io.grpc.Status
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

object ProposalStore {
    fun loadForUpdate(
        conn: Connection,
        proposalId: UUID,
        revision: Int,
    ): ProposalRow? =
        conn
            .queryBound(
                "SELECT id, revision, actor_id, connector_id, client_kind, content::text AS content_json, " +
                    "content_hash, challenge_hash, state, operation_id, movie_id, expires_at " +
                    "FROM movie_proposal WHERE id = ? AND revision = ? FOR UPDATE",
                listOf(proposalId, revision),
                ::mapRow,
            ).singleOrNull()

    fun latestForUpdate(conn: Connection, proposalId: UUID): ProposalRow? = conn.queryBound(
        "SELECT id,revision,actor_id,connector_id,client_kind,content::text AS content_json," +
            "content_hash,challenge_hash,state,operation_id,movie_id,expires_at " +
            "FROM movie_proposal WHERE id=? ORDER BY revision DESC LIMIT 1 FOR UPDATE",
        listOf(proposalId), ::mapRow,
    ).singleOrNull()

    fun requireForUpdate(conn: Connection, proposalId: UUID, revision: Int): ProposalRow =
        loadForUpdate(conn, proposalId, revision)
            ?: throw Status.NOT_FOUND.withDescription("PROPOSAL_NOT_FOUND").asRuntimeException()

    private fun mapRow(row: ResultSet): ProposalRow =
        ProposalRow(
            id = row.getObject("id", UUID::class.java),
            revision = row.getInt("revision"),
            actorId = row.getObject("actor_id", UUID::class.java),
            connectorId = row.getObject("connector_id", UUID::class.java),
            clientKind = row.getString("client_kind"),
            contentJson = row.getString("content_json"),
            contentHash = row.getBytes("content_hash"),
            challengeHash = row.getBytes("challenge_hash"),
            state = row.getString("state"),
            operationId = row.getObject("operation_id", UUID::class.java),
            movieId = row.getObject("movie_id", UUID::class.java),
            expiresAt = row.getTimestamp("expires_at").toInstant(),
        )

    fun markCreated(
        conn: Connection,
        row: ProposalRow,
        movieId: UUID,
        approvedAt: Instant,
        approvalExpiresAt: Instant,
    ) {
        val updated =
            conn.executeBound(
                "UPDATE movie_proposal SET state = 'CREATED', movie_id = ?, approval_actor_id = ?, " +
                    "approval_connector_id = ?, approval_content_hash = ?, approved_at = ?, " +
                    "approval_expires_at = ?, approval_consumed_at = ? " +
                    "WHERE id = ? AND revision = ? AND state = 'REVIEW'",
                movieId,
                row.actorId,
                row.connectorId,
                row.contentHash,
                Timestamp.from(approvedAt),
                Timestamp.from(approvalExpiresAt),
                Timestamp.from(approvedAt),
                row.id,
                row.revision,
            )
        if (updated != 1) throw Status.ABORTED.withDescription("PROPOSAL_RACE").asRuntimeException()
    }

    fun markRejected(
        conn: Connection,
        proposalId: UUID,
        revision: Int,
    ) {
        val updated =
            conn.executeBound(
                "UPDATE movie_proposal SET state = 'REJECTED' WHERE id = ? AND revision = ? AND state = 'REVIEW'",
                proposalId,
                revision,
            )
        if (updated != 1) {
            throw Status.FAILED_PRECONDITION.withDescription("PROPOSAL_NOT_REVIEWABLE").asRuntimeException()
        }
    }
}
