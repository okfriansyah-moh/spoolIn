package com.spoolin.catalog.operations.receipts

import com.spoolin.support.receipts.WorkRequest
import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

object ReceiptWork {
    fun enqueue(
        connection: Connection,
        operation: UUID,
        correlation: UUID,
        work: WorkRequest,
        now: Instant,
    ) {
        require(work.deadline.isAfter(now)) { "Work deadline is unavailable" }
        val sql =
            "INSERT INTO durable_work (id,operation_id,kind,payload,max_attempts,deadline_at," +
                "correlation_id,base_delay_ms,cap_delay_ms,backoff_multiplier) VALUES (?,?,?,?::jsonb,?,?,?,?,?,?)"
        connection.prepareStatement(sql).use { statement ->
            statement.setObject(1, work.id(operation))
            statement.setObject(2, operation)
            statement.setString(3, work.kind)
            statement.setString(4, "{\"subjectId\":\"${work.subjectId}\"}")
            statement.setInt(5, work.maxAttempts)
            statement.setTimestamp(6, Timestamp.from(work.deadline))
            statement.setObject(7, correlation)
            statement.setInt(8, work.baseDelayMs)
            statement.setInt(9, work.capDelayMs)
            statement.setInt(10, work.multiplier)
            check(statement.executeUpdate() == 1)
        }
    }
}
