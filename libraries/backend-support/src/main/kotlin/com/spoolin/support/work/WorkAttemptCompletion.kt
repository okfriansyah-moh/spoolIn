package com.spoolin.support.work

import java.time.Instant
import java.util.UUID

/** Terminal or reschedule outcome for [WorkSql.completeAttempt]. */
data class WorkAttemptCompletion(
    val outcome: AttemptOutcome,
    val errorCode: String? = null,
    val nextState: String,
    val nextAttemptAt: Instant? = null,
    val eventId: UUID = UUID.randomUUID(),
)

/** Operator recovery metadata for [WorkSql.recoverWork]. */
data class WorkRecoveryCommit(
    val operatorId: String,
    val reason: String,
    val correlationId: UUID,
    val eventId: UUID = UUID.randomUUID(),
)
