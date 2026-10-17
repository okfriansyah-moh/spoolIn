package com.spoolin.catalog.operations.audit

import com.spoolin.support.receipts.executeBound
import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/**
 * Inserts an immutable business audit record inside the caller's transaction.
 *
 * ATDD-224: The record is commit-atomic with the domain change that it describes.
 * AC-053: A success claim cannot stand without its associated audit evidence.
 * AC-056: Credentials, tokens and emails never appear in the changes column.
 */
object AuditInsert {

    @Suppress("LongParameterList")
    fun record(
        connection: Connection,
        operationId: UUID?,
        actorId: UUID?,
        actorKind: String,
        action: String,
        subjectKind: String,
        subjectId: UUID?,
        outcome: String,
        changes: String = "{}",
        correlationId: UUID,
        createdAt: Instant,
        id: UUID = UUID.randomUUID(),
    ) {
        val sql =
            "INSERT INTO catalog_audit_event " +
                "(id,operation_id,actor_id,actor_kind,action,subject_kind,subject_id," +
                "outcome,changes,correlation_id,created_at) " +
                "VALUES (?,?,?,?,?,?,?,?,?::jsonb,?,?)"
        check(
            connection.executeBound(
                sql, id, operationId, actorId, actorKind, action, subjectKind, subjectId,
                outcome, changes, correlationId, Timestamp.from(createdAt),
            ) == 1,
        ) { "Audit record insert failed unexpectedly" }
    }
}
