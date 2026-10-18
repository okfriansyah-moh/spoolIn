package com.spoolin.support.notifications

import com.spoolin.support.receipts.committed
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import com.spoolin.support.work.AttemptOutcome
import com.spoolin.support.work.WorkAttemptCompletion
import com.spoolin.support.work.WorkClaim
import com.spoolin.support.work.WorkFence
import com.spoolin.support.work.WorkSchedule
import com.spoolin.support.work.WorkSql
import java.sql.Connection
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

data class NotificationClaim(
    val work: WorkClaim,
    val mode: String,
    val summary: String,
) {
    override fun toString(): String = "NotificationClaim(redacted)"
}

class NotificationStore(
    private val dataSource: DataSource,
) {
    fun claim(): NotificationClaim? =
        dataSource.committed { connection ->
            connection.queryBound("SELECT pg_advisory_xact_lock(720034)", emptyList()) { true }
            expire(connection)
            val active =
                connection
                    .queryBound(
                        "SELECT count(*) FROM durable_work WHERE kind='NOTIFICATION' AND state='LEASED'",
                        emptyList(),
                    ) { it.getInt(1) }
                    .single()
            if (active >= 2) return@committed null
            val now = databaseNow(connection)
            val selected =
                connection
                    .queryBound(
                        "SELECT id,operation_id,payload::text,attempt_count,max_attempts,deadline_at," +
                            "lease_epoch,correlation_id,base_delay_ms,cap_delay_ms,backoff_multiplier," +
                            "generation,notification_mode," +
                            "notification_summary::text FROM durable_work " +
                            "WHERE kind='NOTIFICATION' AND state='READY' " +
                            "AND notification_state='PENDING' AND next_attempt_at<=clock_timestamp() " +
                            "AND deadline_at>clock_timestamp() " +
                            "AND attempt_count<max_attempts ORDER BY next_attempt_at,id LIMIT 1 FOR UPDATE SKIP LOCKED",
                        emptyList(),
                    ) { row ->
                        val work =
                            WorkClaim(
                                row.getObject("id", UUID::class.java),
                                row.getObject("operation_id", UUID::class.java),
                                "NOTIFICATION",
                                row.getString("payload"),
                                row.getInt("attempt_count"),
                                row.getInt("max_attempts"),
                                row.getTimestamp("deadline_at").toInstant(),
                                UUID.randomUUID(),
                                row.getLong("lease_epoch"),
                                row.getObject("correlation_id", UUID::class.java),
                                row.getInt("base_delay_ms"),
                                row.getInt("cap_delay_ms"),
                                row.getInt("backoff_multiplier"),
                                row.getInt("generation"),
                            )
                        NotificationClaim(
                            work, row.getString("notification_mode"), row.getString("notification_summary"),
                        )
                    }.singleOrNull() ?: return@committed null
            WorkSql.commitClaim(connection, selected.work, UUID.randomUUID(), now)?.let { selected.copy(work = it) }
        }

    fun budgetMs(claim: WorkClaim): Long =
        dataSource.connection.use { connection ->
            val now = databaseNow(connection)
            val status = WorkSql.leaseStatus(connection, claim.workId)
            if (status == null || !WorkFence.isEligible(status, claim.leaseOwner, claim.leaseEpoch, now)) {
                0
            } else {
                java.time.Duration
                    .between(
                        now,
                        minOf(status.deadlineAt, status.leaseUntil!!),
                    ).toMillis()
                    .coerceAtMost(MAX_SEND_MS)
            }
        }

    fun finish(
        claim: WorkClaim,
        result: NotificationResult,
    ) = dataSource.committed { connection ->
        val now = databaseNow(connection)
        val next =
            now.plusMillis(
                maxOf(
                    result.retryAfterMs,
                    WorkSchedule.sampleDelayMs(
                        claim.baseDelayMs,
                        claim.capDelayMs,
                        claim.backoffMultiplier,
                        claim.attemptCount,
                        java.security.SecureRandom().nextDouble(),
                    ),
                ),
            )
        val retry =
            result.outcome == AttemptOutcome.TRANSIENT && claim.attemptCount < claim.maxAttempts &&
                next < claim.deadlineAt
        val completion = WorkAttemptCompletion(
            result.outcome, result.errorCode, workState(result, retry), if (retry) next else null,
        )
        if (!WorkSql.completeAttempt(connection, claim, completion, now)) return@committed false
        check(
            connection.executeBound(
                "UPDATE durable_work SET notification_state=?,provider_reference=? WHERE id=?",
                notificationState(result, retry),
                result.reference,
                claim.workId,
            ) == 1,
        )
        true
    }

    private fun workState(result: NotificationResult, retry: Boolean): String =
        when {
            result.outcome == AttemptOutcome.SUCCEEDED -> "DONE"
            retry -> "READY"
            else -> "EXHAUSTED"
        }

    private fun notificationState(result: NotificationResult, retry: Boolean): String =
        if (retry) "PENDING" else when (result.outcome) {
            AttemptOutcome.SUCCEEDED -> "ACCEPTED"
            AttemptOutcome.UNCERTAIN -> "UNKNOWN"
            else -> "FAILED"
        }

    private fun expire(connection: Connection) {
        // A crashed sender has no queryable provider receipt. Never reset its lease to READY.
        connection.executeBound(
            "WITH expired AS (UPDATE durable_work SET state='EXHAUSTED',notification_state='UNKNOWN'," +
                "lease_owner=NULL,lease_until=NULL,last_error='NOTIFICATION_UNCERTAIN',updated_at=clock_timestamp() " +
                "WHERE kind='NOTIFICATION' AND state='LEASED' " +
                "AND (lease_until<=clock_timestamp() OR deadline_at<=clock_timestamp()) " +
                "RETURNING *) INSERT INTO operational_event " +
                "(id,operation_id,actor_kind,action,subject_kind,subject_id,outcome," +
                "correlation_id,event_type,work_id,generation,attempt_no,lease_epoch,attempt_outcome,error_code) " +
                "SELECT gen_random_uuid(),operation_id,'SERVICE','WORK_ATTEMPT','WORK',id,'FAILED',correlation_id," +
                "'ATTEMPT_FINISHED',id,generation,attempt_count,lease_epoch,'UNCERTAIN'," +
                "'NOTIFICATION_UNCERTAIN' FROM expired " +
                "WHERE EXISTS (SELECT 1 FROM operational_event e WHERE e.work_id=expired.id " +
                "AND e.generation=expired.generation " +
                "AND e.attempt_no=expired.attempt_count AND e.lease_epoch=expired.lease_epoch " +
                "AND e.event_type='ATTEMPT_STARTED') " +
                "ON CONFLICT DO NOTHING",
        )
        connection.executeBound(
            "UPDATE durable_work SET state='EXHAUSTED',notification_state='FAILED'," +
                "last_error='NOTIFICATION_BUDGET_EXHAUSTED',updated_at=clock_timestamp() WHERE kind='NOTIFICATION' " +
                "AND state='READY' AND (deadline_at<=clock_timestamp() OR attempt_count>=max_attempts)",
        )
    }

    private companion object {
        const val MAX_SEND_MS = 14000L
    }

    private fun databaseNow(connection: Connection): Instant =
        connection.queryBound("SELECT clock_timestamp()", emptyList()) { it.getTimestamp(1).toInstant() }.single()
}
