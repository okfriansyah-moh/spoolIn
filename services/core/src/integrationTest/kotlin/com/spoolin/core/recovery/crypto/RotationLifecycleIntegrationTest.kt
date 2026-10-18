package com.spoolin.core.recovery.crypto

import com.spoolin.core.features.account.persistence.AccountInsert
import com.spoolin.core.features.account.persistence.AccountRole
import com.spoolin.core.features.account.persistence.IdentityRules
import com.spoolin.core.features.crypto.AeadCipher
import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.ProtectedEnvelope
import com.spoolin.core.features.crypto.ProtectedRead
import com.spoolin.core.features.crypto.StoredPasswordHash
import com.spoolin.core.features.crypto.committed
import com.spoolin.core.features.crypto.executeBound
import com.spoolin.core.features.crypto.queryBound
import com.spoolin.core.features.draft.DraftCryptography
import com.spoolin.core.operations.crypto.KeyRotationPlan
import com.spoolin.core.operations.crypto.RotationRetirement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/** ATDD-182/183: real owner rows, committed nonce allocation, interruption and protected aliases. */
class RotationLifecycleIntegrationTest {
    @Test
    fun `101 account and draft rows resume across key switches without losing values or dual aliases`() {
        CryptoFixture("rotation-lifecycle.json").use { fixture ->
            val accounts =
                (1..101).map { index ->
                    val id = UUID.randomUUID()
                    assertInstanceOf(
                        AccountInsert.Stored::class.java,
                        fixture.accounts.insert(
                            id,
                            IdentityRules().identity("Rotation Person $index", "rotation-$index@example.test"),
                            StoredPasswordHash("synthetic-unusable"),
                            AccountRole.MAINTAINER,
                        ),
                    )
                    id to index
                }
            val drafts = prepareDrafts(fixture, accounts)
            fixture.backupCheckpoint("rotation-before")
            val pii = KeyRotationPlan("pii-write", "pii-old", KeyPurpose.PII)
            val interrupted = pii.rotate(fixture.pool, fixture.registry, false, fixture.keys, maxBatches = 1)
            assertFalse(interrupted.ok)
            assertEquals(100, interrupted.backfilled)
            assertEquals(1L, interrupted.remaining)
            assertFalse(RotationRetirement(fixture.pool).retire("pii-write", KeyPurpose.PII).ok)
            val resumed = pii.rotate(fixture.pool, fixture.registry, false, fixture.keys)
            assertTrue(resumed.ok)
            assertEquals(0L, resumed.remaining)
            accounts.forEach { (id, index) ->
                val identity =
                    assertInstanceOf(
                        ProtectedRead.Value::class.java,
                        fixture.accounts.identity(id),
                    ).value as com.spoolin.core.features.account.persistence.AccountIdentity
                assertEquals("Rotation Person $index", identity.name)
                assertEquals("rotation-$index@example.test", identity.emailNormalized)
            }
            val draft = KeyRotationPlan("draft-write", "draft-old", KeyPurpose.DRAFT)
            assertEquals(1L, draft.rotate(fixture.pool, fixture.registry, false, fixture.keys, maxBatches = 1).remaining)
            assertTrue(draft.rotate(fixture.pool, fixture.registry, false, fixture.keys).ok)
            verifyDrafts(fixture, drafts)
            // Controlled legacy setup: accounts predate registration of the target lookup epoch.
            fixture.pool.committed { it.executeBound("DELETE FROM appdata.account_email_lookup WHERE key_id='email-old'") }
            val lookup = KeyRotationPlan("email-write", "email-old", KeyPurpose.EMAIL_LOOKUP)
            val partial = lookup.rotate(fixture.pool, fixture.registry, false, fixture.keys, maxBatches = 1)
            assertEquals(1L, partial.remaining)
            val duplicate =
                fixture.accounts.insert(
                    UUID.randomUUID(),
                    IdentityRules().identity("Duplicate", "ROTATION-1@EXAMPLE.TEST"),
                    StoredPasswordHash("synthetic-unusable"),
                    AccountRole.VIEWER,
                )
            assertEquals(AccountInsert.Conflict, duplicate)
            assertTrue(lookup.rotate(fixture.pool, fixture.registry, false, fixture.keys).ok)
            assertEquals(202L, fixture.scalar("SELECT count(*) FROM appdata.account_email_lookup"))
            assertFalse(RotationRetirement(fixture.pool).retire("email-write", KeyPurpose.EMAIL_LOOKUP).ok)
            assertEquals(101L, fixture.scalar("SELECT count(*) FROM appdata.user_account"))
            assertEquals(0L, fixture.scalar("SELECT count(*) FROM appdata.crypto_key_version WHERE state='RETIRED'"))
            fixture.observe(
                "t040",
                "resume",
                mapOf(
                    "accounts" to 101,
                    "drafts" to 101,
                    "aliases" to 202,
                    "backfillResumed" to true,
                    "retirementBlockedWithoutBackupProof" to true,
                ),
            )
        }
    }

    private fun prepareDrafts(
        fixture: CryptoFixture,
        accounts: List<Pair<UUID, Int>>,
    ): List<Pair<UUID, Int>> {
        val crypto = DraftCryptography(fixture.registry, fixture.keys, fixture.allocator, AeadCipher())
        return accounts.map { (account, index) ->
            val id = UUID.randomUUID()
            val clear = "{\"title\":\"Draft $index\"}".toByteArray()
            val envelope =
                try {
                    crypto.encrypt(id, clear)
                } finally {
                    clear.fill(0)
                }
            fixture.pool.committed { connection ->
                connection.executeBound(
                    "INSERT INTO appdata.form_draft(id,account_id,target_kind,policy_revision," +
                        "body_key_id,body_nonce,body_cipher,expires_at) " +
                        "VALUES(?,?,'MOVIE','policy',?,?,?,?)",
                    id,
                    account,
                    envelope.keyId,
                    envelope.nonce(),
                    envelope.cipher(),
                    Timestamp.from(Instant.now().plusSeconds(3600)),
                )
            }
            id to index
        }
    }

    private fun verifyDrafts(
        fixture: CryptoFixture,
        drafts: List<Pair<UUID, Int>>,
    ) {
        val crypto = DraftCryptography(fixture.registry, fixture.keys, fixture.allocator, AeadCipher())
        drafts.forEach { (id, index) ->
            val envelope =
                fixture.pool.connection.use { connection ->
                    connection
                        .queryBound("SELECT body_key_id,body_nonce,body_cipher FROM appdata.form_draft WHERE id=?", listOf(id)) {
                            ProtectedEnvelope(1, it.getString("body_key_id"), it.getBytes("body_nonce"), it.getBytes("body_cipher"))
                        }.single()
                }
            val read = crypto.decrypt(id, envelope)
            assertTrue(read is ProtectedRead.Value)
            if (read is ProtectedRead.Value) {
                try {
                    assertEquals("{\"title\":\"Draft $index\"}", read.value.toString(Charsets.UTF_8))
                } finally {
                    read.value.fill(0)
                }
            }
        }
    }

    @Test
    fun `running key provider loads newly staged IDs and rejects changed material for existing versions`() {
        CryptoFixture("rotation-live-map.json").use { fixture ->
            val map = fixture.owner.root.resolve("rotation-keys.json")
            val next = fixture.owner.root.resolve("pii-next")
            val material = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }
            java.nio.file.Files
                .write(next, material)
            java.nio.file.Files
                .setPosixFilePermissions(
                    next,
                    java.nio.file.attribute.PosixFilePermissions
                        .fromString("rw-------"),
                )
            val mounts =
                fixture.mounts + (
                    "pii-next" to
                        com.spoolin.core.features.crypto
                            .KeyMount(KeyPurpose.PII, next)
                )
            val json =
                tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsBytes(
                    mounts.mapValues { (_, value) ->
                        mapOf("purpose" to value.purpose.name, "file" to value.file.toString())
                    },
                )
            java.nio.file.Files
                .write(map, json)
            java.nio.file.Files
                .setPosixFilePermissions(
                    map,
                    java.nio.file.attribute.PosixFilePermissions
                        .fromString("rw-------"),
                )
            com.spoolin.core.features.crypto.MountedKeys(fixture.mounts, map).use { runtime ->
                val loaded = requireNotNull(runtime.key("pii-next", KeyPurpose.PII))
                try {
                    org.junit.jupiter.api.Assertions
                        .assertArrayEquals(material, loaded)
                } finally {
                    loaded.fill(0)
                }
                val replacement = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }
                java.nio.file.Files
                    .write(next, replacement)
                org.junit.jupiter.api.Assertions.assertThrows(com.spoolin.core.features.crypto.ProtectionUnavailable::class.java) {
                    runtime.key("unregistered-next", KeyPurpose.PII)
                }
                val retained = requireNotNull(runtime.key("pii-next", KeyPurpose.PII))
                try {
                    org.junit.jupiter.api.Assertions
                        .assertArrayEquals(material, retained)
                } finally {
                    retained.fill(0)
                    replacement.fill(0)
                }
            }
            material.fill(0)
        }
    }

    @Test
    fun `missing material and incompatible target fail without promoting an unverified write key`() {
        CryptoFixture("rotation-failure.json").use { fixture ->
            val before = fixture.registry.writeKey(KeyPurpose.PII).id
            assertFalse(KeyRotationPlan("pii-write", "pii-old", KeyPurpose.PII).rotate(fixture.pool, fixture.registry, false).ok)
            assertFalse(
                KeyRotationPlan("pii-write", "draft-old", KeyPurpose.PII).rotate(fixture.pool, fixture.registry, false, fixture.keys).ok,
            )
            assertEquals(before, fixture.registry.writeKey(KeyPurpose.PII).id)
        }
    }
}
