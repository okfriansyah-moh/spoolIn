package com.spoolin.support.identity

import java.util.UUID

class ActorRejected : IllegalArgumentException("Actor assertion is not permitted")

data class ActorBinding(
    val audience: String,
    val method: String,
    val accountId: UUID?,
    val operationId: UUID?,
)

data class ActorClaims(
    val audience: String,
    val method: String,
    val accountId: UUID,
    val credentialId: UUID,
    val clientKind: String?,
    val role: String,
    val authEpoch: Long,
    val issuedAt: Long,
    val expiresAt: Long,
    val nonce: String,
    val correlationId: UUID,
    val operationId: UUID?,
) {
    override fun toString(): String = "ActorClaims(redacted)"
}
