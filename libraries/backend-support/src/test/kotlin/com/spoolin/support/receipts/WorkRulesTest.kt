package com.spoolin.support.receipts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.time.Instant
import java.util.UUID

class WorkRulesTest {
    private val parent = UUID.fromString("95c65556-47e7-4150-a739-9ad8d065495e")
    private val work = WorkRequest("followup", "SYNC", parent, Instant.parse("2026-10-20T00:00:00Z"))

    @Test
    fun `new child UUID uses SHA256 version eight namespace and exact step`() {
        assertEquals(UUID.fromString("4e75ca89-bd9d-83f7-9011-978879815e46"), work.id(parent))
        assertEquals(8, work.id(parent).version())
        assertEquals(2, work.id(parent).variant())
        assertEquals(work.id(parent), work.id(parent))
        assertNotEquals(work.id(parent), work.copy(step = "other").id(parent))
        assertNotEquals(work.id(parent), work.id(UUID.randomUUID()))
        assertEquals(3, work.maxAttempts)
        assertEquals(500, work.baseDelayMs)
        assertEquals(5000, work.capDelayMs)
        assertEquals(2, work.multiplier)
    }

    @TestFactory
    fun `work snapshots reject out of bounds and untyped handlers`() =
        listOf<Pair<String, () -> WorkRequest>>(
            "attempt zero" to { work.copy(maxAttempts = 0) },
            "attempt eleven" to { work.copy(maxAttempts = 11) },
            "base zero" to { work.copy(baseDelayMs = 0) },
            "base above cap" to { work.copy(baseDelayMs = 60001) },
            "cap zero" to { work.copy(capDelayMs = 0) },
            "cap above bound" to { work.copy(capDelayMs = 3600001) },
            "multiplier zero" to { work.copy(multiplier = 0) },
            "multiplier above bound" to { work.copy(multiplier = 11) },
            "inverted delays" to { work.copy(capDelayMs = 1) },
            "untyped handler" to { work.copy(kind = "execute arbitrary command") },
            "notification requires separate typed evidence" to { work.copy(kind = "NOTIFICATION") },
            "empty stable step" to { work.copy(step = "") },
        ).map { (name, construct) ->
            DynamicTest.dynamicTest(name) { assertThrows(IllegalArgumentException::class.java) { construct() } }
        }
}
