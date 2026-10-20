package com.spoolin.core.features.mcpgateway

import com.spoolin.core.features.connector.ConnectorRecord
import com.spoolin.support.identity.ActorClaims
import com.spoolin.support.identity.ActorSigner
import java.security.SecureRandom
import java.time.Clock
import java.util.Base64
import java.util.UUID

private const val ACTOR_LIFETIME = 30L
private const val ACTOR_NONCE_BYTES = 16

class McpActorIssuer(
    private val signer: ActorSigner,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val random = SecureRandom()

    fun issue(
        connector: ConnectorRecord,
        role: String,
        method: String,
        operationId: UUID?,
        correlationId: UUID,
    ): String {
        val now = clock.instant().epochSecond
        val expires = minOf(now + ACTOR_LIFETIME, connector.expiresAt.epochSecond)
        require(expires > now)
        val nonce = ByteArray(ACTOR_NONCE_BYTES).also(random::nextBytes)
        return signer.sign(
            ActorClaims(
                audience = if (method.contains("MediaService")) "media" else "catalog",
                method = method,
                accountId = connector.accountId,
                credentialId = connector.id,
                clientKind = connector.clientKind,
                role = role,
                authEpoch = connector.authEpoch,
                issuedAt = now,
                expiresAt = expires,
                nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(nonce),
                correlationId = correlationId,
                operationId = operationId,
            ),
        )
    }
}
