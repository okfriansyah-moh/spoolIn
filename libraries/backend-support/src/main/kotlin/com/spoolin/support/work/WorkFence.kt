package com.spoolin.support.work

import java.time.Instant
import java.util.UUID

/**
 * Snapshot of a durable_work row used to verify lease eligibility.
 *
 * Read from the DB for CAS checks; the actual claim/renew/complete SQL
 * uses WHERE clause fencing against the same fields.
 */
data class LeaseStatus(
    val workId: UUID,
    val state: String,
    val leaseOwner: UUID?,
    val leaseEpoch: Long,
    val leaseUntil: Instant?,
    val deadlineAt: Instant,
)

/**
 * Pure lease eligibility check for worker claims, heartbeat renewals and terminal completions.
 *
 * A worker is the "current live owner" and may renew or start an eligible effect when ALL of:
 * 1. state == 'LEASED'      — work is currently leased (not terminal or READY)
 * 2. lease_owner == workerId — this worker holds the lease
 * 3. lease_epoch == epoch    — the generation fence matches (no replacement has occurred)
 * 4. lease_until > now       — the lease has not expired yet
 * 5. deadline_at > now       — the work deadline has not been reached
 *
 * The SQL UPDATE WHERE clause enforces conditions 1–3 atomically; this function makes the
 * eligibility logic explicit for unit testing and pre-send checks.
 */
object WorkFence {
    fun isEligible(
        status: LeaseStatus,
        workerId: UUID,
        claimedEpoch: Long,
        now: Instant,
    ): Boolean {
        val until = status.leaseUntil
        return status.state == "LEASED" &&
            status.leaseOwner == workerId &&
            status.leaseEpoch == claimedEpoch &&
            until != null &&
            until.isAfter(now) &&
            status.deadlineAt.isAfter(now)
    }
}
