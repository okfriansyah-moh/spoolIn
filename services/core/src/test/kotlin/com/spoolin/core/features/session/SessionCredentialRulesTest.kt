package com.spoolin.core.features.session

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class SessionCredentialRulesTest {
    private val key = ByteArray(32) { (it + 1).toByte() }

    @Test
    fun `opaque credentials store only digests and deliver scoped session cookies`() {
        val credential = SessionCredential.issue()
        val header = credential.cookie()
        val value = header.substringAfter('=').substringBefore(';')
        val raw = Base64.getUrlDecoder().decode(value)
        assertEquals(32, raw.size)
        assertArrayEquals(MessageDigest.getInstance("SHA-256").digest(raw), credential.digest())
        assertEquals("__Host-spoolin_session=$value; Secure; HttpOnly; SameSite=Lax; Path=/", header)
        assertFalse(credential.toString().contains(value))
        assertArrayEquals(credential.digest(), requireNotNull(SessionCredential.parse(value)).digest())
        assertNotEquals(header, SessionCredential.issue().cookie())
        assertEquals(
            "__Host-spoolin_session=; Secure; HttpOnly; SameSite=Lax; Path=/; Max-Age=0",
            SessionCredential.deletion(),
        )
    }

    @Test
    fun `credential parser rejects padding wrong sizes and alternate encodings`() {
        listOf("", "a", "a".repeat(42), "a".repeat(44), "a".repeat(43) + "=", "+".repeat(43), "/".repeat(43))
            .forEach { assertNull(SessionCredential.parse(it)) }
    }

    @Test
    fun `csrf derives from exact decoded session bytes and changes with key or credential`() {
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32))
        val credential = requireNotNull(SessionCredential.parse(encoded))
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        val expected =
            Base64.getUrlEncoder().withoutPadding().encodeToString(
                mac.doFinal("session-csrf-v1".toByteArray() + byteArrayOf(0) + ByteArray(32)),
            )
        val csrf = SessionCsrf(key)
        assertEquals(expected, csrf.derive(credential))
        assertArrayEquals(MessageDigest.getInstance("SHA-256").digest(expected.toByteArray()), csrf.digest(expected))
        assertFalse(csrf.matches(expected + "x", csrf.digest(expected)))
        assertNotEquals(expected, SessionCsrf(ByteArray(32)).derive(credential))
        assertNotEquals(expected, csrf.derive(SessionCredential.issue()))
    }
}
