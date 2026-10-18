package com.spoolin.support.work

import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

private const val LEASE_DURATION_SECONDS = 30L
private const val HEARTBEAT_INTERVAL_SECONDS = 10L
private const val MAX_EFFECT_SECONDS = 15L
private const val MAX_BATCH = 10
private const val RECOVERY_DEADLINE_SECONDS = 60L

/**
 * Shared SQL operations for the durable_work / operational_event tables.
 *
 * All three services share an identical schema; this object contains the reusable
 * claim/renew/complete/recover primitives. Service-specific wrappers apply their
 * own business context (schema path is resolved via the pool's search_path).
 *
 * Claim protocol (must remain in one transaction):
 *   1. SELECT FOR UPDATE SKIP LOCKED — atomically selects unclaimed READY rows.
 *   2. UPDATE durable_work — set LEASED, increment lease_epoch and attempt_count.
 *   3. INSERT operational_event ATTEMPT_STARTED — durable evidence before any effect.
 *   Commit all three before releasing the DB connection.
 *
 * Lease constants (F-LEASE):
 *   lease_duration = 30 s
 *   heartbeat_interval = 10 s
 *   max_effect_duration = 15 s
 */
object WorkSql {
    val leaseDuration: Long get() = LEASE_DURATION_SECONDS
    val heartbeatInterval: Long get() = HEARTBEAT_INTERVAL_SECONDS
    val maxEffectDuration: Long get() = MAX_EFFECT_SECONDS

    /**
     * Select up to [batchSize] READY rows with SKIP LOCKED.
     * Must be called inside an open, non-autocommit transaction.
     * Returns raw claim snapshots; the caller must call [commitClaim] for each before committing.
     */
    fun selectForClaim(
        connection: Connection,
        now: Instant,
        batchSize: Int = MAX_BATCH,
    ): List<WorkClaim> {
        require(!connection.autoCommit) { "Work claims require an explicit transaction" }
        require(batchSize in 1..MAX_BATCH) { "Batch size must be between 1 and $MAX_BATCH" }
        val sql =
            """
            SELECT id, operation_id, kind, payload::text, attempt_count, max_attempts,
                   deadline_at, lease_epoch, correlation_id,
                   base_delay_ms, cap_delay_ms, backoff_multiplier, generation
            FROM durable_work
            WHERE state = 'READY' AND next_attempt_at <= ?
              AND deadline_at > GREATEST(?, clock_timestamp()) AND attempt_count < max_attempts
            ORDER BY next_attempt_at, id
            LIMIT ?
            FOR UPDATE SKIP LOCKED
            """.trimIndent()
        val ts = Timestamp.from(now)
        return connection.queryBound(sql, listOf(ts, ts, batchSize)) { rs ->
            WorkClaim(
                workId = rs.getObject("id", UUID::class.java),
                operationId = rs.getObject("operation_id", UUID::class.java),
                kind = rs.getString("kind"),
                payload = rs.getString("payload"),
                attemptCount = rs.getInt("attempt_count"),
                maxAttempts = rs.getInt("max_attempts"),
                deadlineAt = rs.getTimestamp("deadline_at").toInstant(),
                leaseOwner = UUID.randomUUID(), // placeholder — replaced by commitClaim
                leaseEpoch = rs.getLong("lease_epoch"),
                correlationId = rs.getObject("correlation_id", UUID::class.java),
                baseDelayMs = rs.getInt("base_delay_ms"),
                capDelayMs = rs.getInt("cap_delay_ms"),
                backoffMultiplier = rs.getInt("backoff_multiplier"),
                generation = rs.getInt("generation"),
            )
        }
    }

    /**
     * Atomically transitions a READY row to LEASED and inserts the ATTEMPT_STARTED event.
     * Returns the final WorkClaim with the assigned [workerId] and new lease_epoch; returns
     * null when the row was already claimed by another worker (race on SKIP LOCKED edge case).
     *
     * Must be called inside the same open transaction as [selectForClaim], before commit.
     */
    fun commitClaim(
        connection: Connection,
        candidate: WorkClaim,
        workerId: UUID,
        now: Instant,
        eventId: UUID = UUID.randomUUID(),
    ): WorkClaim? {
        lockForMutation(connection, candidate.workId)
        val leaseUntil = now.plusSeconds(LEASE_DURATION_SECONDS)
        val newEpoch = candidate.leaseEpoch + 1
        val newAttemptCount = candidate.attemptCount + 1
        val updated =
            connection.executeBound(
                """UPDATE durable_work
               SET state = 'LEASED',
                   lease_owner = ?,
                   lease_until = LEAST(GREATEST(?, clock_timestamp() + INTERVAL '30 seconds'), deadline_at),
                   lease_epoch = lease_epoch + 1,
                   attempt_count = attempt_count + 1,
                   updated_at = ?
               WHERE id = ? AND state = 'READY' AND lease_epoch = ? AND generation = ?
                 AND attempt_count = ? AND attempt_count < max_attempts
                 AND next_attempt_at <= ? AND deadline_at > GREATEST(?, clock_timestamp())""",
                workerId,
                Timestamp.from(leaseUntil),
                Timestamp.from(now),
                candidate.workId,
                candidate.leaseEpoch,
                candidate.generation,
                candidate.attemptCount,
                Timestamp.from(now),
                Timestamp.from(now),
            )
        if (updated == 0) return null
        val claim = candidate.copy(leaseOwner = workerId, leaseEpoch = newEpoch, attemptCount = newAttemptCount)
        insertAttemptStarted(connection, claim, eventId)
        return claim
    }

    private fun insertAttemptStarted(connection: Connection, claim: WorkClaim, eventId: UUID) {
        connection.executeBound(
            """INSERT INTO operational_event
               (id, operation_id, actor_kind, action, subject_kind, subject_id,
                correlation_id, event_type, work_id, generation, attempt_no, lease_epoch)
               VALUES (?, ?, 'SERVICE', 'WORK_ATTEMPT', 'WORK', ?, ?, 'ATTEMPT_STARTED', ?, ?, ?, ?)""",
            eventId, claim.operationId, claim.workId, claim.correlationId, claim.workId,
            claim.generation, claim.attemptCount, claim.leaseEpoch,
        )
    }

    /**
     * Heartbeat renewal — extends [leaseUntil] when the caller is still the live owner.
     * Atomically validates the owner, epoch, generation, attempt, live lease and deadline.
     * Returns true when the renewal succeeded, false when the lease was lost.
     */
    fun renewLease(
        connection: Connection,
        claim: WorkClaim,
        now: Instant,
    ): Boolean {
        lockForMutation(connection, claim.workId)
        val newUntil = now.plusSeconds(LEASE_DURATION_SECONDS)
        return connection.executeBound(
            """UPDATE durable_work
               SET lease_until = LEAST(GREATEST(?, lease_until, clock_timestamp() + INTERVAL '30 seconds'), deadline_at),
                   updated_at = GREATEST(?, clock_timestamp())
               WHERE id = ? AND state = 'LEASED' AND lease_owner = ? AND lease_epoch = ?
                 AND generation = ? AND attempt_count = ?
                 AND lease_until > GREATEST(?, clock_timestamp())
                 AND deadline_at > GREATEST(?, clock_timestamp())""",
            Timestamp.from(newUntil),
            Timestamp.from(now),
            claim.workId,
            claim.leaseOwner,
            claim.leaseEpoch,
            claim.generation,
            claim.attemptCount,
            Timestamp.from(now),
            Timestamp.from(now),
        ) == 1
    }

    /**
     * Fenced terminal or reschedule commit — writes ATTEMPT_FINISHED plus the new durable_work
     * state in a single transaction.  The database time and tuple checks ensure that an expired owner or a
     * replacement worker cannot overwrite the current generation.
     *
     * Returns true when this worker's commit was accepted, false when already superseded.
     *
     * @param nextAttemptAt  When to reschedule (TRANSIENT only); null for terminal outcomes.
     * @param nextState      Target state: 'DONE', 'READY', 'EXHAUSTED', or 'CANCELLED'.
     */
    fun completeAttempt(
        connection: Connection,
        claim: WorkClaim,
        completion: WorkAttemptCompletion,
        now: Instant,
    ): Boolean {
        lockForMutation(connection, claim.workId)
        val lease = leaseFieldsForCompletion(completion.nextState, claim, now)
        val nextAttemptTs =
            completion.nextAttemptAt?.let { Timestamp.from(it) } ?: Timestamp.from(now)
        val updated =
            connection.executeBound(
                """UPDATE durable_work
               SET state = ?, lease_owner = ?, lease_until = ?, last_error = ?,
                   next_attempt_at = ?, updated_at = ?
               WHERE id = ? AND state = 'LEASED' AND lease_owner = ? AND lease_epoch = ?
                 AND generation = ? AND attempt_count = ?
                 AND lease_until > GREATEST(?, clock_timestamp())
                 AND deadline_at > GREATEST(?, clock_timestamp())
                 AND EXISTS (SELECT 1 FROM operational_event e
                   WHERE e.work_id = durable_work.id AND e.event_type = 'ATTEMPT_STARTED'
                     AND e.generation = durable_work.generation AND e.attempt_no = durable_work.attempt_count
                     AND e.lease_epoch = durable_work.lease_epoch)""",
                completion.nextState,
                lease.owner,
                lease.until,
                completion.errorCode,
                nextAttemptTs,
                Timestamp.from(now),
                claim.workId,
                claim.leaseOwner,
                claim.leaseEpoch,
                claim.generation,
                claim.attemptCount,
                Timestamp.from(now),
                Timestamp.from(now),
            )
        if (updated == 0) return false
        insertAttemptFinished(connection, claim, completion)
        return true
    }

    private fun lockForMutation(connection: Connection, workId: UUID) {
        require(!connection.autoCommit) { "Work lease changes require an explicit transaction" }
        connection.queryBound("SELECT id FROM durable_work WHERE id=? FOR UPDATE", listOf(workId)) { true }
    }

    private data class LeaseFields(
        val owner: Any?,
        val until: Any?,
    )

    private fun leaseFieldsForCompletion(
        nextState: String,
        claim: WorkClaim,
        now: Instant,
    ): LeaseFields {
        if (nextState != "LEASED") {
            return LeaseFields(null, null)
        }
        return LeaseFields(
            claim.leaseOwner,
            Timestamp.from(now.plusSeconds(LEASE_DURATION_SECONDS)),
        )
    }

    private fun insertAttemptFinished(
        connection: Connection,
        claim: WorkClaim,
        completion: WorkAttemptCompletion,
    ) {
        val eventOutcome =
            if (completion.outcome == AttemptOutcome.SUCCEEDED) "SUCCEEDED" else "FAILED"
        connection.executeBound(
            """INSERT INTO operational_event
               (id, operation_id, actor_kind, action, subject_kind, subject_id,
                outcome, correlation_id, event_type, work_id, generation, attempt_no,
                lease_epoch, attempt_outcome, error_code)
               VALUES (?, ?, 'SERVICE', 'WORK_ATTEMPT', 'WORK', ?, ?, ?, 'ATTEMPT_FINISHED',
                       ?, ?, ?, ?, ?, ?)""",
            completion.eventId,
            claim.operationId,
            claim.workId,
            eventOutcome,
            claim.correlationId,
            claim.workId,
            claim.generation,
            claim.attemptCount,
            claim.leaseEpoch,
            completion.outcome.code,
            completion.errorCode,
        )
    }

    /**
     * Operator-initiated controlled recovery. Transitions an EXHAUSTED work item to READY
     * with an incremented generation, resets the attempt counter and bounded deadline, and appends a RECOVERY event
     * that preserves the original evidence and records the operator identity and reason.
     *
     * Returns true when recovery was committed, false when the work is not in EXHAUSTED state.
     */
    fun recoverWork(
        connection: Connection,
        workId: UUID,
        commit: WorkRecoveryCommit,
        now: Instant,
    ): Boolean {
        require(!connection.autoCommit) { "Work recovery requires an explicit transaction" }
        val recoveryDeadlineAt = now.plusSeconds(RECOVERY_DEADLINE_SECONDS)
        WorkRecoveryRequest(workId, commit.operatorId, commit.reason, commit.correlationId)
        val previous = connection.queryBound(
            "SELECT generation,lease_epoch,operation_id,deadline_at FROM durable_work " +
                "WHERE id=? AND state='EXHAUSTED' FOR UPDATE",
            listOf(workId),
        ) { row ->
            RecoveryRow(row.getInt("generation"), row.getLong("lease_epoch"),
                row.getObject("operation_id", UUID::class.java), row.getTimestamp("deadline_at").toInstant())
        }.singleOrNull() ?: return false
        val updated = connection.executeBound(
            "UPDATE durable_work SET state='READY', generation=generation+1, attempt_count=0," +
                "lease_owner=NULL,lease_until=NULL,last_error=NULL,next_attempt_at=?,deadline_at=?,updated_at=? " +
                "WHERE id=? AND state='EXHAUSTED' AND generation=? AND lease_epoch=? " +
                "AND ? > clock_timestamp()",
            Timestamp.from(now), Timestamp.from(recoveryDeadlineAt), Timestamp.from(now), workId,
            previous.generation, previous.epoch, Timestamp.from(recoveryDeadlineAt),
        )
        if (updated == 0) return false
        insertRecoveryEvent(connection, workId, commit, previous, recoveryDeadlineAt)
        return true
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
            commit.eventId, previous.operationId, workId, commit.correlationId, workId,
            previous.generation + 1, previous.epoch, commit.operatorId,
            """{"reason":${escapeJson(commit.reason)},"previousDeadlineAt":"${previous.deadlineAt}","deadlineAt":"$deadlineAt"}""",
        )
    }

    /**
     * Reads the lease status of a single work row for pre-send eligibility checks.
     */
    fun leaseStatus(
        connection: Connection,
        workId: UUID,
    ): LeaseStatus? =
        connection
            .queryBound(
                "SELECT id, state, lease_owner, lease_epoch, lease_until, deadline_at FROM durable_work WHERE id = ?",
                listOf(workId),
            ) { rs ->
                LeaseStatus(
                    workId = rs.getObject("id", UUID::class.java),
                    state = rs.getString("state"),
                    leaseOwner = rs.getObject("lease_owner", UUID::class.java),
                    leaseEpoch = rs.getLong("lease_epoch"),
                    leaseUntil = rs.getTimestamp("lease_until")?.toInstant(),
                    deadlineAt = rs.getTimestamp("deadline_at").toInstant(),
                )
            }.singleOrNull()

    private fun escapeJson(value: String): String =
        tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(value)
}
