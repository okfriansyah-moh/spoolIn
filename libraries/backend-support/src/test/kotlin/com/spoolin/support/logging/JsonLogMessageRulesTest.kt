package com.spoolin.support.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class JsonLogMessageRulesTest {
    @Test
    fun jsonString_quotes_and_escapes_message() {
        assertEquals("\"hello\"", jsonString("hello"))
        assertEquals("\"line\\nbreak\"", jsonString("line\nbreak"))
        assertEquals("\"say \\\"hi\\\"\"", jsonString("say \"hi\""))
    }

    @Test
    fun normalize_log_level_to_info_or_debug_only() {
        assertEquals("DEBUG", normalizeConfiguredLogLevel("debug"))
        assertEquals("INFO", normalizeConfiguredLogLevel("info"))
        assertEquals("INFO", normalizeConfiguredLogLevel("trace"))
        assertEquals("INFO", normalizeConfiguredLogLevel(null))
    }
}
