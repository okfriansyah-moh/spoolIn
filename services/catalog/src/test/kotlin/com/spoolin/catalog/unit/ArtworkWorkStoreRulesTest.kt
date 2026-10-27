package com.spoolin.catalog.unit

import com.spoolin.catalog.features.artwork.ArtworkIntent
import com.spoolin.catalog.features.artwork.ArtworkWorkStore
import com.spoolin.support.work.WorkClaim
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

internal fun artworkClaim(kind: String = "ARTWORK_ATTACH") =
    WorkClaim(
        unitOperation,
        unitOperation,
        kind,
        """{"subjectId":"$unitOperation","credentialId":"$unitCredential","authEpoch":1,"catalogVersion":8}""",
        1,
        3,
        Instant.now().plusSeconds(300),
        unitCredential,
        2,
        unitCorrelation,
        100,
        1000,
        2,
        0,
    )

internal fun artworkRow(
    state: String = "PREPARING",
    old: UUID? = null,
) = listOf(
    mapOf(
        "title_id" to unitTarget,
        "actor_id" to unitActor,
        "asset_id" to unitAsset,
        "expected_version" to 7L,
        "policy_revision" to "policy",
        "state" to state,
        "old_hold_id" to old,
    ),
)

internal fun artworkIntent(
    state: String = "PREPARING",
    old: UUID? = null,
) = ArtworkIntent(unitOperation, unitTarget, unitActor, unitAsset, 7, "policy", state, old)

class ArtworkWorkStoreRulesTest {
    private val one = listOf(mapOf("one" to 1))

    private fun store(
        db: JdbcRows,
        policy: String = "policy",
    ) = ArtworkWorkStore(db.source, ownerReceipts(db)) { policy }

    @Test fun `attachment work commits selected asset version receipt confirmation and prior hold release`() {
        val db =
            JdbcRows(
                one,
                one,
                one,
                artworkRow(old = unitAsset),
                listOf(mapOf("version" to 7L, "status" to "ACTIVE")),
                listOf(mapOf("correlation_id" to unitCorrelation)),
                listOf(mapOf("version" to 8L)),
                one,
            )
        store(db).attach(artworkClaim(), artworkIntent(old = unitAsset))
        assertTrue(db.writes.any { unitAsset in it && 8L in it && unitTarget in it })
        assertTrue(db.writes.any { "ARTWORK_CONFIRM" in it })
        assertTrue(db.writes.any { "ARTWORK_RELEASE" in it })
        assertTrue(db.writes.any { "DONE" in it })
        assertTrue(
            db.writes
                .flatten()
                .filterIsInstance<String>()
                .any { it.contains("resultId") && it.contains(unitTarget.toString()) },
        )
    }

    @Test fun `attachment work rejects stale title policy or archived state and releases hold`() {
        for ((title, policy) in listOf(
            emptyList<Map<String, Any?>>() to "policy",
            listOf(mapOf("version" to 6L, "status" to "ACTIVE")) to "policy",
            listOf(mapOf("version" to 7L, "status" to "ARCHIVED")) to "policy",
            listOf(mapOf("version" to 7L, "status" to "ACTIVE")) to "changed-policy",
        )) {
            val db = JdbcRows(one, one, one, artworkRow(), title, listOf(mapOf("version" to 8L)), one)
            store(db, policy).attach(artworkClaim(), artworkIntent())
            assertTrue(db.writes.any { "STALE_VERSION" in it })
            assertTrue(db.writes.any { "ARTWORK_RELEASE" in it })
            assertFalse(db.writes.any { "ARTWORK_CONFIRM" in it })
        }
    }

    @Test fun `already completed attachment finishes work without selecting again`() {
        val db = JdbcRows(one, one, one, artworkRow("ATTACHED"), one)
        store(db).attach(artworkClaim(), artworkIntent("ATTACHED"))
        assertTrue(db.writes.any { "DONE" in it })
        assertFalse(db.writes.any { "ARTWORK_CONFIRM" in it })
        val lost = JdbcRows(one, one, emptyList())
        assertEquals(
            "WORK_LEASE_LOST",
            assertThrows(IllegalStateException::class.java) {
                store(lost).attach(artworkClaim(), artworkIntent())
            }.message,
        )
        assertTrue(lost.writes.isEmpty())
    }

    @Test fun `abort releases preparing hold and only completes already settled work`() {
        val preparing = JdbcRows(one, one, one, artworkRow(), listOf(mapOf("version" to 8L)), one)
        store(preparing).abort(artworkClaim(), artworkIntent(), "FILE_INVALID")
        assertTrue(preparing.writes.any { "FILE_INVALID" in it })
        assertTrue(preparing.writes.any { "ARTWORK_RELEASE" in it })
        val settled = JdbcRows(one, one, one, artworkRow("ABORTED"), one)
        store(settled).abort(artworkClaim(), artworkIntent("ABORTED"), "FILE_INVALID")
        assertFalse(settled.writes.any { "FILE_INVALID" in it })
        assertTrue(settled.writes.any { "DONE" in it })
    }

    @Test fun `exhausted attachment reconciliation aborts pending intent and queues release`() {
        val db =
            JdbcRows(
                listOf(mapOf("id" to unitOperation)),
                one,
                one,
                listOf(mapOf("correlation_id" to unitCorrelation)),
                artworkRow(),
                listOf(mapOf("version" to 8L)),
            )
        store(db).reconcileExhausted()
        assertTrue(db.writes.any { "ARTWORK_RETRY_EXHAUSTED" in it })
        assertTrue(db.writes.any { "ARTWORK_RELEASE" in it })
        val none = JdbcRows(emptyList())
        store(none).reconcileExhausted()
        assertTrue(none.writes.isEmpty())
    }
}
