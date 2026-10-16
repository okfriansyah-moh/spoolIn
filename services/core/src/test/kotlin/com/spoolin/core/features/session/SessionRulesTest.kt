package com.spoolin.core.features.session

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.time.Instant

class SessionRulesTest {
    private val created = Instant.parse("2026-10-20T00:00:00Z")

    @TestFactory
    fun `idle and absolute eligibility ends at exact cutoffs before purge`() =
        listOf("idle", "absolute").flatMap { kind ->
            val cutoff = created.plusSeconds(if (kind == "idle") 1800 else 43200)
            val seen = if (kind == "idle") created else cutoff.minusSeconds(60)
            listOf(-1L, 0L, 1L).map { micros ->
                DynamicTest.dynamicTest("$kind at $micros microseconds") {
                    val window = SessionWindow(created, seen)
                    assertEquals(micros < 0, window.eligible(cutoff.plusNanos(micros * 1000), 7, 7))
                }
            }
        }

    @Test
    fun `revocation and account epoch changes cannot be restored by a cookie`() {
        assertFalse(SessionWindow(created, created, revoked = created).eligible(created, 7, 7))
        assertFalse(SessionWindow(created, created).eligible(created, 7, 8))
        assertFalse(SessionWindow(created, created).shouldTouch(created.plusSeconds(59)))
        assertEquals(true, SessionWindow(created, created).shouldTouch(created.plusSeconds(60)))
    }
}
