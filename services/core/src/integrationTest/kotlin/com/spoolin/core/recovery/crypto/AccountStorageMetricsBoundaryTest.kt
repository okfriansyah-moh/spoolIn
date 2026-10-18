package com.spoolin.core.recovery.crypto

import com.spoolin.core.features.account.persistence.AccountInsert
import com.spoolin.core.features.account.persistence.AccountPayload
import com.spoolin.core.features.account.persistence.AccountRole
import com.spoolin.core.features.account.persistence.IdentityRules
import com.spoolin.core.features.account.persistence.PreparedAccount
import com.spoolin.core.features.account.persistence.ProtectedAccounts
import com.spoolin.core.features.crypto.AccountCryptography
import com.spoolin.core.features.crypto.AeadCipher
import com.spoolin.core.features.crypto.ProtectedRead
import com.spoolin.core.features.crypto.ProtectionMetrics
import com.spoolin.core.features.crypto.ProtectionUnavailable
import com.spoolin.core.features.crypto.StoredPasswordHash
import com.spoolin.core.features.crypto.executeBound
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.sql.SQLException
import java.util.UUID

class AccountStorageMetricsBoundaryTest {
    private val identity = IdentityRules().identity("Synthetic account", "synthetic@example.invalid")
    private val password = StoredPasswordHash("isolated-fixture-not-used-for-sign-in")

    @Test
    fun `stale prepared account write key fails the actual insertion boundary without storage effects`() {
        CryptoFixture("account-storage-metrics-cas.json").use { fixture ->
            SimpleMeterRegistry().useRegistry { metrics ->
                val accounts = accounts(fixture, metrics)
                val id = UUID.randomUUID()
                val prepared = accounts.prepare(id, identity, password, AccountRole.VIEWER)
                val originalCipher = prepared.envelope.cipher()
                fixture.owner.connection("core").use { connection ->
                    connection.executeBound("UPDATE appdata.crypto_key_version SET state='READ' WHERE id='pii-write'")
                    connection.executeBound("UPDATE appdata.crypto_key_version SET state='WRITE' WHERE id='pii-old'")
                }
                unavailableInsertion(fixture, accounts, prepared)
                assertArrayEquals(originalCipher, prepared.envelope.cipher())
                assertEquals(1.0, count(metrics, "KEY"))
                assertEquals(0L, fixture.scalar("SELECT count(*) FROM appdata.user_account"))
                assertEquals(0L, fixture.scalar("SELECT count(*) FROM appdata.account_email_lookup"))
            }
        }
    }

    @Test
    fun `missing active lookup key keeps public unavailable results distinct from ordinary missing credentials`() {
        CryptoFixture("account-storage-metrics-lookup.json").use { fixture ->
            SimpleMeterRegistry().useRegistry { metrics ->
                val accounts = accounts(fixture, metrics)
                assertSame(ProtectedRead.Unavailable, accounts.credential("absent@example.invalid"))
                assertTrue(metrics.meters.isEmpty())
                fixture.owner.connection("core").use {
                    it.executeBound("UPDATE appdata.crypto_key_version SET state='READ' WHERE id='email-write'")
                }
                assertEquals(AccountInsert.Unavailable, accounts.insert(UUID.randomUUID(), identity, password, AccountRole.VIEWER))
                assertEquals(1.0, count(metrics, "KEY"))
                fixture.owner.connection("core").use {
                    it.executeBound("UPDATE appdata.crypto_key_version SET state='RETIRED' WHERE purpose='EMAIL_LOOKUP'")
                }
                assertSame(ProtectedRead.Unavailable, accounts.credential(identity.emailNormalized))
                assertEquals(2.0, count(metrics, "KEY"))
                assertEquals(0L, fixture.scalar("SELECT count(*) FROM appdata.user_account"))
            }
        }
    }

    @Test
    fun `key validation on an unavailable connection still throws SQL rather than inventing a domain rejection`() {
        CryptoFixture("account-storage-metrics-database.json").use { fixture ->
            SimpleMeterRegistry().useRegistry { metrics ->
                val accounts = accounts(fixture, metrics)
                val prepared = accounts.prepare(UUID.randomUUID(), identity, password, AccountRole.VIEWER)
                val closed = fixture.pool.connection
                closed.close()
                assertThrows(SQLException::class.java) { accounts.insert(closed, prepared) }
                assertEquals(1.0, count(metrics, "UNAVAILABLE"))
                assertEquals(0L, fixture.scalar("SELECT count(*) FROM appdata.user_account"))
            }
        }
    }

    @Test
    fun `authenticated but malformed stored identity retains unavailable read and original alert`() {
        CryptoFixture("account-storage-metrics-payload.json").use { fixture ->
            SimpleMeterRegistry().useRegistry { metrics ->
                val id = UUID.randomUUID()
                // Controlled corruption uses valid AEAD so this test reaches the persisted payload decoder.
                val malformed = fixture.crypto.encrypt(id, "{}".toByteArray())
                fixture.owner.connection("core").use {
                    it.executeBound(
                        "INSERT INTO appdata.user_account(id,pii_key_id,pii_nonce,pii_cipher,password_hash,role) " +
                            "VALUES(?,?,?,?,?,'VIEWER')",
                        id,
                        malformed.keyId,
                        malformed.nonce(),
                        malformed.cipher(),
                        password.encoded,
                    )
                }
                assertSame(ProtectedRead.Unavailable, accounts(fixture, metrics).identity(id))
                assertEquals(1.0, count(metrics, "UNAVAILABLE"))
                assertEquals(listOf(id), fixture.alerts)
            }
        }
    }

    private fun unavailableInsertion(
        fixture: CryptoFixture,
        accounts: ProtectedAccounts,
        prepared: PreparedAccount,
    ) {
        fixture.pool.connection.use { connection ->
            connection.autoCommit = false
            try {
                val failure = assertThrows(ProtectionUnavailable::class.java) { accounts.insert(connection, prepared) }
                assertEquals("Protected data is unavailable", failure.message)
            } finally {
                connection.rollback()
            }
        }
    }

    private fun accounts(
        fixture: CryptoFixture,
        metrics: SimpleMeterRegistry,
    ): ProtectedAccounts {
        val observer = ProtectionMetrics(metrics).observing(fixture.alert)
        val crypto = AccountCryptography(fixture.registry, fixture.keys, fixture.allocator, AeadCipher(), observer)
        return ProtectedAccounts(fixture.pool, fixture.registry, crypto, AccountPayload(), observer)
    }

    private fun count(
        registry: SimpleMeterRegistry,
        code: String,
    ): Double =
        registry
            .get("spoolin_crypto_failures_total")
            .tag("code", code)
            .counter()
            .count()
}

private inline fun <R> SimpleMeterRegistry.useRegistry(action: (SimpleMeterRegistry) -> R): R =
    try {
        action(this)
    } finally {
        close()
    }
