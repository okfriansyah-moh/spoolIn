package com.spoolin.core.recovery.crypto

import com.spoolin.core.features.crypto.AccountCryptography
import com.spoolin.core.features.crypto.AeadCipher
import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.MountedKeys
import com.spoolin.core.features.crypto.ProtectedEnvelope
import com.spoolin.core.features.crypto.ProtectedRead
import com.spoolin.core.features.crypto.ProtectionAlert
import com.spoolin.core.features.crypto.ProtectionFailure
import com.spoolin.core.features.crypto.ProtectionMetrics
import com.spoolin.core.features.crypto.ProtectionUnavailable
import com.spoolin.core.features.draft.DraftCryptography
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.sql.SQLException
import java.util.UUID

class ProtectionMetricsBoundaryTest {
    @Test
    fun `successful account draft and lookup protection does not create failure series`() {
        CryptoFixture("protection-metrics-success.json").use { fixture ->
            SimpleMeterRegistry().useRegistry { metrics ->
                val observer = ProtectionMetrics(metrics).observing(fixture.alert)
                val account = account(fixture, fixture.keys, observer)
                val draft = DraftCryptography(fixture.registry, fixture.keys, fixture.allocator, AeadCipher(), observer)
                val row = UUID.randomUUID()
                val plain = "isolated synthetic protected data".toByteArray()
                val envelope = account.encrypt(row, plain)
                assertArrayEquals(plain, (account.decrypt(row, envelope) as ProtectedRead.Value).value)
                val body = draft.encrypt(row, plain)
                assertArrayEquals(plain, (draft.decrypt(row, body) as ProtectedRead.Value).value)
                val key = fixture.registry.writeKey(KeyPurpose.EMAIL_LOOKUP)
                assertEquals(32, account.lookupDigest(key, "synthetic@example.invalid").size)
                assertTrue(metrics.meters.isEmpty())
            }
        }
    }

    @Test
    fun `authentication and missing keys retain unavailable reads and original account alerts`() {
        CryptoFixture("protection-metrics-authentication.json").use { fixture ->
            SimpleMeterRegistry().useRegistry { metrics ->
                val observer = ProtectionMetrics(metrics).observing(fixture.alert)
                val account = account(fixture, fixture.keys, observer)
                val draft = DraftCryptography(fixture.registry, fixture.keys, fixture.allocator, AeadCipher(), observer)
                val row = UUID.randomUUID()
                val envelope = account.encrypt(row, byteArrayOf(1, 2, 3))
                val changed = envelope.cipher().also { it[0] = (it[0].toInt() xor 1).toByte() }
                val corrupt = ProtectedEnvelope(1, envelope.keyId, envelope.nonce(), changed)
                assertSameUnavailable(account.decrypt(row, corrupt))
                val body = draft.encrypt(row, byteArrayOf(4, 5, 6))
                assertSameUnavailable(draft.decrypt(UUID.randomUUID(), body))
                assertEquals(2.0, count(metrics, "AUTHENTICATION"))
                assertEquals(listOf(row), fixture.alerts)
                missingKey(fixture, observer, metrics, row, envelope)
            }
        }
    }

    private fun missingKey(
        fixture: CryptoFixture,
        observer: ProtectionAlert,
        metrics: SimpleMeterRegistry,
        row: UUID,
        envelope: ProtectedEnvelope,
    ) {
        MountedKeys(emptyMap(), null).use { missing ->
            val account = account(fixture, missing, observer)
            val exception = assertThrows(ProtectionUnavailable::class.java) { account.encrypt(row, byteArrayOf(1)) }
            assertEquals("Protected data is unavailable", exception.message)
            assertSameUnavailable(account.decrypt(row, envelope))
            val key = fixture.registry.writeKey(KeyPurpose.EMAIL_LOOKUP)
            assertThrows(ProtectionUnavailable::class.java) { account.lookupDigest(key, "synthetic@example.invalid") }
            val draft = DraftCryptography(fixture.registry, missing, fixture.allocator, AeadCipher(), observer)
            assertThrows(ProtectionUnavailable::class.java) { draft.encrypt(row, byteArrayOf(1)) }
            assertSameUnavailable(draft.decrypt(row, envelope))
            assertEquals(5.0, count(metrics, "KEY"))
        }
    }

    @Test
    fun `database confirmed exhausted nonce counters fail writes without changing stored counters`() {
        CryptoFixture("protection-metrics-nonce.json").use { fixture ->
            SimpleMeterRegistry().useRegistry { metrics ->
                val observer = ProtectionMetrics(metrics).observing(fixture.alert)
                fixture.owner.connection("core").use { connection ->
                    connection.createStatement().use {
                        assertEquals(
                            2,
                            it.executeUpdate(
                                "UPDATE appdata.crypto_key_version SET next_nonce=9223372036854775807 " +
                                    "WHERE purpose IN ('PII','DRAFT') AND state='WRITE'",
                            ),
                        )
                    }
                }
                val missing = assertThrows(ProtectionUnavailable::class.java) {
                    fixture.allocator.reserve("unknown-key", KeyPurpose.PII)
                }
                assertEquals(ProtectionFailure.UNAVAILABLE, missing.reason)
                val account = account(fixture, fixture.keys, observer)
                val draft = DraftCryptography(fixture.registry, fixture.keys, fixture.allocator, AeadCipher(), observer)
                assertThrows(ProtectionUnavailable::class.java) { account.encrypt(UUID.randomUUID(), byteArrayOf(1)) }
                assertThrows(ProtectionUnavailable::class.java) { draft.encrypt(UUID.randomUUID(), byteArrayOf(2)) }
                assertEquals(2.0, count(metrics, "NONCE_EXHAUSTION"))
                assertEquals(
                    2L,
                    fixture.scalar(
                        "SELECT count(*) FROM appdata.crypto_key_version " +
                            "WHERE purpose IN ('PII','DRAFT') AND state='WRITE' AND next_nonce=9223372036854775807",
                    ),
                )
            }
        }
    }

    @Test
    fun `unavailable owner pool preserves the SQL error and records infrastructure failure`() {
        CryptoFixture("protection-metrics-unavailable.json").use { fixture ->
            SimpleMeterRegistry().useRegistry { metrics ->
                val observer = ProtectionMetrics(metrics).observing(fixture.alert)
                val account = account(fixture, fixture.keys, observer)
                fixture.pool.close()
                assertThrows(SQLException::class.java) { account.encrypt(UUID.randomUUID(), byteArrayOf(1)) }
                assertEquals(1.0, count(metrics, "UNAVAILABLE"))
                assertEquals(1, metrics.meters.size)
            }
        }
    }

    private fun account(
        fixture: CryptoFixture,
        keys: MountedKeys,
        alert: ProtectionAlert,
    ): AccountCryptography = AccountCryptography(fixture.registry, keys, fixture.allocator, AeadCipher(), alert)

    private fun count(
        registry: SimpleMeterRegistry,
        code: String,
    ): Double = registry.get("spoolin_crypto_failures_total").tag("code", code).counter().count()

    private fun assertSameUnavailable(result: ProtectedRead<ByteArray>) {
        assertInstanceOf(ProtectedRead.Unavailable::class.java, result)
    }
}

private inline fun <R> SimpleMeterRegistry.useRegistry(action: (SimpleMeterRegistry) -> R): R =
    try {
        action(this)
    } finally {
        close()
    }
