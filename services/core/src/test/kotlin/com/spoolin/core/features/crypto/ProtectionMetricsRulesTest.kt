package com.spoolin.core.features.crypto

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.security.GeneralSecurityException
import java.sql.SQLException

class ProtectionMetricsRulesTest {
    @Test
    fun `only actual failures create series with the two approved labels`() {
        SimpleMeterRegistry().useRegistry { registry ->
            val alert = ProtectionMetrics(registry).observing(ProtectionAlert { })
            assertTrue(registry.meters.isEmpty())
            ProtectionFailure.entries.forEach { alert.recordFailure(it) }
            assertEquals(4, registry.meters.size)
            registry.meters.forEach { meter ->
                assertEquals(
                    setOf("service", "code"),
                    meter.id.tags
                        .map { it.key }
                        .toSet(),
                )
                assertEquals("core", meter.id.getTag("service"))
                assertTrue(meter.id.getTag("code") in ProtectionFailure.entries.map { it.name })
            }
        }
    }

    @Test
    fun `observations preserve exact original failures and distinguish database unavailability`() {
        SimpleMeterRegistry().useRegistry { registry ->
            val alert = ProtectionMetrics(registry).observing(ProtectionAlert { })
            val database = SQLException("synthetic connection failure")
            assertSame(
                database,
                assertThrows(SQLException::class.java) {
                    observeProtection(alert, ProtectionFailure.KEY) { throw database }
                },
            )
            val unavailable = ProtectionUnavailable()
            assertSame(
                unavailable,
                assertThrows(ProtectionUnavailable::class.java) {
                    observeProtection(alert, ProtectionFailure.KEY) { throw unavailable }
                },
            )
            val engine = GeneralSecurityException("synthetic engine failure")
            assertSame(
                engine,
                assertThrows(GeneralSecurityException::class.java) {
                    observeProtection(alert, ProtectionFailure.UNAVAILABLE) { throw engine }
                },
            )
            assertEquals(
                2.0,
                registry
                    .get("spoolin_crypto_failures_total")
                    .tag("code", "UNAVAILABLE")
                    .counter()
                    .count(),
            )
            assertEquals(
                1.0,
                registry
                    .get("spoolin_crypto_failures_total")
                    .tag("code", "KEY")
                    .counter()
                    .count(),
            )
        }
    }

    @Test
    fun `input validation remains its original rejection and is not reported as a crypto fault`() {
        SimpleMeterRegistry().useRegistry { registry ->
            val alert = ProtectionMetrics(registry).observing(ProtectionAlert { })
            val invalid = IllegalArgumentException("synthetic invalid caller input")
            assertSame(
                invalid,
                assertThrows(IllegalArgumentException::class.java) {
                    observeProtection(alert, ProtectionFailure.UNAVAILABLE) { throw invalid }
                },
            )
            assertTrue(registry.meters.isEmpty())
        }
    }

    @Test
    fun `AEAD reports authentication and malformed key classes while retaining unavailable envelopes`() {
        val cipher = AeadCipher()
        val key = ByteArray(32) { 1 }
        val binding = ProtectionBinding("core", "user_account", java.util.UUID.randomUUID(), KeyPurpose.PII)
        val valid = cipher.encrypt("synthetic-key", key, ByteArray(12), binding, byteArrayOf(1, 2))
        val damaged = valid.cipher().also { it[0] = (it[0].toInt() xor 1).toByte() }
        val corrupt = ProtectedEnvelope(1, valid.keyId, valid.nonce(), damaged)
        val codes = mutableListOf<ProtectionFailure>()
        assertSame(ProtectedRead.Unavailable, cipher.decrypt(corrupt, key, binding) { codes.add(it) })
        assertSame(ProtectedRead.Unavailable, cipher.decrypt(valid, ByteArray(31), binding) { codes.add(it) })
        val invalidVersion = ProtectedEnvelope(2, valid.keyId, valid.nonce(), valid.cipher())
        assertSame(ProtectedRead.Unavailable, cipher.decrypt(invalidVersion, key, binding) { codes.add(it) })
        assertEquals(listOf(ProtectionFailure.AUTHENTICATION, ProtectionFailure.KEY, ProtectionFailure.AUTHENTICATION), codes)
        assertSame(ProtectedRead.Unavailable, cipher.decrypt(corrupt, key, binding) { error("synthetic observer failure") })
    }

    @Test
    fun `metric failure cannot replace an original exception or a successful result`() {
        val broken =
            object : ProtectionAlert {
                override fun unavailable(rowId: java.util.UUID) = Unit

                override fun failed(code: ProtectionFailure): Unit = error("synthetic metric failure")
            }
        val original = ProtectionUnavailable(ProtectionFailure.NONCE_EXHAUSTION)
        assertSame(
            original,
            assertThrows(ProtectionUnavailable::class.java) {
                observeProtection(broken, ProtectionFailure.KEY) { throw original }
            },
        )
        assertEquals("actual result", observeProtection(broken, ProtectionFailure.KEY) { "actual result" })
        val bytes = ByteArray(32) { 1 }
        assertSame(ProtectedRead.Unavailable, decryptProtected(broken, bytes) { ProtectedRead.Unavailable })
        assertArrayEquals(ByteArray(32), bytes)
        assertSame(ProtectedRead.Unavailable, decryptProtected(broken, null) { error("Key must not be used") })
    }
}

private inline fun <R> SimpleMeterRegistry.useRegistry(action: (SimpleMeterRegistry) -> R): R =
    try {
        action(this)
    } finally {
        close()
    }
