package com.spoolin.catalog.operations.events

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
        connection.prepareStatement(sql).use { statement ->
            statement.setObject(1, UUID.randomUUID())
            statement.setObject(2, operationId)
            statement.setObject(3, subjectId)
            statement.setObject(4, correlationId)
            check(statement.executeUpdate() == 1)
        }
    }
}
