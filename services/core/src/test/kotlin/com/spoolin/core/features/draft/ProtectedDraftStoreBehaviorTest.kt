package com.spoolin.core.features.draft

import com.spoolin.core.features.crypto.AeadCipher
import com.spoolin.core.features.crypto.KeyMount
import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.KeyRegistry
import com.spoolin.core.features.crypto.MountedKeys
import com.spoolin.core.features.crypto.NonceAllocator
import com.spoolin.core.features.crypto.ProtectedEnvelope
import com.spoolin.core.features.crypto.ProtectionAlert
import com.spoolin.core.features.crypto.ProtectionBinding
import com.spoolin.core.features.crypto.ProtectionUnavailable
import com.spoolin.core.features.session.SessionJdbcFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class ProtectedDraftStoreBehaviorTest {
    @TempDir lateinit var root: Path
    private val account = UUID.randomUUID()
    private val draft = UUID.randomUUID()
    private val now = Instant.parse("2026-10-25T10:00:00Z")
    private val key = ByteArray(32) { 8 }
    private val payload = DraftPayload()
    private val input = DraftInput("PERSON", null, null, "{\"name\":\"山田\"}", null, "revision")

    @Test
    fun `create saves encrypted values and update uses exact compare and swap version`() =
        fixture { db, store, _ ->
            db.enqueue(mapOf("id" to "draft-key"))
            db.enqueue(mapOf("id" to account))
            db.enqueue(mapOf("1" to 0L))
            val command = DraftPersistCommand(account, draft, null, input, envelope())
            assertEquals(0L, store.saveInTransaction(db.jdbc, command))
            db.enqueue(mapOf("id" to "draft-key"))
            db.enqueue(mapOf("id" to account))
            assertEquals(5L, store.saveInTransaction(db.jdbc, command.copy(expectedVersion = 4)))
            assertEquals(2, db.updates)
        }

    @Test
    fun `discard protects absent ownership and rejects stale version without deleting`() =
        fixture { db, store, _ ->
            db.enqueue(mapOf("id" to account))
            db.enqueue()
            assertFalse(store.discardInTransaction(db.jdbc, account, draft, 3))
            db.enqueue(mapOf("id" to account))
            db.enqueue(mapOf("version" to 2L))
            assertThrows(DraftStaleVersion::class.java) { store.discardInTransaction(db.jdbc, account, draft, 3) }
            assertEquals(0, db.updates)
            db.enqueue(mapOf("id" to account))
            db.enqueue(mapOf("version" to 3L))
            assertTrue(store.discardInTransaction(db.jdbc, account, draft, 3))
        }

    @Test
    fun `list decrypts exact body and skips corrupted envelope with alert`() =
        fixture { db, store, alerts ->
            val valid = envelope()
            db.enqueue(row(valid))
            db.enqueue(mapOf("id" to "draft-key"))
            val listed = store.list(account).single()
            assertEquals(input.values, listed.values)
            assertEquals(draft, listed.id)
            assertEquals("PERSON", listed.targetKind)
            assertEquals(2L, listed.version)
            assertEquals(now.plusSeconds(3600), listed.expiresAt)
            db.enqueue(row(ProtectedEnvelope(1, "draft-key", valid.nonce(), ByteArray(32))))
            db.enqueue(mapOf("id" to "draft-key"))
            assertTrue(store.list(account).isEmpty())
            assertEquals(listOf(draft), alerts)
        }

    @Test
    fun `unavailable owner or changed write key blocks persistence`() =
        fixture { db, store, _ ->
            val command = DraftPersistCommand(account, draft, null, input, envelope())
            db.enqueue(mapOf("id" to "another-key"))
            assertThrows(ProtectionUnavailable::class.java) { store.saveInTransaction(db.jdbc, command) }
            db.enqueue(mapOf("id" to "draft-key"))
            db.enqueue()
            assertThrows(ProtectionUnavailable::class.java) { store.saveInTransaction(db.jdbc, command) }
            assertEquals(0, db.updates)
        }

    @Test
    fun `quota boundary and stale compare and swap cannot overwrite another draft`() =
        fixture { db, store, _ ->
            val command = DraftPersistCommand(account, draft, null, input, envelope())
            db.enqueue(mapOf("id" to "draft-key"))
            db.enqueue(mapOf("id" to account))
            db.enqueue(mapOf("1" to 20L))
            assertThrows(DraftQuotaExceeded::class.java) { store.saveInTransaction(db.jdbc, command) }
            assertEquals(0, db.updates)
            db.updateResult = 0
            db.enqueue(mapOf("id" to "draft-key"))
            db.enqueue(mapOf("id" to account))
            assertThrows(DraftStaleVersion::class.java) { store.saveInTransaction(db.jdbc, command.copy(expectedVersion = 4)) }
        }

    private fun envelope() =
        AeadCipher().encrypt(
            "draft-key",
            key,
            ByteArray(12) {
                1
            },
            ProtectionBinding("core", "form_draft", draft, KeyPurpose.DRAFT),
            payload.encode("PERSON", input.values),
        )

    private fun row(envelope: ProtectedEnvelope): Map<String, Any?> =
        mapOf(
            "id" to draft,
            "target_kind" to "PERSON",
            "target_id" to null,
            "base_version" to null,
            "operation_id" to null,
            "body_key_id" to envelope.keyId,
            "body_nonce" to envelope.nonce(),
            "body_cipher" to envelope.cipher(),
            "version" to 2L,
            "expires_at" to Timestamp.from(now.plusSeconds(3600)),
        )

    private fun fixture(test: (SessionJdbcFixture, ProtectedDrafts, MutableList<UUID>) -> Unit) {
        val file = root.resolve("key")
        Files.write(file, key)
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"))
        val db = SessionJdbcFixture(emptyList())
        val alerts = mutableListOf<UUID>()
        MountedKeys(mapOf("draft-key" to KeyMount(KeyPurpose.DRAFT, file)), null).use { keys ->
            val crypto = DraftCryptography(KeyRegistry(db), keys, NonceAllocator(db), AeadCipher())
            test(db, ProtectedDrafts(db, crypto, payload, ProtectionAlert { alerts.add(it) }, Clock.fixed(now, ZoneOffset.UTC)), alerts)
        }
    }
}
