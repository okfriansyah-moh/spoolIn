package com.spoolin.core.operations.crypto

import com.spoolin.core.features.account.persistence.AccountPayload
import com.spoolin.core.features.account.persistence.IdentityRules
import com.spoolin.core.features.crypto.AeadCipher
import com.spoolin.core.features.crypto.KeyMount
import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.KeyRegistry
import com.spoolin.core.features.crypto.MountedKeys
import com.spoolin.core.features.crypto.ProtectedEnvelope
import com.spoolin.core.features.crypto.ProtectionBinding
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
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class RotationBackfillBehaviorTest {
    @TempDir lateinit var root: Path
    private val id = UUID.randomUUID()
    private val oldBytes = ByteArray(32) { 31 }
    private val newBytes = ByteArray(32) { 37 }

    @Test
    fun `protected backfill authenticates original data and commits resumable progress for both encrypted stores`() {
        for (purpose in listOf(KeyPurpose.PII, KeyPurpose.DRAFT)) {
            fixture(purpose) { db, keys ->
                val table = if (purpose == KeyPurpose.DRAFT) "form_draft" else "user_account"
                val envelope =
                    AeadCipher().encrypt(
                        "old",
                        oldBytes,
                        ByteArray(12) {
                            1
                        },
                        ProtectionBinding("core", table, id, purpose),
                        "preserved content".toByteArray(),
                    )
                db.enqueue(row(envelope))
                db.enqueue(mapOf("id" to "old"))
                db.enqueue(mapOf("id" to "new"))
                db.enqueue(mapOf("allocated" to 3L))
                db.enqueue(emptyMap())
                db.enqueue(mapOf("total" to 0L))
                val result = RotationBackfill(db, KeyRegistry(db), keys, KeyRotationPlan("old", "new", purpose)).run(1)
                assertTrue(result.ok)
                assertEquals(1, result.backfilled)
                assertEquals(0L, result.remaining)
                assertEquals(2, db.commits)
                assertEquals(1, db.updates)
            }
        }
    }

    @Test
    fun `backfill reports remaining work and does not claim completion when no rows changed`() =
        fixture(KeyPurpose.PII) { db, keys ->
            db.enqueue()
            db.enqueue(mapOf("total" to 2L))
            val result = RotationBackfill(db, KeyRegistry(db), keys, KeyRotationPlan("old", "new", KeyPurpose.PII)).run(10)
            assertFalse(result.ok)
            assertEquals(0, result.backfilled)
            assertEquals(2L, result.remaining)
            assertEquals(0, db.updates)
        }

    @Test
    fun `unreadable key and unauthenticated source stop rotation before writes`() {
        for (readable in listOf(false, true)) {
            fixture(KeyPurpose.PII) { db, keys ->
                val invalid = ProtectedEnvelope(1, "old", ByteArray(12), ByteArray(32))
                db.enqueue(row(invalid))
                if (readable) db.enqueue(mapOf("id" to "old")) else db.enqueue()
                assertThrows(IllegalStateException::class.java) {
                    RotationBackfill(db, KeyRegistry(db), keys, KeyRotationPlan("old", "new", KeyPurpose.PII)).run(1)
                }
                assertEquals(0, db.updates)
            }
        }
    }

    @Test
    fun `lookup backfill verifies target alias digest and refuses a colliding alias`() {
        for (matches in listOf(true, false)) {
            fixture(KeyPurpose.EMAIL_LOOKUP) { db, keys ->
                val identity = IdentityRules().identity("Person", "person@example.test")
                val envelope =
                    AeadCipher().encrypt(
                        "pii",
                        oldBytes,
                        ByteArray(12),
                        ProtectionBinding("core", "user_account", id, KeyPurpose.PII),
                        AccountPayload().encode(identity),
                    )
                val digest =
                    Mac.getInstance("HmacSHA256").also { it.init(SecretKeySpec(newBytes, "HmacSHA256")) }.doFinal(
                        (
                            "email-lookup-v1\u0000" +
                                identity.emailNormalized
                        ).toByteArray(),
                    )
                db.enqueue(row(envelope))
                db.enqueue(mapOf("id" to "pii"))
                db.enqueue(mapOf("id" to "new", "state" to "WRITE"))
                db.enqueue(emptyMap())
                db.enqueue(mapOf("digest" to if (matches) digest else ByteArray(32)))
                if (matches) db.enqueue(mapOf("total" to 0L))
                val backfill = RotationBackfill(db, KeyRegistry(db), keys, KeyRotationPlan("old", "new", KeyPurpose.EMAIL_LOOKUP))
                if (matches) {
                    val result = backfill.run(1)
                    assertTrue(result.ok)
                    assertEquals(1, result.backfilled)
                } else {
                    assertThrows(IllegalStateException::class.java) { backfill.run(1) }
                }
            }
        }
    }

    private fun row(envelope: ProtectedEnvelope): Map<String, Any?> =
        mapOf(
            "id" to id,
            "version" to 5L,
            "key_id" to envelope.keyId,
            "nonce" to envelope.nonce(),
            "cipher" to envelope.cipher(),
        )

    private fun fixture(
        purpose: KeyPurpose,
        test: (SessionJdbcFixture, MountedKeys) -> Unit,
    ) {
        val old = root.resolve("old-$purpose")
        Files.write(old, oldBytes)
        val next = root.resolve("new-$purpose")
        Files.write(next, newBytes)
        listOf(old, next).forEach { Files.setPosixFilePermissions(it, PosixFilePermissions.fromString("rw-------")) }
        val mounts =
            if (purpose ==
                KeyPurpose.EMAIL_LOOKUP
            ) {
                mapOf("pii" to KeyMount(KeyPurpose.PII, old), "new" to KeyMount(purpose, next))
            } else {
                mapOf("old" to KeyMount(purpose, old), "new" to KeyMount(purpose, next))
            }
        MountedKeys(mounts, null).use { test(SessionJdbcFixture(emptyList()), it) }
    }
}
