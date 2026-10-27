package com.spoolin.support.maintenance

import com.spoolin.support.fixtures.JdbcBoundary
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class RetentionWorkflowRulesTest {
    private val id = UUID.fromString("00000000-0000-0000-0000-000000000001")

    private fun row(value: Any = id) = mapOf("1" to value)

    @Test fun `hourly retention returns component specific affected row counts`() {
        val core = JdbcBoundary().apply { updates.addAll(listOf(2, 3, 4, 5, 6)) }
        assertEquals(
            mapOf("receiptBodies" to 2, "sessions" to 3, "credentials" to 4, "drafts" to 5, "rateWindows" to 6),
            RetentionMaintenance(core.source, "core").hourly(),
        )
        assertEquals(1, core.commits)
        for (owner in listOf("catalog", "media")) {
            val db = JdbcBoundary().apply { updates.add(7) }
            assertEquals(mapOf("receiptBodies" to 7), RetentionMaintenance(db.source, owner).hourly())
        }
        assertThrows(IllegalArgumentException::class.java) { RetentionMaintenance(JdbcBoundary().source, "other") }
    }

    @Test fun `daily retention aggregates each resolved work item and detached activity`() {
        val db =
            JdbcBoundary()
                .rows(row(), row(UUID.randomUUID()))
                .rows(row())
                .rows(row())
                .rows(row())
                .rows(row())
                .rows(row())
                .rows()
        db.updates.addAll(listOf(2, 3, 1, 4, 5))
        assertEquals(
            mapOf("payloads" to 6, "attemptEvents" to 3, "work" to 1, "activityEvents" to 5),
            RetentionMaintenance(db.source, "catalog").daily(),
        )
        assertEquals(3, db.commits)
    }

    @Test fun `work retention preserves ineligible or unstable assets`() {
        val missing = JdbcBoundary().rows()
        assertEquals(emptyMap<String, Int>(), WorkRetention("core").purge(missing.connection, id))
        assertEquals(1, missing.rollbacks)
        val unresolved = JdbcBoundary().rows(row()).rows()
        assertTrue(WorkRetention("catalog").purge(unresolved.connection, id).isEmpty())
        val changed =
            JdbcBoundary()
                .rows(row())
                .rows(row())
                .rows(row())
                .rows(row(UUID.randomUUID()))
        assertTrue(WorkRetention("media").purge(changed.connection, id).isEmpty())
        assertEquals(1, changed.rollbacks)
    }

    @Test fun `media retention clears processing evidence only for stable resolved expired history`() {
        val db =
            JdbcBoundary()
                .rows(row())
                .rows(row())
                .rows(row())
                .rows(row())
                .rows(row())
        db.updates.addAll(listOf(2, 1, 7, 1))
        assertEquals(
            mapOf("payloads" to 2, "attemptEvents" to 7, "work" to 1),
            WorkRetention("media").purge(db.connection, id),
        )
    }

    @Test fun `recent resolved work loses payload while its event history remains`() {
        val db =
            JdbcBoundary()
                .rows(row())
                .rows(row())
                .rows()
                .apply { updates.add(3) }
        assertEquals(mapOf("payloads" to 3), WorkRetention("core").purge(db.connection, id))
        assertEquals(listOf(id), WorkRetention("core").candidates(JdbcBoundary().rows(row()).connection))
    }
}
