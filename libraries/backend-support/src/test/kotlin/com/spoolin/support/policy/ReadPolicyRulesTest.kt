package com.spoolin.support.policy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ReadPolicyRulesTest {
    private val policy = ReadPolicy(PolicySettings.resolve(ConfigurationSources(emptyMap(), mapOf(
        "REQUEST_DEFAULT_PAGE" to "2", "REQUEST_MAX_PAGE" to "3",
        "SEARCH_MIN_CODE_POINTS" to "2", "SEARCH_MAX_CODE_POINTS" to "4",
    ))))

    @Test
    fun `effective page defaults and inclusive bounds reject rather than clamp invalid requests`() {
        assertEquals(2, policy.pageSize(null))
        assertEquals(2, policy.rpcPageSize(0))
        assertEquals(1, policy.pageSize(1))
        assertEquals(3, policy.pageSize(3))
        listOf(-1, 0, 4).forEach { first ->
            assertThrows(InputViolation::class.java) { policy.pageSize(first) }
        }
    }

    @Test
    fun `search uses normalized scalar counts and nonempty minimum while blank clears`() {
        assertNull(policy.search(null))
        assertNull(policy.search(" \u00a0\t "))
        assertEquals("Zo", policy.search(" Zo "))
        assertEquals("A\uD83D\uDE00BC", policy.search("A\uD83D\uDE00BC"))
        listOf("a", "abcde", "\uD800", "a\u0000b").forEach { text ->
            assertThrows(InputViolation::class.java) { policy.search(text) }
        }
    }

    @Test
    fun `scope binds typed criteria page and policy without delimiter collisions`() {
        val first = policy.cursorScope("people", 2, mapOf("text" to "a;k=;g=;y=;m="))
        assertNotEquals(first, policy.cursorScope("people", 2, mapOf("text" to "a", "k" to "")))
        assertNotEquals(first, policy.cursorScope("people", 3, mapOf("text" to "a;k=;g=;y=;m=")))
        val changed = ReadPolicy(PolicySettings.resolve(ConfigurationSources()))
        assertNotEquals(first, changed.cursorScope("people", 2, mapOf("text" to "a;k=;g=;y=;m=")))
    }
}
