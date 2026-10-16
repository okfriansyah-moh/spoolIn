package com.spoolin.core.boundary.admission

import com.spoolin.core.features.session.IdentityHttpFixture
import com.spoolin.core.recovery.crypto.CryptoFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CoreReplicaAdmissionTest {
    @Test
    fun `two real Core HTTPS replicas share one remaining slot and full application restart retains quota`() {
        CryptoFixture("t009-core-replica.json").use { crypto ->
            val settings = mapOf("RATE_LIMIT_READ_MAX_REQUESTS" to "5", "RATE_LIMIT_READ_WINDOW_SECONDS" to "86400")
            replicaClaims(crypto, settings)
            IdentityHttpFixture(false, true, settings, crypto).use { restarted ->
                assertEquals(429, restarted.request("""{"query":"query { policy { revision } }"}""").statusCode())
            }
            assertEquals(5, crypto.scalar("SELECT sum(hits) FROM appdata.request_rate_window"))
        }
    }

    private fun replicaClaims(
        crypto: CryptoFixture,
        settings: Map<String, String>,
    ) {
        IdentityHttpFixture(false, true, settings, crypto).use { first ->
            IdentityHttpFixture(false, true, settings, crypto).use { second -> compete(first, second, crypto) }
        }
    }

    private fun compete(
        first: IdentityHttpFixture,
        second: IdentityHttpFixture,
        crypto: CryptoFixture,
    ) {
        val body = """{"query":"query { policy { revision } }"}"""
        repeat(4) { assertEquals(200, first.request(body).statusCode()) }
        val executor = Executors.newFixedThreadPool(8)
        val barrier = CyclicBarrier(8)
        val futures =
            (0..7).map { index ->
                executor.submit<Int> {
                    requestAtBarrier(first, second, index, body, barrier)
                }
            }
        val results =
            try {
                futures.map { it.get(15, TimeUnit.SECONDS) }
            } finally {
                executor.shutdownNow()
            }
        assertEquals(1, results.count { it == 200 })
        assertEquals(7, results.count { it == 429 })
        assertEquals(5, crypto.scalar("SELECT sum(hits) FROM appdata.request_rate_window"))
        assertEquals(0, crypto.scalar("SELECT count(*) FROM appdata.operation_receipt"))
    }

    private fun requestAtBarrier(
        first: IdentityHttpFixture,
        second: IdentityHttpFixture,
        index: Int,
        body: String,
        barrier: CyclicBarrier,
    ): Int {
        barrier.await(10, TimeUnit.SECONDS)
        return (if (index % 2 == 0) first else second).request(body).statusCode()
    }

    @Test
    fun `divergent actual resolved replica profile cannot become ready under existing protected agreement`() {
        CryptoFixture("t009-core-profile-mismatch.json").use { crypto ->
            val firstSettings = mapOf("RATE_LIMIT_READ_MAX_REQUESTS" to "5")
            IdentityHttpFixture(false, true, firstSettings, crypto).use { first ->
                assertThrows(Exception::class.java) {
                    IdentityHttpFixture(false, true, mapOf("RATE_LIMIT_READ_MAX_REQUESTS" to "6"), crypto).close()
                }
                assertEquals(200, first.request("""{"query":"query { policy { revision } }"}""").statusCode())
                assertEquals(1, crypto.scalar("SELECT sum(hits) FROM appdata.request_rate_window"))
            }
        }
    }
}
