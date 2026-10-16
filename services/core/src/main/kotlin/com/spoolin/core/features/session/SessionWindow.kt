package com.spoolin.core.features.session

import java.time.Duration
import java.time.Instant

private const val IDLE_MINUTES = 30L
private const val ABSOLUTE_HOURS = 12L
private val IDLE_WINDOW = Duration.ofMinutes(IDLE_MINUTES)
private val ABSOLUTE_WINDOW = Duration.ofHours(ABSOLUTE_HOURS)
private val TOUCH_WINDOW = Duration.ofMinutes(1)

data class SessionWindow(
    val created: Instant,
    val lastSeen: Instant,
    val expires: Instant = created.plus(ABSOLUTE_WINDOW),
    val revoked: Instant? = null,
) {
    fun eligible(
        now: Instant,
        recordedEpoch: Long,
        currentEpoch: Long,
    ): Boolean =
        revoked == null && recordedEpoch == currentEpoch &&
            now.isBefore(expires) && now.isBefore(lastSeen.plus(IDLE_WINDOW))

    fun shouldTouch(now: Instant): Boolean = !now.isBefore(lastSeen.plus(TOUCH_WINDOW))
}
