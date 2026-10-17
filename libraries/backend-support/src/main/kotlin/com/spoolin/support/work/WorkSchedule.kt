package com.spoolin.support.work

import java.time.Instant

/**
 * Pure jitter-based retry schedule calculation.
 *
 * Implements full-jitter exponential backoff: `delay = draw * min(base * factor^(n-1), cap)`,
 * where n is the attempt number that just completed (attempt_count after the failed attempt).
 *
 * D22 defaults: base=500 ms, factor=2, cap=5000 ms, max 3 total attempts.
 */
object WorkSchedule {
    /**
     * Returns the sampled delay in milliseconds before the next attempt.
     *
     * @param baseDelayMs   Persisted base delay in ms (1..60000).
     * @param capDelayMs    Persisted cap delay in ms (1..3600000).
     * @param backoffMultiplier  Persisted exponential multiplier (1..10).
     * @param attemptCount  Attempts started so far in this generation (after the current failure).
     * @param draw          Uniform sample in [0.0, 1.0] for full-jitter.
     * @return Sampled delay in milliseconds.
     */
    fun sampleDelayMs(
        baseDelayMs: Int,
        capDelayMs: Int,
        backoffMultiplier: Int,
        attemptCount: Int,
        draw: Double,
    ): Long {
        require(draw >= 0.0 && draw <= 1.0) { "Jitter draw must be in [0.0, 1.0]" }
        val exp = (attemptCount - 1).coerceAtLeast(0)
        val raw = baseDelayMs.toDouble() * Math.pow(backoffMultiplier.toDouble(), exp.toDouble())
        val upper = minOf(raw, capDelayMs.toDouble())
        return (draw * upper).toLong()
    }

    /**
     * Returns true when a new attempt may be scheduled for [now] under the persisted budget.
     *
     * A new attempt is allowed only when attempt_count < maxAttempts AND now is strictly before
     * the deadline. The next_attempt_at scheduling check is separate (handled by the READY claim
     * query).
     */
    fun canAttempt(
        attemptCount: Int,
        maxAttempts: Int,
        now: Instant,
        deadlineAt: Instant,
    ): Boolean = attemptCount < maxAttempts && now.isBefore(deadlineAt)
}
