package com.spoolin.core.boundary.admission

import com.spoolin.core.features.crypto.executeBound
import com.spoolin.core.features.ratelimit.RateMaintenance
import com.spoolin.core.features.session.IdentityHttpFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

class RateMaintenanceTest {
    @Test
    fun `purge removes only expired windows and changed agreement fails live readiness and admission`() {
        IdentityHttpFixture(false, true).use { fixture ->
            val maintenance = fixture.context.getBean(RateMaintenance::class.java)
            val body = """{"query":"query { policy { revision } }"}"""
            assertEquals(200, fixture.request(body).statusCode())
            fixture.crypto.owner.connection("core").use { connection ->
                connection.executeBound(
                    "INSERT INTO appdata.request_rate_window(bucket_key,window_start,hits,expires_at) " +
                        "VALUES (decode(repeat('00',32),'hex'),clock_timestamp()-interval '2 hours',1," +
                        "clock_timestamp()-interval '1 hour')",
                )
            }
            assertTrue(maintenance.ready())
            assertEquals(1, maintenance.purge())
            assertEquals(1, fixture.crypto.scalar("SELECT count(*) FROM appdata.request_rate_window"))
            Files.writeString(
                fixture.crypto.owner.root
                    .resolve("rate-agreement.json"),
                "{}",
            )
            assertFalse(maintenance.ready())
            assertEquals(503, fixture.request(body).statusCode())
            assertEquals(1, fixture.crypto.scalar("SELECT sum(hits) FROM appdata.request_rate_window"))
            assertEquals(0, fixture.crypto.scalar("SELECT count(*) FROM appdata.operation_receipt"))
        }
    }
}
