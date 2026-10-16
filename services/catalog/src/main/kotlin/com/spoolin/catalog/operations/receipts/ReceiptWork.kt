package com.spoolin.catalog.operations.receipts

import com.spoolin.support.receipts.WorkRequest
import com.spoolin.support.receipts.executeBound
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
        val inserted =
            connection.executeBound(
                sql,
                work.id(operation),
                operation,
                work.kind,
                "{\"subjectId\":\"${work.subjectId}\"}",
                work.maxAttempts,
                Timestamp.from(work.deadline),
                correlation,
                work.baseDelayMs,
                work.capDelayMs,
                work.multiplier,
            )
        check(inserted == 1)
    }
}
