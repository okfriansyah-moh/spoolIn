package com.spoolin.core.boundary.grpc

import com.spoolin.core.features.session.SessionPrincipal
import com.spoolin.core.identity.AccountAuthority
import com.spoolin.core.identity.AccountRole
import com.spoolin.support.identity.ActorSigner
import com.spoolin.support.identity.ActorVerifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.security.KeyPairGenerator
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Base64
import java.util.UUID

class ActorIssuerRulesTest {
    private val now = Instant.ofEpochSecond(1800000000)
    private val keys = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
    private val issuer = ActorIssuer(ActorSigner("key_A", keys.private), Clock.fixed(now, ZoneOffset.UTC))
    private val account = AccountAuthority(UUID.randomUUID(), AccountRole.VIEWER, 5)
    private val principal = SessionPrincipal(UUID.randomUUID(), account, now.plusSeconds(600), "private-csrf")
    private val target =
        ActorTarget("catalog", "/spoolin.v1.CatalogService/GetOperation", UUID.randomUUID(), UUID.randomUUID())

    @Test
    fun `issuer binds authoritative identity and fresh sixteen byte nonces with thirty second ceiling`() {
        val first =
            ActorVerifier(mapOf("key_A" to keys.public)).verifyTarget(
                issuer.issue(principal, target),
                target.audience,
                target.method,
                now,
            )
        val second =
            ActorVerifier(mapOf("key_A" to keys.public)).verifyTarget(
                issuer.issue(principal, target),
                target.audience,
                target.method,
                now,
            )
        assertEquals(account.id, first.accountId)
        assertEquals(principal.id, first.credentialId)
        assertEquals("VIEWER", first.role)
        assertEquals(5, first.authEpoch)
        assertEquals(target.operation, first.operationId)
        assertEquals(target.correlation, first.correlationId)
        assertEquals(30, first.expiresAt - first.issuedAt)
        assertEquals(16, Base64.getUrlDecoder().decode(first.nonce).size)
        assertNotEquals(first.nonce, second.nonce)
    }

    @Test
    fun `session expiry bounds issued assertions and expired or wrong audience target cannot issue`() {
        val short = SessionPrincipal(principal.id, account, now.plusSeconds(10), "private-csrf")
        val claims =
            ActorVerifier(mapOf("key_A" to keys.public)).verifyTarget(
                issuer.issue(short, target),
                target.audience,
                target.method,
                now,
            )
        assertEquals(now.epochSecond + 10, claims.expiresAt)
        assertThrows(IllegalArgumentException::class.java) {
            issuer.issue(SessionPrincipal(principal.id, account, now, "private-csrf"), target)
        }
        assertThrows(IllegalArgumentException::class.java) { issuer.issue(principal, target.copy(audience = "mcp")) }
    }
}
