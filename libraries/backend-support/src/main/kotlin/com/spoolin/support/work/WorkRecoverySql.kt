package com.spoolin.support.work

import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

private const val RECOVERY_DEADLINE_SECONDS = 60L

/** Locked recovery transition and retained predecessor evidence share the caller's transaction. */
internal object WorkRecoverySql {
    fun recoverLocked(
        connection: Connection,
        workId: UUID,
        commit: WorkRecoveryCommit,
        now: Instant,
        notification: Boolean,
    ): Boolean {
        val kind = if (notification) "kind='NOTIFICATION' AND notification_state='FAILED'" else "kind<>'NOTIFICATION'"
        require(!connection.autoCommit) { "Work recovery requires an explicit transaction" }
        val recoveryDeadlineAt = now.plusSeconds(RECOVERY_DEADLINE_SECONDS)
        WorkRecoveryRequest(workId, commit.operatorId, commit.reason, commit.correlationId)
        val previous =
            connection
                .queryBound(
                    "SELECT generation,lease_epoch,operation_id,deadline_at FROM durable_work " +
                        "WHERE id=? AND state='EXHAUSTED' AND $kind FOR UPDATE",
                    listOf(workId),
                ) { row ->
                    RecoveryRow(
                        row.getInt("generation"),
                        row.getLong("lease_epoch"),
                        row.getObject("operation_id", UUID::class.java),
                        row.getTimestamp("deadline_at").toInstant(),
                    )
                }.singleOrNull() ?: return false
        val updated =
            connection.executeBound(
                "UPDATE durable_work SET state='READY', generation=generation+1, attempt_count=0," +
                    "lease_owner=NULL,lease_until=NULL,last_error=NULL,next_attempt_at=?,deadline_at=?,updated_at=?," +
                    "notification_state=CASE WHEN kind='NOTIFICATION' THEN 'PENDING' ELSE notification_state END " +
                    "WHERE id=? AND state='EXHAUSTED' AND $kind AND generation=? AND lease_epoch=? " +
                    "AND ? > clock_timestamp()",
                Timestamp.from(now),
                Timestamp.from(recoveryDeadlineAt),
                Timestamp.from(now),
                workId,
                previous.generation,
                previous.epoch,
                Timestamp.from(recoveryDeadlineAt),
            )
        return if (updated == 0) {
            false
        } else {
            insertRecoveryEvent(connection, workId, commit, previous, recoveryDeadlineAt)
            true
        }
    }

    private data class RecoveryRow(
        val generation: Int,
        val epoch: Long,
        val operationId: UUID?,
        val deadlineAt: Instant,
    )

    private fun insertRecoveryEvent(
        connection: Connection,
        workId: UUID,
        commit: WorkRecoveryCommit,
        previous: RecoveryRow,
        deadlineAt: Instant,
    ) {
        connection.executeBound(
            """INSERT INTO operational_event
               (id, operation_id, actor_kind, action, subject_kind, subject_id,
                outcome, correlation_id, event_type, work_id, generation,
                lease_epoch, operator_id, changes)
               VALUES (?, ?, 'SERVICE', 'WORK_RECOVERY', 'WORK', ?, 'SUCCEEDED',
                       ?, 'RECOVERY', ?, ?, ?, ?, ?::jsonb)""",
            commit.eventId,
            previous.operationId,
            workId,
            commit.correlationId,
            workId,
            previous.generation + 1,
            previous.epoch,
            commit.operatorId,
            """{"reason":${escapeJson(
                commit.reason,
            )},"previousDeadlineAt":"${previous.deadlineAt}","deadlineAt":"$deadlineAt"}""",
        )
    }

    private fun escapeJson(value: String): String =
        tools.jackson.databind.json.JsonMapper
            .builder()
            .build()
            .writeValueAsString(value)
}
