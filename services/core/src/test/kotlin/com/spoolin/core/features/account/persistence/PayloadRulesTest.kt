package com.spoolin.core.features.account.persistence

import com.spoolin.core.features.crypto.ProtectedRead
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PayloadRulesTest {
    private val codec = AccountPayload()

    @Test
    fun versionedUtf8PayloadKeepsExactPermittedNameAndDisplayEmail() {
        val input = IdentityRules().identity("É O'Name", "A@Example.org")
        val encoded = codec.encode(input)
        val actual = (codec.decode(encoded) as ProtectedRead.Value).value
        assertEquals("É O'Name", actual.name)
        assertEquals("A@Example.org", actual.emailDisplay)
        assertEquals("a@example.org", actual.emailNormalized)
    }

    @Test
    fun unsupportedPayloadVersionsShapesAndInvalidUtf8HaveNoFallback() {
        val valid = """{"version":1,"name":"synthetic","emailDisplay":"A@example.org",
                "emailNormalized":"a@example.org"}"""
        listOf(
            valid.replace("\"version\":1", "\"version\":2"),
            valid.replace("\"version\":1", "\"version\":1.0"),
            valid.replace("\"name\":\"synthetic\"", "\"name\":null"),
            valid.replace("\"name\":\"synthetic\",", ""),
            valid.dropLast(1) + ",\"extra\":true}",
            "[]",
            "null",
            "invalid",
            "{}",
        ).forEach { assertEquals(ProtectedRead.Unavailable, codec.decode(it.toByteArray())) }
        assertEquals(ProtectedRead.Unavailable, codec.decode(byteArrayOf(0xc3.toByte(), 0x28)))
    }
}
