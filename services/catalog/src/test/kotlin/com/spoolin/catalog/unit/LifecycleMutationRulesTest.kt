package com.spoolin.catalog.unit

import com.spoolin.catalog.features.lifecycle.DeletionImpact
import com.spoolin.catalog.features.lifecycle.LifecycleCommand
import com.spoolin.catalog.features.lifecycle.LifecycleRules
import com.spoolin.catalog.features.lifecycle.LifecycleStore
import io.grpc.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class LifecycleMutationRulesTest {
    private fun command(
        action: LifecycleRules.Action = LifecycleRules.Action.ARCHIVE,
        confirmed: Boolean = false,
    ) = LifecycleCommand(unitTarget, 7, action, confirmed, null, unitOperation, unitCorrelation, unitActor, "policy")

    private fun title(
        status: String = "ACTIVE",
        kind: String = "MOVIE",
        version: Long = 7,
        hold: UUID? = null,
    ) = listOf(
        mapOf(
            "status" to status,
            "kind" to kind,
            "version" to version,
            "attachment_id" to hold,
        ),
    )

    @Test fun `movie archive and restore persist state and return next version`() {
        for ((before, action, after) in listOf(
            Triple("ACTIVE", LifecycleRules.Action.ARCHIVE, "ARCHIVED"),
            Triple("ARCHIVED", LifecycleRules.Action.RESTORE, "ACTIVE"),
        )) {
            val db = JdbcRows(title(before), emptyList())
            val result = db.source.connection.use { LifecycleStore(unitClock).movie(it, command(action)) }
            assertEquals(unitTarget, result.resultId)
            assertEquals(8L, result.version)
            assertTrue(db.writes.any { after in it && unitTarget in it && 7L in it })
            assertTrue(
                db.writes
                    .flatten()
                    .filterIsInstance<String>()
                    .any { it.contains(before) && it.contains(after) },
            )
        }
    }

    @Test fun `movie deletion removes catalog relationships and schedules retained artwork release`() {
        for (hold in listOf(null, unitAsset)) {
            val rows =
                if (hold == null) {
                    arrayOf(title("ARCHIVED"), emptyList())
                } else {
                    arrayOf(title("ARCHIVED", hold = hold), emptyList(), listOf(mapOf("version" to 8L)))
                }
            val db = JdbcRows(*rows)
            val result = db.source.connection.use { LifecycleStore(unitClock).movie(it, command(LifecycleRules.Action.DELETE, true)) }
            assertEquals(8L, result.version)
            assertTrue(db.writes.any { "DELETED" in it })
            val payloads = db.writes.flatten().filterIsInstance<String>()
            if (hold != null) assertTrue(payloads.any { it.contains("catalogVersion") && it.contains(unitAsset.toString()) })
        }
    }

    @Test fun `movie lifecycle rejects missing television stale state and unconfirmed deletion`() {
        for ((rows, request, code) in listOf(
            Triple(emptyList<Map<String, Any?>>(), command(), null),
            Triple(title(kind = "TV"), command(), "TV_READ_ONLY"),
            Triple(title(version = 6), command(), "STALE_VERSION"),
            Triple(title("DELETED"), command(), "LIFECYCLE_STATE_CHANGED"),
            Triple(title("ARCHIVED"), command(LifecycleRules.Action.DELETE), "CONFIRMATION_REQUIRED"),
        )) {
            val db = JdbcRows(rows)
            val failure =
                assertThrows(io.grpc.StatusRuntimeException::class.java) {
                    db.source.connection.use { LifecycleStore(unitClock).movie(it, request) }
                }
            assertEquals(code, failure.status.description)
            assertTrue(db.writes.isEmpty())
        }
    }

    @Test fun `person archive and restore update version and record state changes`() {
        for ((before, action, after) in listOf(
            Triple("ACTIVE", LifecycleRules.Action.ARCHIVE, "ARCHIVED"),
            Triple("ARCHIVED", LifecycleRules.Action.RESTORE, "ACTIVE"),
        )) {
            val db = JdbcRows(emptyList(), listOf(mapOf("status" to before, "version" to 7L)))
            val result = db.source.connection.use { LifecycleStore(unitClock).person(it, command(action), DeletionImpact(db.source)) }
            assertEquals(8L, result.version)
            assertTrue(db.writes.any { after in it && 7L in it })
        }
        val missing = JdbcRows(emptyList(), emptyList())
        assertEquals(
            Status.Code.NOT_FOUND,
            assertThrows(io.grpc.StatusRuntimeException::class.java) {
                missing.source.connection.use { LifecycleStore(unitClock).person(it, command(), DeletionImpact(missing.source)) }
            }.status.code,
        )
    }

    @Test fun `credit tombstone ownership rejects dynamic column input`() {
        val db = JdbcRows()
        assertThrows(IllegalArgumentException::class.java) {
            db.source.connection.use { LifecycleStore(unitClock).removeCreditTombstones(it, "unsafe", unitTarget) }
        }
        assertTrue(db.writes.isEmpty())
    }
}
