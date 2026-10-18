package com.spoolin.media.operations.work

import com.spoolin.support.receipts.committed
import com.spoolin.support.receipts.queryBound
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
 * Media service work claimer.
 *
 * Claims a batch of eligible READY work items with SELECT FOR UPDATE SKIP LOCKED (batch ≤ 10,
 * concurrency 2 per service), commits lease + ATTEMPT_STARTED before any remote effect, and
 * releases the DB connection before the caller performs its bounded remote call.
 */
class WorkClaimer(
    private val dataSource: DataSource,
    private val clock: Clock = Clock.systemUTC(),
    private val random: SecureRandom = SecureRandom(),
) {
    fun claim(batchSize: Int = 10): List<WorkClaim> {
        val now = clock.instant()
        return dataSource.committed { connection ->
            connection.queryBound("SELECT pg_advisory_xact_lock(720022)", emptyList()) { true }
            val active =
                connection
                    .queryBound(
                        "SELECT count(*) AS count FROM durable_work WHERE kind='MEDIA_PROCESS' AND state='LEASED'",
                        emptyList(),
                    ) { it.getInt("count") }
                    .single()
            val capacity = minOf(batchSize, 2 - active)
            if (capacity <= 0) return@committed emptyList()
            val candidates = WorkSql.selectForClaim(connection, now, capacity, setOf("MEDIA_PROCESS"))
            val workerId = UUID.randomUUID()
            candidates.mapNotNull { candidate ->
                WorkSql.commitClaim(connection, candidate, workerId, now)
            }
        }
    }
}

/**
 * Media service lease guard — heartbeat renewals and fenced terminal commits.
 */
class WorkLeaseGuard(
    private val dataSource: DataSource,
    private val clock: Clock = Clock.systemUTC(),
    private val random: SecureRandom = SecureRandom(),
) {
    fun renew(claim: WorkClaim): Boolean =
        dataSource.committed { connection ->
            WorkSql.renewLease(connection, claim, clock.instant())
        }

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
