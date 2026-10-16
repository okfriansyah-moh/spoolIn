package com.spoolin.core.operations.events

import com.spoolin.support.receipts.executeBound
import java.sql.Connection
import java.util.UUID

object ReceiptActivity {
    fun append(
        connection: Connection,
        operationId: UUID,
        correlationId: UUID,
        subjectId: UUID,
        audit: ReceiptAudit = ReceiptAudit(),
    ) {
        val sql =
            "INSERT INTO operational_event " +
                "(id,operation_id,actor_kind,actor_id,action,subject_kind,subject_id," +
                "outcome,correlation_id,event_type) " +
                "VALUES (?,?,?,?,?,?,?,'SUCCEEDED',?,'ACTIVITY')"
        check(
            connection.executeBound(
                sql,
                UUID.randomUUID(),
                operationId,
                if (audit.actorId == null) "SERVICE" else "HUMAN",
                audit.actorId,
                audit.action.name,
                audit.action.subject,
                audit.subjectId ?: subjectId,
                correlationId,
            ) == 1,
        )
    }
}
