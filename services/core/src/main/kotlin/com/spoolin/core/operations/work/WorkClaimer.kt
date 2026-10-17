package com.spoolin.core.operations.work

import com.spoolin.support.receipts.committed
import com.spoolin.support.work.AttemptOutcome
import com.spoolin.support.work.WorkAttemptCompletion
import com.spoolin.support.work.WorkClaim
import com.spoolin.support.work.WorkSchedule
import com.spoolin.support.work.WorkSql
import java.security.SecureRandom
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

/**
 * Core service work claimer.
 *
 * Claims a batch of eligible READY work items with SELECT FOR UPDATE SKIP LOCKED (batch ≤ 10,
 * concurrency 2 per service), commits lease + ATTEMPT_STARTED before any remote effect, and
 * releases the DB connection before the caller performs its bounded remote call.
 *
 * Callers must honour the lease duration (30 s) and use [WorkLeaseGuard] to renew every 10 s.
 * Effects must complete within 15 s.
 */
class WorkClaimer(
    private val dataSource: DataSource,
    private val clock: Clock = Clock.systemUTC(),
    private val random: SecureRandom = SecureRandom(),
) {
    /**
     * Claims up to [batchSize] READY items due by [now] and returns committed WorkClaim snapshots.
     *
     * Each returned claim has state=LEASED with ATTEMPT_STARTED recorded. The DB connection is
     * closed before returning — callers own the full lease duration for their remote work.
     */
    fun claim(batchSize: Int = 10): List<WorkClaim> {
        val now = clock.instant()
        return dataSource.committed { connection ->
            val candidates = WorkSql.selectForClaim(connection, now, batchSize)
            val workerId = UUID.randomUUID()
            candidates.mapNotNull { candidate ->
                WorkSql.commitClaim(connection, candidate, workerId, now)
            }
        }
    }
}

/**
 * Core service lease guard — heartbeat renewals and fenced terminal commits.
 *
 * All writes use a WHERE clause CAS on (id, lease_owner, lease_epoch, state='LEASED') to fence
 * out stale or replaced workers. A 0-row update means this worker's lease was superseded and the
 * attempt outcome must be treated as UNCERTAIN (see [WorkLeaseGuard.complete]).
 */
class WorkLeaseGuard(
    private val dataSource: DataSource,
    private val clock: Clock = Clock.systemUTC(),
    private val random: SecureRandom = SecureRandom(),
) {
    /**
     * Renews the lease for [claim] by 30 s.  Returns true when the renewal was accepted (i.e.
     * this worker is still the live owner), false when the lease was lost to a replacement.
     */
    fun renew(claim: WorkClaim): Boolean =
        dataSource.committed { connection ->
            WorkSql.renewLease(connection, claim, clock.instant())
        }

    /**
     * Commits the attempt result atomically with the new durable_work state.
     *
     * SUCCEEDED → state=DONE.
     * TRANSIENT → state=READY with jitter-sampled next_attempt_at; EXHAUSTED when budget reached.
     * PERMANENT → state=EXHAUSTED immediately (no auto-retry).
     * UNCERTAIN → state=EXHAUSTED with last_error='UNCERTAIN' (destination check required).
     * CANCELLED → state=CANCELLED.
     *
     * Returns true when this worker's commit was accepted, false when the lease was already lost
     * (stale epoch). A false return does not mean the effect did not happen — callers should treat
     * the remote outcome as UNCERTAIN.
     */
    fun complete(
        claim: WorkClaim,
        outcome: AttemptOutcome,
        errorCode: String? = null,
    ): Boolean {
        val now = clock.instant()
        val (nextState, nextAttemptAt) = resolveTransition(claim, outcome, now)
        return dataSource.committed { connection ->
            WorkSql.completeAttempt(
                connection,
                claim,
                WorkAttemptCompletion(
                    outcome = outcome,
                    errorCode =
                        if (outcome == AttemptOutcome.SUCCEEDED) {
                            null
                        } else {
                            errorCode ?: outcome.code
                        },
                    nextState = nextState,
                    nextAttemptAt = nextAttemptAt,
                ),
                now,
            )
        }
    }

    private fun resolveTransition(
        claim: WorkClaim,
        outcome: AttemptOutcome,
        now: Instant,
    ): Pair<String, Instant?> =
        when (outcome) {
            AttemptOutcome.SUCCEEDED -> "DONE" to null
            AttemptOutcome.CANCELLED -> "CANCELLED" to null
            AttemptOutcome.PERMANENT -> "EXHAUSTED" to null
            AttemptOutcome.UNCERTAIN -> "EXHAUSTED" to null
            AttemptOutcome.TRANSIENT -> {
                val exhausted = !WorkSchedule.canAttempt(claim.attemptCount, claim.maxAttempts, now, claim.deadlineAt)
                if (exhausted) {
                    "EXHAUSTED" to null
                } else {
                    val delayMs =
                        WorkSchedule.sampleDelayMs(
                            claim.baseDelayMs,
                            claim.capDelayMs,
                            claim.backoffMultiplier,
                            claim.attemptCount,
                            random.nextDouble(),
                        )
                    "READY" to now.plusMillis(delayMs)
                }
            }
        }
}
