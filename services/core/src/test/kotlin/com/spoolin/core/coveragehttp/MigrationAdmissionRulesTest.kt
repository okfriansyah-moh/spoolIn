package com.spoolin.core.coveragehttp

import com.spoolin.core.MigrationRunner
import com.spoolin.core.features.session.SessionJdbcFixture
import com.spoolin.core.operations.receipts.ReceiptWork
import com.spoolin.support.receipts.WorkRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.UUID

class MigrationAdmissionRulesTest {
    @Test
    fun `migration requires bounded lock budget and releases acquired lock after action fails`() {
        val db = SessionJdbcFixture(emptyList())
        val runner = MigrationRunner(db)
        for (budget in listOf(Duration.ZERO, Duration.ofSeconds(-1), Duration.ofSeconds(121))) {
            assertThrows(IllegalArgumentException::class.java) { runner.migrate(budget) }
        }
        assertEquals(0, db.queries)
        db.enqueue(mapOf("1" to true))
        val marker = IllegalStateException("Unit pre-migration action refused")
        assertSame(marker, assertThrows(IllegalStateException::class.java) { runner.migrate(Duration.ofSeconds(1)) { throw marker } })
        assertEquals(1, db.executions)
    }

    @Test
    fun `interrupted lock wait refuses migration and preserves interruption state`() {
        val db = SessionJdbcFixture(listOf(listOf(mapOf("1" to false))))
        Thread.currentThread().interrupt()
        try {
            assertThrows(IllegalStateException::class.java) { MigrationRunner(db).migrate(Duration.ofSeconds(1)) }
        } finally { Thread.interrupted() }
        assertEquals(0, db.executions)
    }

    @Test
    fun `receipt work persists stable effect identity and retry budget and refuses expired intent`() {
        val db = SessionJdbcFixture(emptyList()); val now = Instant.now(); val operation = UUID.randomUUID(); val correlation = UUID.randomUUID()
        val work = WorkRequest("RECHECK", "TITLE", UUID.randomUUID(), now.plusSeconds(60))
        ReceiptWork.enqueue(db.jdbc, operation, correlation, work, now)
        assertEquals(work.id(operation), db.writes.single()[1])
        assertEquals(work.kind, db.writes.single()[3])
        assertEquals(work.maxAttempts, db.writes.single()[5])
        assertEquals(correlation, db.writes.single()[7])
        assertThrows(IllegalArgumentException::class.java) { ReceiptWork.enqueue(db.jdbc, operation, correlation, work, now.plusSeconds(60)) }
        db.updateResult = 0
        assertThrows(IllegalStateException::class.java) { ReceiptWork.enqueue(db.jdbc, operation, correlation, work, now) }
    }
}
