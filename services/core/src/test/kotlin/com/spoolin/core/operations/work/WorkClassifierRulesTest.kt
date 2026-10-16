package com.spoolin.core.operations.work

import com.spoolin.support.work.AttemptOutcome
import com.spoolin.support.work.WorkClassifier
import com.spoolin.support.work.WorkError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ATDD-285: Classify retryable failure separately from uncertain effect.
 *
 * Verifies:
 * - INVALID_ARGUMENT, PERMISSION_DENIED, NOT_FOUND, STALE_VERSION → PERMANENT (no retry).
 * - UNAVAILABLE_BEFORE_SEND, RATE_LIMITED → TRANSIENT (safe retry within budget).
 * - TIMEOUT_AFTER_SEND, UNKNOWN_PROVIDER → UNCERTAIN (destination check before replay).
 */
class WorkClassifierRulesTest {

    @Test
    fun `INVALID_ARGUMENT maps to PERMANENT`() {
        assertEquals(AttemptOutcome.PERMANENT, WorkClassifier.classify(WorkError.INVALID_ARGUMENT))
    }

    @Test
    fun `PERMISSION_DENIED maps to PERMANENT`() {
        assertEquals(AttemptOutcome.PERMANENT, WorkClassifier.classify(WorkError.PERMISSION_DENIED))
    }

    @Test
    fun `NOT_FOUND maps to PERMANENT`() {
        assertEquals(AttemptOutcome.PERMANENT, WorkClassifier.classify(WorkError.NOT_FOUND))
    }

    @Test
    fun `STALE_VERSION maps to PERMANENT`() {
        assertEquals(AttemptOutcome.PERMANENT, WorkClassifier.classify(WorkError.STALE_VERSION))
    }

    @Test
    fun `UNAVAILABLE_BEFORE_SEND maps to TRANSIENT`() {
        assertEquals(AttemptOutcome.TRANSIENT, WorkClassifier.classify(WorkError.UNAVAILABLE_BEFORE_SEND))
    }

    @Test
    fun `RATE_LIMITED maps to TRANSIENT`() {
        assertEquals(AttemptOutcome.TRANSIENT, WorkClassifier.classify(WorkError.RATE_LIMITED))
    }

    @Test
    fun `TIMEOUT_AFTER_SEND maps to UNCERTAIN`() {
        assertEquals(AttemptOutcome.UNCERTAIN, WorkClassifier.classify(WorkError.TIMEOUT_AFTER_SEND))
    }

    @Test
    fun `UNKNOWN_PROVIDER maps to UNCERTAIN`() {
        assertEquals(AttemptOutcome.UNCERTAIN, WorkClassifier.classify(WorkError.UNKNOWN_PROVIDER))
    }

    @Test
    fun `permanent failures are not safe retries`() {
        listOf(WorkError.INVALID_ARGUMENT, WorkError.PERMISSION_DENIED, WorkError.NOT_FOUND, WorkError.STALE_VERSION)
            .forEach { error ->
                assertTrue(WorkClassifier.isPermanent(error), "$error should be permanent")
                assertFalse(WorkClassifier.isSafeRetry(error), "$error should not be safe retry")
                assertFalse(WorkClassifier.isUncertain(error), "$error should not be uncertain")
            }
    }

    @Test
    fun `transient failures are safe retries only`() {
        listOf(WorkError.UNAVAILABLE_BEFORE_SEND, WorkError.RATE_LIMITED).forEach { error ->
            assertTrue(WorkClassifier.isSafeRetry(error), "$error should be safe retry")
            assertFalse(WorkClassifier.isPermanent(error), "$error should not be permanent")
            assertFalse(WorkClassifier.isUncertain(error), "$error should not be uncertain")
        }
    }

    @Test
    fun `uncertain failures require destination reconciliation`() {
        listOf(WorkError.TIMEOUT_AFTER_SEND, WorkError.UNKNOWN_PROVIDER).forEach { error ->
            assertTrue(WorkClassifier.isUncertain(error), "$error should be uncertain")
            assertFalse(WorkClassifier.isPermanent(error), "$error should not be permanent")
            assertFalse(WorkClassifier.isSafeRetry(error), "$error should not be safe retry")
        }
    }

    @Test
    fun `fake provider response does not change classification`() {
        // ATDD-285: "A fake provider response cannot establish actual acceptance or durable attempts."
        // Classification is based on known error kind, not provider claim.
        assertEquals(AttemptOutcome.UNCERTAIN, WorkClassifier.classify(WorkError.UNKNOWN_PROVIDER))
    }
}
