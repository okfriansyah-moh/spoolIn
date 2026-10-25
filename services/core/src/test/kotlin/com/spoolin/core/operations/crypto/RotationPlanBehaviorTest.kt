package com.spoolin.core.operations.crypto

import com.spoolin.core.features.crypto.*
import com.spoolin.core.features.session.SessionJdbcFixture
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions

class RotationPlanBehaviorTest {
    @TempDir lateinit var root: Path

    @Test
    fun `dry run checks material and source target states without activation or backfill`() = fixture { db, keys ->
        db.enqueue(emptyMap()); db.enqueue(state("old", "WRITE"), state("new", "READ")); db.enqueue(emptyMap())
        val outcome = KeyRotationPlan("old", "new", KeyPurpose.PII).rotate(db, KeyRegistry(db), true, keys)
        assertTrue(outcome.ok)
        assertEquals(0, db.updates)
        assertEquals(0, db.commits)
        db.enqueue(emptyMap()); db.enqueue(state("old", "WRITE")); db.enqueue(emptyMap())
        assertFalse(KeyRotationPlan("old", "new", KeyPurpose.PII).rotate(db, KeyRegistry(db), true, keys).ok)
    }

    @Test
    fun `activation validates target cryptography reserves nonce and completes empty live backfill`() = fixture { db, keys ->
        db.enqueue(emptyMap())
        db.enqueue(state("old", "WRITE"), state("new", "READ"))
        db.enqueue()
        db.enqueue(mapOf("nonce" to 3L))
        db.enqueue(state("old", "WRITE"), state("new", "READ"))
        db.enqueue()
        db.enqueue(mapOf("total" to 0L))
        db.enqueue(emptyMap())
        val result = KeyRotationPlan("old", "new", KeyPurpose.PII).rotate(db, KeyRegistry(db), false, keys, 1)
        assertTrue(result.ok)
        assertEquals(0, result.backfilled)
        assertEquals(2, db.updates)
        assertEquals(2, db.commits)
    }

    @Test
    fun `target registration validates purpose state and material availability`() = fixture { db, keys ->
        db.enqueue(mapOf("purpose" to "PII", "state" to "READ"))
        RotationKeys.registerTarget(db, keys, "new", KeyPurpose.PII)
        assertEquals(1, db.commits)
        db.enqueue(mapOf("purpose" to "DRAFT", "state" to "READ"))
        assertThrows(IllegalArgumentException::class.java) { RotationKeys.registerTarget(db, keys, "new", KeyPurpose.PII) }
        assertThrows(IllegalArgumentException::class.java) { RotationKeys.registerTarget(db, keys, "missing", KeyPurpose.PII) }
    }

    @Test
    fun `rotation key file accepts explicit protected mounts and rejects unknown keys relative paths and duplicate fields`() = fixture { _, _ ->
        val old = root.resolve("old")
        val next = root.resolve("new")
        val valid = "{\"old\":{\"purpose\":\"PII\",\"file\":\"$old\"},\"new\":{\"purpose\":\"PII\",\"file\":\"$next\"}}"
        val file = root.resolve("rotation.json")
        Files.writeString(file, valid)
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"))
        RotationKeys.load(file).use { assertNotNull(it.key("new", KeyPurpose.PII)) }
        for (invalid in listOf("{}", valid.replace("$old", "relative"), valid.replace("\"purpose\":\"PII\"", "\"purpose\":\"PII\",\"extra\":true"))) {
            Files.writeString(file, invalid)
            assertThrows(IllegalArgumentException::class.java) { RotationKeys.load(file) }
        }
    }

    private fun state(id: String, state: String) = mapOf("id" to id, "state" to state)
    private fun fixture(test: (SessionJdbcFixture, MountedKeys) -> Unit) {
        val old = root.resolve("old"); Files.write(old, ByteArray(32) { 41 })
        val next = root.resolve("new"); Files.write(next, ByteArray(32) { 43 })
        listOf(old, next).forEach { Files.setPosixFilePermissions(it, PosixFilePermissions.fromString("rw-------")) }
        MountedKeys(mapOf("old" to KeyMount(KeyPurpose.PII, old), "new" to KeyMount(KeyPurpose.PII, next)), null).use { test(SessionJdbcFixture(emptyList()), it) }
    }
}
