package com.spoolin.core.features.bootstrap

import com.spoolin.core.features.account.persistence.AccountPayload
import com.spoolin.core.features.account.persistence.AccountRole
import com.spoolin.core.features.account.persistence.IdentityRules
import com.spoolin.core.features.account.persistence.ProtectedAccounts
import com.spoolin.core.features.crypto.AccountCryptography
import com.spoolin.core.features.crypto.AeadCipher
import com.spoolin.core.features.crypto.Argon2Engine
import com.spoolin.core.features.crypto.KeyMount
import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.KeyRegistry
import com.spoolin.core.features.crypto.MountedKeys
import com.spoolin.core.features.crypto.NonceAllocator
import com.spoolin.core.features.crypto.PasswordHasher
import com.spoolin.core.features.crypto.ProtectionAlert
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

class AccountInitializationBehaviorTest {
    @TempDir lateinit var root: Path
    private val material = ByteArray(32) { 61 }
    private val payload = AccountPayload()

    @Test
    fun `initialization prepares encrypted distinct viewer and maintainer identities and commits once`() =
        fixture { db, initializer, config ->
            db.enqueue(emptyMap())
            db.enqueue()
            repeat(3) { db.enqueue() }
            repeat(2) {
                db.enqueue(mapOf("id" to "pii"))
                db.enqueue(mapOf("allocated" to (it + 1).toLong()))
            }
            repeat(2) {
                db.enqueue(mapOf("id" to "pii"))
                db.enqueue(mapOf("id" to "lookup", "state" to "WRITE"))
            }
            db.enqueue(emptyMap())
            assertTrue(initializer.initialize(config))
            assertEquals(4, db.commits)
            assertEquals(9, db.updates)
        }

    @Test
    fun `successful matching initialization replays only after validating stored role and encrypted identity`() =
        fixture { db, initializer, config ->
            val viewer = config.accounts.single { it.role == AccountRole.VIEWER }
            val maintainer = config.accounts.single { it.role == AccountRole.MAINTAINER }
            db.enqueue(emptyMap())
            db.enqueue(
                mapOf(
                    "setup_version" to config.version,
                    "setup_manifest_hash" to config.checksum(),
                    "state" to "SUCCEEDED",
                    "setup_viewer_id" to viewer.id,
                    "setup_maintainer_id" to maintainer.id,
                ),
            )
            for (account in listOf(viewer, maintainer)) {
                db.enqueue(mapOf("role" to account.role.name))
                val envelope =
                    AeadCipher().encrypt(
                        "pii",
                        material,
                        ByteArray(12) {
                            1
                        },
                        ProtectionBinding("core", "user_account", account.id, KeyPurpose.PII),
                        payload.encode(account.identity),
                    )
                db.enqueue(mapOf("pii_key_id" to "pii", "pii_nonce" to envelope.nonce(), "pii_cipher" to envelope.cipher()))
                db.enqueue(mapOf("id" to "pii"))
            }
            db.enqueue(emptyMap())
            assertFalse(initializer.initialize(config))
            assertEquals(0, db.updates)
        }

    @Test
    fun `conflicting initialization history blocks all new writes`() =
        fixture { db, initializer, config ->
            db.enqueue(emptyMap())
            db.enqueue(mapOf("setup_version" to "different"))
            db.enqueue(emptyMap())
            assertThrows(IllegalArgumentException::class.java) { initializer.initialize(config) }
            assertEquals(0, db.updates)
        }

    @Test
    fun `invalid password records durable failed initialization instead of partial accounts`() =
        fixture { db, initializer, config ->
            Files.writeString(config.accounts.first().passwordFile, "short")
            db.enqueue(emptyMap())
            db.enqueue()
            repeat(3) { db.enqueue() }
            db.enqueue(emptyMap())
            assertThrows(IllegalArgumentException::class.java) { initializer.initialize(config) }
            assertEquals(5, db.updates)
            assertEquals(2, db.commits)
        }

    private fun fixture(test: (SessionJdbcFixture, AccountInitialization, InitializationConfiguration) -> Unit) {
        val mounts =
            listOf(
                Triple("pii", KeyPurpose.PII, material),
                Triple(
                    "draft",
                    KeyPurpose.DRAFT,
                    ByteArray(32) {
                        67
                    },
                ),
                Triple("lookup", KeyPurpose.EMAIL_LOOKUP, ByteArray(32) { 71 }),
            ).associate { (id, purpose, bytes) ->
                val path = root.resolve(id)
                Files.write(path, bytes)
                protect(path)
                id to KeyMount(purpose, path)
            }
        val accounts =
            AccountRole.entries.map { role ->
                val password = root.resolve("password-$role")
                Files.writeString(password, "a valid long password for $role")
                protect(password)
                InitializationAccount(
                    UUID.randomUUID(),
                    IdentityRules().identity(role.name, "${role.name.lowercase()}@example.test"),
                    role,
                    password,
                )
            }
        val config = InitializationConfiguration("fixture-v1", accounts, mounts)
        val db = SessionJdbcFixture(emptyList())
        MountedKeys(mounts, null).use { keys ->
            val registry = KeyRegistry(db)
            val alert = ProtectionAlert { }
            val crypto = AccountCryptography(registry, keys, NonceAllocator(db), AeadCipher(), alert)
            val protectedAccounts = ProtectedAccounts(db, registry, crypto, payload, alert)
            PasswordHasher(Argon2Engine()).use { hasher -> test(db, AccountInitialization(db, protectedAccounts, hasher), config) }
        }
    }

    private fun protect(path: Path) {
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"))
    }
}
