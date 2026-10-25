package com.spoolin.core.operations.work

import com.spoolin.core.features.session.SessionJdbcFixture
import com.spoolin.support.work.AttemptOutcome
import com.spoolin.support.work.WorkClaim
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class WorkPersistenceBehaviorTest {
    private val now = Instant.parse("2026-10-25T10:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val claim =
        WorkClaim(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "SYNTHETIC",
            "{}",
            1,
            3,
            now.plusSeconds(300),
            UUID.randomUUID(),
            4,
            UUID.randomUUID(),
            100,
            1000,
            2,
            0,
        )

    @Test
    fun `empty work batch commits selection without emitting remote work`() {
        val db = SessionJdbcFixture(listOf(emptyList()))
        assertTrue(WorkClaimer(db, clock).claim().isEmpty())
        assertEquals(1, db.commits)
        assertThrows(IllegalArgumentException::class.java) { WorkClaimer(db, clock).claim(11) }
    }

    @Test
    fun `lease renewal returns fenced database acceptance and refuses lost owner`() {
        val db = SessionJdbcFixture(emptyList())
        val guard = WorkLeaseGuard(db, clock)
        db.enqueue(emptyMap())
        assertTrue(guard.renew(claim))
        db.updateResult = 0
        db.enqueue(emptyMap())
        assertFalse(guard.renew(claim))
    }

    @Test
    fun `attempt completion persists each observable terminal or bounded retry transition`() {
        val cases =
            listOf(
                AttemptOutcome.SUCCEEDED to "DONE",
                AttemptOutcome.CANCELLED to "CANCELLED",
                AttemptOutcome.PERMANENT to "EXHAUSTED",
                AttemptOutcome.UNCERTAIN to "EXHAUSTED",
                AttemptOutcome.TRANSIENT to "READY",
            )
        for ((outcome, state) in cases) {
            val db = SessionJdbcFixture(listOf(listOf(emptyMap())))
            assertTrue(WorkLeaseGuard(db, clock).complete(claim, outcome, "REMOTE_FAILURE"))
            assertEquals(state, db.writes.first()[1])
            assertEquals(if (outcome == AttemptOutcome.SUCCEEDED) null else "REMOTE_FAILURE", db.writes.first()[4])
            assertEquals(1, db.commits)
        }
        val db = SessionJdbcFixture(listOf(listOf(emptyMap())))
        assertTrue(WorkLeaseGuard(db, clock).complete(claim.copy(attemptCount = 3), AttemptOutcome.TRANSIENT))
        assertEquals("EXHAUSTED", db.writes.first()[1])
    }

    @Test
    fun `stale completion returns false without emitting finished attempt`() {
        val db = SessionJdbcFixture(listOf(listOf(emptyMap())))
        db.updateResult = 0
        assertFalse(WorkLeaseGuard(db, clock).complete(claim, AttemptOutcome.SUCCEEDED))
        assertEquals(1, db.updates)
    }
}
