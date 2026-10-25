package com.spoolin.media.operations.work

import com.spoolin.media.fixtures.JdbcBoundary
import com.spoolin.media.fixtures.actorId
import com.spoolin.media.fixtures.instant
import com.spoolin.media.fixtures.operationId
import com.spoolin.support.work.AttemptOutcome
import com.spoolin.support.work.WorkClaim
import com.spoolin.support.work.WorkRecoveryRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.sql.Timestamp
import java.time.Clock
import java.time.ZoneOffset

class WorkBoundaryRulesTest {
    private val clock = Clock.fixed(instant, ZoneOffset.UTC)

    private fun candidate() =
        mapOf<String, Any?>(
            "id" to operationId,
            "operation_id" to operationId,
            "kind" to "MEDIA_PROCESS",
            "payload" to "{}",
            "attempt_count" to 0,
            "max_attempts" to 4,
            "deadline_at" to Timestamp.from(instant.plusSeconds(120)),
            "lease_epoch" to 0L,
            "correlation_id" to operationId,
            "base_delay_ms" to 100,
            "cap_delay_ms" to 1000,
            "backoff_multiplier" to 2,
            "generation" to 0,
        )

    private fun claim() =
        WorkClaim(operationId, operationId, "MEDIA_PROCESS", "{}", 1, 4, instant.plusSeconds(120), actorId, 1, operationId, 100, 1000, 2, 0)

    @Test fun `work claims respect owner concurrency and return committed lease tuples`() {
        for (active in listOf(2, 3)) {
            val db = JdbcBoundary().rows().rows(mapOf("count" to active))
            assertTrue(WorkClaimer(db.source, clock).claim().isEmpty())
            assertEquals(1, db.commits)
        }
        val db =
            JdbcBoundary()
                .rows()
                .rows(mapOf("count" to 1))
                .rows(candidate())
                .rows()
        val selected = WorkClaimer(db.source, clock).claim(1).single()
        assertEquals(operationId, selected.workId)
        assertEquals(1, selected.attemptCount)
        assertEquals(1L, selected.leaseEpoch)
        assertEquals(1, db.commits)
        val race =
            JdbcBoundary()
                .rows()
                .rows(mapOf("count" to 0))
                .rows(candidate())
                .rows()
                .apply { updates.add(0) }
        assertTrue(WorkClaimer(race.source, clock).claim().isEmpty())
    }

    @Test fun `lease guard returns fence outcomes for each terminal and retry classification`() {
        assertTrue(WorkLeaseGuard(JdbcBoundary().source, clock).renew(claim()))
        assertFalse(WorkLeaseGuard(JdbcBoundary().apply { updates.add(0) }.source, clock).renew(claim()))
        for (outcome in AttemptOutcome.entries) {
            val db = JdbcBoundary()
            assertTrue(WorkLeaseGuard(db.source, clock).complete(claim(), outcome))
            assertEquals(1, db.commits)
        }
        for (expired in listOf(claim().copy(attemptCount = 4), claim().copy(deadlineAt = instant))) {
            assertTrue(WorkLeaseGuard(JdbcBoundary().source, clock).complete(expired, AttemptOutcome.TRANSIENT, "SAFE_ERROR"))
        }
    }

    @Test fun `watchdog commits bounded expired work reconciliation and recovery preserves operator attribution`() {
        val db = JdbcBoundary().rows(mapOf("id" to operationId))
        MediaWorkWatchdog(db.source, clock).reconcile()
        assertEquals(1, db.commits)
        val recovery =
            JdbcBoundary().rows(
                mapOf(
                    "generation" to 0,
                    "lease_epoch" to 1L,
                    "operation_id" to operationId,
                    "deadline_at" to Timestamp.from(instant),
                ),
            )
        assertTrue(
            WorkRecovery(
                recovery.source,
                clock,
            ).recover(WorkRecoveryRequest(operationId, "operator", "Verified accepted input", operationId)),
        )
        assertFalse(
            WorkRecovery(
                JdbcBoundary().source,
                clock,
            ).recover(WorkRecoveryRequest(operationId, "operator", "Verified accepted input", operationId)),
        )
    }
}
