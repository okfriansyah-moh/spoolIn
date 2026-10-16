package com.spoolin.core.features.session

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class AnonymousCsrfRulesTest {
    @Test
    fun `anonymous scope and csrf are signed bound and expire before registration`() {
        val now = Instant.parse("2026-10-20T00:00:00Z")
        val key = ByteArray(32) { 10 }
        val signer = AnonymousCsrf(key, Clock.fixed(now, ZoneOffset.UTC))
        val issued = signer.issue()
        assertEquals(issued.scope, signer.verify(issued.scopeCookieValue(), issued.csrfToken)?.scope)
        assertEquals(issued.nonce, signer.verify(issued.scopeCookieValue(), issued.csrfToken)?.nonce)
        assertNull(signer.verify(issued.scopeCookieValue() + "x", issued.csrfToken))
        assertNull(signer.verify(issued.scopeCookieValue(), signer.issue().csrfToken))
        val rotated = AnonymousCsrf(ByteArray(32), Clock.fixed(now, ZoneOffset.UTC))
        assertNull(rotated.verify(issued.scopeCookieValue(), issued.csrfToken))
        val expired = AnonymousCsrf(key, Clock.fixed(now.plusSeconds(1800), ZoneOffset.UTC))
        assertNull(expired.verify(issued.scopeCookieValue(), issued.csrfToken))
        assertNotEquals(issued.scope, signer.issue().scope)
    }
}
