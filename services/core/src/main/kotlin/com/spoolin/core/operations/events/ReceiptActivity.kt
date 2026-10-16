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
    ) {
        val sql =
            "INSERT INTO operational_event " +
                "(id,operation_id,actor_kind,action,subject_kind,subject_id,outcome,correlation_id,event_type) " +
                "VALUES (?,?,'SERVICE','OPERATION_COMPLETED','OPERATION',?,'SUCCEEDED',?,'ACTIVITY')"
        check(connection.executeBound(sql, UUID.randomUUID(), operationId, subjectId, correlationId) == 1)
    }
}
