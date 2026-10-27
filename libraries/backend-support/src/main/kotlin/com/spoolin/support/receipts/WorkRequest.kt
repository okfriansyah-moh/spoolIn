package com.spoolin.support.receipts

import java.time.Instant
import java.util.UUID

private const val MAX_ATTEMPTS = 10
private const val MAX_BASE_DELAY_MS = 60000
private const val MAX_CAP_DELAY_MS = 3600000
private const val MAX_MULTIPLIER = 10

data class WorkRequest(
    val step: String,
    val kind: String,
    val subjectId: UUID,
    val deadline: Instant,
    val maxAttempts: Int = 3,
    val baseDelayMs: Int = 500,
    val capDelayMs: Int = 5000,
    val multiplier: Int = 2,
) {
    init {
        require(step.matches(Regex("[A-Za-z][A-Za-z0-9_-]{0,63}")))
        require(kind.matches(Regex("[A-Z][A-Z0-9_]{0,63}")) && kind != "NOTIFICATION")
        require(
            maxAttempts in 1..MAX_ATTEMPTS && baseDelayMs in 1..MAX_BASE_DELAY_MS && capDelayMs in 1..MAX_CAP_DELAY_MS,
        )
        require(multiplier in 1..MAX_MULTIPLIER && capDelayMs >= baseDelayMs)
    }

    fun id(parent: UUID): UUID =
        com.spoolin.support.identity.NamedIdentities
            .id(parent, step.toByteArray(Charsets.UTF_8))
}
