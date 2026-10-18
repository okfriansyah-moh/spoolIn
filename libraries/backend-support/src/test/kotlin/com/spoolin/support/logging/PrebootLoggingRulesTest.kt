package com.spoolin.support.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import java.io.IOException

class PrebootLoggingRulesTest {
    private class Terminated(val status: Int) : RuntimeException()

    @Test
    fun `invalid private configuration emits fixed fatal JSON then preserves exit 78 for each owner`() {
        for (service in listOf("core", "catalog", "media")) {
            val records = mutableListOf<ByteArray>()
            val terminated = assertThrows(Terminated::class.java) {
                applyNormalizedLogLevel("private-token\nforged-event", service, { records.add(it) }) {
                    throw Terminated(it)
                }
            }
            assertEquals(78, terminated.status)
            assertEquals(1, records.size)
            val raw = records.single().toString(Charsets.UTF_8)
            val json = JsonMapper.builder().build().readTree(raw)
            assertEquals("fatal", json["level"].asText())
            assertEquals(service, json["resource"]["service.name"].asText())
            assertEquals("spoolin", json["resource"]["service.namespace"].asText())
            assertEquals("00000000000000000000000000000000", json["trace_id"].asText())
            assertEquals("0000000000000000", json["span_id"].asText())
            assertEquals("LOG_LEVEL must be info or debug", json["message"].asText())
            assertTrue(json["timestamp"].asText().endsWith("Z"))
            assertEquals(1, raw.count { it == '\n' })
            assertFalse(raw.contains("private-token") || raw.contains("forged-event"))
        }
    }

    @Test
    fun `fatal diagnostic sink failure cannot replace required configuration exit`() {
        val terminated = assertThrows(Terminated::class.java) {
            applyNormalizedLogLevel("invalid", "media", { throw IOException("private-sink-error") }) {
                throw Terminated(it)
            }
        }
        assertEquals(78, terminated.status)
    }

    @Test
    fun `valid explicit debug config emits nothing and does not terminate`() {
        val previous = System.getProperty("LOG_LEVEL")
        try {
            applyNormalizedLogLevel("debug", "core", { error("No diagnostic expected") }) {
                error("No termination expected")
            }
            assertEquals("DEBUG", System.getProperty("LOG_LEVEL"))
        } finally {
            if (previous == null) System.clearProperty("LOG_LEVEL") else System.setProperty("LOG_LEVEL", previous)
        }
    }
}
