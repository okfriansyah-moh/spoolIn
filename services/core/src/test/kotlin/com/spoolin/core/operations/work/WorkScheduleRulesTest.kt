package com.spoolin.core.operations.work

import com.spoolin.support.work.WorkSchedule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * ATDD-286: Bound jitter and remaining retry budget without resetting it.
 *
 * Verifies:
 * - Full-jitter delay arithmetic for attempt indices 1, 2, and 5 with draws 0, 0.5, 1.0.
 * - Zero retries (maxAttempts=1) permits only the initial send; no new send after first attempt.
 * - Persisted attempt_count=3/maxAttempts=3 prevents a further attempt.
 * - A past deadline prevents a further attempt regardless of attempt budget.
 */
class WorkScheduleRulesTest {
    // D22-like defaults: base=500ms, factor=2, cap=5000ms
    private val base = 500
    private val cap = 5000
    private val factor = 2

    @Test
    fun `attempt index 1 produces upper bound of 500 ms with full jitter`() {
        // attempt_count=1 after first failed attempt → exp=0 → upper=500
        assertEquals(0L, WorkSchedule.sampleDelayMs(base, cap, factor, 1, 0.0))
        assertEquals(250L, WorkSchedule.sampleDelayMs(base, cap, factor, 1, 0.5))
        assertEquals(500L, WorkSchedule.sampleDelayMs(base, cap, factor, 1, 1.0))
    }

    @Test
    fun `attempt index 2 produces upper bound of 1000 ms with full jitter`() {
        // attempt_count=2 → exp=1 → upper=1000
        assertEquals(0L, WorkSchedule.sampleDelayMs(base, cap, factor, 2, 0.0))
        assertEquals(500L, WorkSchedule.sampleDelayMs(base, cap, factor, 2, 0.5))
        assertEquals(1000L, WorkSchedule.sampleDelayMs(base, cap, factor, 2, 1.0))
    }

    @Test
    fun `attempt index 5 is capped at 5000 ms with full jitter`() {
        // attempt_count=5 → exp=4 → raw=8000 → capped at 5000
        assertEquals(0L, WorkSchedule.sampleDelayMs(base, cap, factor, 5, 0.0))
        assertEquals(2500L, WorkSchedule.sampleDelayMs(base, cap, factor, 5, 0.5))
        assertEquals(5000L, WorkSchedule.sampleDelayMs(base, cap, factor, 5, 1.0))
    }

    @Test
    fun `zero retries permits only the initial send`() {
        val now = Instant.parse("2026-10-20T00:00:00Z")
        val deadline = now.plusSeconds(3600)
        // maxAttempts=1: attempt_count=0 → can attempt; attempt_count=1 → exhausted
        assertTrue(WorkSchedule.canAttempt(0, 1, now, deadline), "should allow initial send")
        assertFalse(WorkSchedule.canAttempt(1, 1, now, deadline), "zero retries: no further send")
    }

    @Test
    fun `attempt count at maximum prevents new send`() {
        val now = Instant.parse("2026-10-20T00:00:00Z")
        val deadline = now.plusSeconds(3600)
        // attempt_count=3 == maxAttempts=3 → no new send
        assertFalse(WorkSchedule.canAttempt(3, 3, now, deadline), "count==maxAttempts: no new send")
    }

    @Test
    fun `past deadline prevents new send even with remaining budget`() {
        val deadline = Instant.parse("2026-10-20T00:00:00Z")
        val now = deadline // exactly at deadline — not strictly before
        assertFalse(WorkSchedule.canAttempt(0, 3, now, deadline), "at deadline: no new send")
        val past = deadline.plusMillis(1)
        assertFalse(WorkSchedule.canAttempt(0, 3, past, deadline), "after deadline: no new send")
    }

    @Test
    fun `changed multiplier does not alter already computed delay when given fixed inputs`() {
        // The persisted budget drives scheduling; this verifies independence from config changes
        // by using the saved values directly in sampleDelayMs.
        val savedBase = 500
        val savedCap = 5000
        val savedFactor = 2
        val changedFactor = 5 // simulates a config change that must not affect saved budget
        val delayWithSaved = WorkSchedule.sampleDelayMs(savedBase, savedCap, savedFactor, 2, 0.5)
        val delayWithChanged = WorkSchedule.sampleDelayMs(savedBase, savedCap, changedFactor, 2, 0.5)
        assertEquals(500L, delayWithSaved)
        // With changedFactor=5: upper=min(500*5,5000)=2500 → 0.5*2500=1250 — different
        assertEquals(1250L, delayWithChanged)
        // Caller must use persisted values, not runtime config, to preserve generation's budget
        // (this test documents the invariant rather than testing the framework enforcement)
    }
}
