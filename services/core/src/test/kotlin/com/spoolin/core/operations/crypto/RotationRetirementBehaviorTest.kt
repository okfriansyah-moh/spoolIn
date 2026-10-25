package com.spoolin.core.operations.crypto

import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.KeyRegistry
import com.spoolin.core.features.session.SessionJdbcFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.sql.Timestamp
import java.time.Instant

class RotationRetirementBehaviorTest {
    @Test
    fun `retirement requires independently verified backup custody before database access`() {
        val db = SessionJdbcFixture(emptyList())
        val result = RotationRetirement(db).retire("old", KeyPurpose.PII)
        assertFalse(result.ok)
        assertEquals(0, db.queries)
        assertEquals(0, db.updates)
        assertThrows(IllegalArgumentException::class.java) { RotationRetirement(db).retire("../old", KeyPurpose.PII) }
    }

    @Test
    fun `missing active or not yet reviewed keys cannot retire`() {
        for (keys in listOf(
            emptyList(),
            listOf(key("WRITE", Instant.EPOCH)),
            listOf(key("READ", null)),
            listOf(key("READ", Instant.now().plusSeconds(3600))),
        )) {
            val db = SessionJdbcFixture(listOf(keys))
            val result = RotationRetirement(db, BackupKeyCustody { _, _ -> true }).retire("old", KeyPurpose.PII)
            assertFalse(result.ok)
            assertEquals(0, db.updates)
        }
    }

    @Test
    fun `live encrypted or protected references block retirement with remaining count`() {
        for ((encrypted, protected) in listOf(2L to 0L, 0L to 3L)) {
            val rows = mutableListOf(listOf(key("READ", Instant.EPOCH)), listOf(mapOf("total" to encrypted)))
            if (encrypted == 0L) rows.add(listOf(mapOf("total" to protected)))
            val db = SessionJdbcFixture(rows)
            val result = RotationRetirement(db, BackupKeyCustody { _, _ -> true }).retire("old", KeyPurpose.DRAFT)
            assertFalse(result.ok)
            assertEquals(encrypted + protected, result.remaining)
            assertEquals(0, db.updates)
        }
    }

    @Test
    fun `unreferenced reviewed key retires only after custody checks`() {
        val db = SessionJdbcFixture(listOf(listOf(key("READ", Instant.EPOCH)), listOf(mapOf("total" to 0L)), listOf(mapOf("total" to 0L))))
        val result =
            RotationRetirement(
                db,
                BackupKeyCustody {
                    id,
                    purpose,
                    ->
                    id == "old" && purpose == KeyPurpose.PII
                },
            ).retire("old", KeyPurpose.PII)
        assertTrue(result.ok)
        assertEquals(1, db.updates)
        assertEquals(1, db.commits)
    }

    @Test
    fun `lookup retirement requires all accounts to have replacement aliases`() {
        val keys = listOf(key("READ", Instant.EPOCH), mapOf("id" to "new", "state" to "WRITE", "retire_after" to null))
        val db = SessionJdbcFixture(listOf(keys, listOf(mapOf("total" to 0L)), listOf(mapOf("total" to 4L))))
        val result = RotationRetirement(db, BackupKeyCustody { _, _ -> true }).retire("old", KeyPurpose.EMAIL_LOOKUP)
        assertFalse(result.ok)
        assertEquals(4L, result.remaining)
        assertEquals(0, db.updates)
    }

    @Test
    fun `rotation refuses invalid aliases batches and absent mounted key material`() {
        val db = SessionJdbcFixture(emptyList())
        assertThrows(IllegalArgumentException::class.java) { KeyRotationPlan("same", "same", KeyPurpose.PII) }
        assertThrows(IllegalArgumentException::class.java) { KeyRotationPlan("old", "../new", KeyPurpose.PII) }
        val plan = KeyRotationPlan("old", "new", KeyPurpose.PII)
        assertFalse(plan.rotate(db, KeyRegistry(db), true).ok)
        assertEquals(0, db.queries)
        assertThrows(IllegalArgumentException::class.java) { plan.rotate(db, KeyRegistry(db), true, maxBatches = 0) }
        assertThrows(IllegalArgumentException::class.java) { plan.rotate(db, KeyRegistry(db), true, maxBatches = 10001) }
    }

    private fun key(
        state: String,
        review: Instant?,
    ) = mapOf("id" to "old", "state" to state, "retire_after" to review?.let(Timestamp::from))
}
