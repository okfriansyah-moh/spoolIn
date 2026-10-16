package com.spoolin.support.work

import java.time.Instant
import java.util.UUID

/** Snapshot of a durable_work row successfully claimed with SKIP LOCKED. */
data class WorkClaim(
    val workId: UUID,
    val operationId: UUID?,
    val kind: String,
    val payload: String,
    val attemptCount: Int,
    val maxAttempts: Int,
    val deadlineAt: Instant,
    val leaseOwner: UUID,
    val leaseEpoch: Long,
    val correlationId: UUID,
    val baseDelayMs: Int,
    val capDelayMs: Int,
    val backoffMultiplier: Int,
    val generation: Int,
)
