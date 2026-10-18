package com.spoolin.support.notifications

data class NotificationRetry(
    val maxAttempts: Int = DEFAULT_ATTEMPTS,
    val baseDelayMs: Int = DEFAULT_BASE_DELAY_MS,
    val capDelayMs: Int = DEFAULT_CAP_DELAY_MS,
    val multiplier: Int = DEFAULT_MULTIPLIER,
    val deadlineMs: Int = DEFAULT_DEADLINE_MS,
) {
    init {
        require(maxAttempts in 1..MAX_ATTEMPTS && baseDelayMs in 1..MAX_BASE_DELAY_MS)
        require(capDelayMs in baseDelayMs..MAX_DURATION_MS)
        require(multiplier in 1..MAX_MULTIPLIER && deadlineMs in MIN_DEADLINE_MS..MAX_DURATION_MS)
    }

    companion object {
        private const val DEFAULT_ATTEMPTS = 3
        private const val DEFAULT_BASE_DELAY_MS = 500
        private const val DEFAULT_CAP_DELAY_MS = 5000
        private const val DEFAULT_MULTIPLIER = 2
        private const val DEFAULT_DEADLINE_MS = 60000
        private const val MAX_ATTEMPTS = 10
        private const val MAX_BASE_DELAY_MS = 60000
        private const val MAX_DURATION_MS = 3600000
        private const val MAX_MULTIPLIER = 10
        private const val MIN_DEADLINE_MS = 1000
    }
}
