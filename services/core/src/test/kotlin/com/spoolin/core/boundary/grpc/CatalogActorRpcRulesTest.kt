package com.spoolin.core.boundary.grpc

import com.spoolin.core.coveragehttp.HttpRpcReplies
import com.spoolin.core.features.ratelimit.RateUnavailable
import com.spoolin.core.features.session.SessionPrincipal
import com.spoolin.core.identity.AccountAuthority
import com.spoolin.core.identity.AccountRole
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.identity.ActorSigner
import com.spoolin.support.identity.ActorVerifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.Spoolin
import java.security.KeyPairGenerator
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.TimeUnit

class CatalogActorRpcRulesTest {
    private val now = Instant.ofEpochSecond(1800000000)
    private val keys = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
    private val issuer = ActorIssuer(ActorSigner("test", keys.private), Clock.fixed(now, ZoneOffset.UTC))
    private val principal = SessionPrincipal(
        UUID.randomUUID(), AccountAuthority(UUID.randomUUID(), AccountRole.MAINTAINER, 5), now.plusSeconds(600), "csrf",
    )

    @Test fun `catalog transport attaches the scoped signed actor and preserves default and artwork deadlines`() {
        val replies = HttpRpcReplies(Spoolin.Title.getDefaultInstance(), Spoolin.Title.getDefaultInstance())
        val rpc = CatalogActorRpc(CatalogServiceGrpc.newBlockingStub(replies), issuer)
        val operation = UUID.randomUUID()
        val correlation = UUID.randomUUID()
        val method = "/spoolin.v1.CatalogService/ManagedTitle"
        val token = rpc.issue(principal, method, operation, correlation)
        val claims = ActorVerifier(mapOf("test" to keys.public)).verifyTarget(token, "catalog", method, now)
        assertEquals(principal.accountId, claims.accountId)
        assertEquals(principal.id, claims.credentialId)
        assertEquals("MAINTAINER", claims.role)
        assertEquals(operation, claims.operationId)
        assertEquals(correlation, claims.correlationId)
        rpc.stub(token).managedTitle(Spoolin.Id.newBuilder().setId("title").build())
        rpc.stub(token, 30L).managedTitle(Spoolin.Id.newBuilder().setId("title").build())
        assertEquals(listOf(token, token), replies.requestHeaders.map { it.get(ActorRpc.metadata) })
        for ((options, ceiling) in replies.callOptions.zip(listOf(5L, 30L))) {
            val remaining = requireNotNull(options.deadline).timeRemaining(TimeUnit.MILLISECONDS)
            assertTrue(remaining in 1..TimeUnit.SECONDS.toMillis(ceiling))
        }
    }

    @Test fun `missing signer fails before any external catalog request`() {
        val replies = HttpRpcReplies()
        val rpc = CatalogActorRpc(CatalogServiceGrpc.newBlockingStub(replies), null)
        assertThrows(RateUnavailable::class.java) {
            rpc.issue(principal, "/spoolin.v1.CatalogService/WriteMovie", UUID.randomUUID(), UUID.randomUUID())
        }
        assertTrue(replies.requests.isEmpty())
    }
}
