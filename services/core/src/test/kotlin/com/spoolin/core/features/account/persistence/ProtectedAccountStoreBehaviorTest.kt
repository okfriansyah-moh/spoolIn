package com.spoolin.core.features.account.persistence

import com.spoolin.core.features.crypto.AccountCryptography
import com.spoolin.core.features.crypto.AeadCipher
import com.spoolin.core.features.crypto.KeyMount
import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.KeyRegistry
import com.spoolin.core.features.crypto.MountedKeys
import com.spoolin.core.features.crypto.NonceAllocator
import com.spoolin.core.features.crypto.ProtectedEnvelope
import com.spoolin.core.features.crypto.ProtectedRead
import com.spoolin.core.features.crypto.ProtectionAlert
import com.spoolin.core.features.crypto.ProtectionBinding
import com.spoolin.core.features.crypto.ProtectionUnavailable
import com.spoolin.core.features.crypto.StoredPasswordHash
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

class ProtectedAccountStoreBehaviorTest {
    @TempDir lateinit var root: Path
    private val id = UUID.randomUUID()
    private val identity = IdentityRules().identity("山田", "Person@example.test")
    private val password = StoredPasswordHash("stored-password-hash")
    private val keyBytes = ByteArray(32) { 17 }
    private val payload = AccountPayload()

    @Test
    fun `prepare and insert keep identity encrypted while returning only public account projection`() =
        fixture { db, accounts ->
            db.enqueue(mapOf("id" to "pii"))
            db.enqueue(mapOf("allocated" to 1L))
            val prepared = accounts.prepare(id, identity, password, AccountRole.VIEWER)
            assertFalse(prepared.toString().contains(identity.emailNormalized))
            db.enqueue(mapOf("id" to "pii"))
            db.enqueue(mapOf("id" to "lookup", "state" to "WRITE"))
            val projection = accounts.insert(db.jdbc, prepared)
            assertEquals(AccountProjection(id, AccountRole.VIEWER, 0), projection)
            assertEquals(2, db.updates)
        }

    @Test
    fun `stored identity decrypts exact original values and missing or damaged identity stays unavailable`() =
        fixture { db, accounts ->
            val envelope = envelope(payload.encode(identity))
            db.enqueue(record(envelope))
            db.enqueue(mapOf("id" to "pii"))
            val read = accounts.identity(id) as ProtectedRead.Value
            assertEquals(identity.name, read.value.name)
            assertEquals(identity.emailDisplay, read.value.emailDisplay)
            assertEquals(identity.emailNormalized, read.value.emailNormalized)
            db.enqueue()
            assertEquals(ProtectedRead.Unavailable, accounts.identity(id))
            db.enqueue(record(envelope("invalid json".toByteArray())))
            db.enqueue(mapOf("id" to "pii"))
            assertEquals(ProtectedRead.Unavailable, accounts.identity(id))
        }

    @Test
    fun `credential lookup deduplicates key aliases and refuses ambiguous distinct accounts`() =
        fixture { db, accounts ->
            db.enqueue(mapOf("id" to "lookup", "state" to "WRITE"))
            db.enqueue(mapOf("id" to id, "password_hash" to password.encoded, "auth_epoch" to 4L))
            val credential = (accounts.credential(identity.emailNormalized) as ProtectedRead.Value).value
            assertEquals(id, credential.id)
            assertEquals(4L, credential.authEpoch)
            assertEquals(password.encoded, credential.password.encoded)
            db.enqueue(mapOf("id" to "lookup", "state" to "WRITE"))
            db.enqueue(
                mapOf("id" to id, "password_hash" to password.encoded, "auth_epoch" to 4L),
                mapOf(
                    "id" to UUID.randomUUID(),
                    "password_hash" to password.encoded,
                    "auth_epoch" to 4L,
                ),
            )
            assertEquals(ProtectedRead.Unavailable, accounts.credential(identity.emailNormalized))
            db.enqueue()
            assertEquals(ProtectedRead.Unavailable, accounts.credential(identity.emailNormalized))
        }

    @Test
    fun `password replacement returns false for stale authority and true only for one changed row`() =
        fixture { db, accounts ->
            assertTrue(accounts.replacePassword(id, password, StoredPasswordHash("replacement")))
            db.updateResult = 0
            assertFalse(accounts.replacePassword(id, password, StoredPasswordHash("replacement")))
        }

    @Test
    fun `inconsistent normalized identity and read only lookup registry cannot create account`() =
        fixture { db, accounts ->
            assertThrows(IllegalArgumentException::class.java) {
                accounts.prepare(
                    id,
                    AccountIdentity(identity.name, identity.emailDisplay, "other@example.test"),
                    password,
                    AccountRole.VIEWER,
                )
            }
            db.enqueue(mapOf("id" to "pii"))
            db.enqueue(mapOf("allocated" to 1L))
            val prepared = accounts.prepare(id, identity, password, AccountRole.VIEWER)
            db.enqueue(mapOf("id" to "pii"))
            db.enqueue(mapOf("id" to "lookup", "state" to "READ"))
            assertThrows(ProtectionUnavailable::class.java) { accounts.insert(db.jdbc, prepared) }
            assertEquals(0, db.updates)
        }

    private fun envelope(bytes: ByteArray) =
        AeadCipher().encrypt(
            "pii",
            keyBytes,
            ByteArray(12) {
                2
            },
            ProtectionBinding("core", "user_account", id, KeyPurpose.PII),
            bytes,
        )

    private fun record(envelope: ProtectedEnvelope) =
        mapOf(
            "pii_key_id" to "pii",
            "pii_nonce" to envelope.nonce(),
            "pii_cipher" to envelope.cipher(),
        )

    private fun fixture(test: (SessionJdbcFixture, ProtectedAccounts) -> Unit) {
        val pii = root.resolve("pii")
        Files.write(pii, keyBytes)
        val lookup = root.resolve("lookup")
        Files.write(lookup, ByteArray(32) { 19 })
        listOf(pii, lookup).forEach { Files.setPosixFilePermissions(it, PosixFilePermissions.fromString("rw-------")) }
        val db = SessionJdbcFixture(emptyList())
        val alert = ProtectionAlert { }
        val mounts =
            mapOf(
                "pii" to KeyMount(KeyPurpose.PII, pii),
                "lookup" to KeyMount(KeyPurpose.EMAIL_LOOKUP, lookup),
            )
        MountedKeys(mounts, null).use { keys ->
            val registry = KeyRegistry(db)
            val crypto = AccountCryptography(registry, keys, NonceAllocator(db), AeadCipher(), alert)
            test(db, ProtectedAccounts(db, registry, crypto, payload, alert))
        }
    }
}
