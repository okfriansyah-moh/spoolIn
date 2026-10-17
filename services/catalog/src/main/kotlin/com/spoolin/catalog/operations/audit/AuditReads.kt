package com.spoolin.catalog.operations.audit

import com.spoolin.support.receipts.queryBound
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

private const val MAX_PAGE = 100

/**
 * Reads catalog business audit events for the Maintainer history view.
 *
 * Ordering: created_at DESC, id ASC (stable keyset).
 * F-PERMISSIONS: caller must hold MAINTAINER role; enforced at the gRPC boundary.
 * AC-056: Only permitted readers see this data; the projection never exposes credentials.
 */
class AuditReads(private val dataSource: DataSource) {

    data class AuditRow(
        val id: UUID,
        val actorId: UUID?,
        val actorKind: String,
        val action: String,
        val subjectKind: String,
        val subjectId: UUID?,
        val outcome: String,
        val changes: String,
        val operationId: UUID?,
        val correlationId: UUID,
        val createdAt: Instant,
    )

    data class AuditPage(
        val rows: List<AuditRow>,
        val keyset: Keyset?,
        val hasNextPage: Boolean,
    )

    data class Keyset(val createdAt: Instant, val id: UUID)

    fun list(subjectId: UUID?, first: Int, after: Keyset?): AuditPage {
        val limit = first.coerceIn(1, MAX_PAGE)
        val params = mutableListOf<Any?>()
        val where = mutableListOf<String>()

        if (subjectId != null) {
            where.add("subject_id = ?")
            params.add(subjectId)
        }
        if (after != null) {
            // Keyset: (created_at DESC, id ASC) — rows on pages after the cursor.
            where.add("(created_at < ? OR (created_at = ? AND id > ?))")
            params.add(Timestamp.from(after.createdAt))
            params.add(Timestamp.from(after.createdAt))
            params.add(after.id)
        }

        val clause = if (where.isEmpty()) "" else "WHERE ${where.joinToString(" AND ")}"
        val sql =
            "SELECT id, actor_id, actor_kind, action, subject_kind, subject_id, " +
                "outcome, changes::text AS changes, operation_id, correlation_id, created_at " +
                "FROM catalog_audit_event $clause " +
                "ORDER BY created_at DESC, id ASC LIMIT ${limit + 1}"

        val rows = dataSource.connection.use { conn -> conn.queryBound(sql, params) { mapRow(it) } }
        val hasNext = rows.size > limit
        val page = if (hasNext) rows.dropLast(1) else rows
        val keyset = page.lastOrNull()?.let { Keyset(it.createdAt, it.id) }
        return AuditPage(page, keyset, hasNext)
    }

    private fun mapRow(rs: ResultSet): AuditRow = AuditRow(
        id = rs.getObject("id", UUID::class.java),
        actorId = rs.getObject("actor_id")?.let { UUID.fromString(it.toString()) },
        actorKind = rs.getString("actor_kind"),
        action = rs.getString("action"),
        subjectKind = rs.getString("subject_kind"),
        subjectId = rs.getObject("subject_id")?.let { UUID.fromString(it.toString()) },
        outcome = rs.getString("outcome"),
        changes = rs.getString("changes") ?: "{}",
        operationId = rs.getObject("operation_id")?.let { UUID.fromString(it.toString()) },
        correlationId = rs.getObject("correlation_id", UUID::class.java),
        createdAt = rs.getTimestamp("created_at").toInstant(),
    )
}
