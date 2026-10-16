package com.spoolin.support.identity

import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.interfaces.EdECKey
import java.time.Instant

private const val MAXIMUM_VERIFIERS = 4
private const val ASCII_BOUND = 128
private const val TOKEN_PARTS = 3
private const val MAXIMUM_METADATA = 4096
private const val SIGNATURE_BYTES = 64
private const val NONCE_BYTES = 16
private const val MAXIMUM_LIFETIME = 30L
private const val CLOCK_SKEW = 2L

class ActorSigner(
    private val id: String,
    private val key: PrivateKey,
) {
    init {
        require(id.matches(Regex("[A-Za-z0-9_-]{1,64}")))
        require(key is EdECKey && key.params.name == "Ed25519")
    }

    fun sign(claims: ActorClaims): String =
        checkedActor {
            val input = "$id.${ActorEncoding.payload(claims)}"
            val signature = Signature.getInstance("Ed25519")
            signature.initSign(key)
            signature.update(input.toByteArray(Charsets.US_ASCII))
            "$input.${ActorEncoding.encode(signature.sign())}".also { require(it.length <= MAXIMUM_METADATA) }
        }
}

class ActorVerifier(
    keys: Map<String, PublicKey>,
) {
    private val keys = keys.toMap()

    init {
        require(keys.size in 1..MAXIMUM_VERIFIERS)
        require(
            keys.all { (id, key) ->
                id.matches(Regex("[A-Za-z0-9_-]{1,64}")) && key is EdECKey && key.params.name == "Ed25519"
            },
        )
    }

    fun verify(
        token: String,
        binding: ActorBinding,
        now: Instant,
    ): ActorClaims {
        val claims = verifyTarget(token, binding.audience, binding.method, now)
        return checkedActor {
            require(binding.accountId == null || claims.accountId == binding.accountId)
            require(claims.operationId == binding.operationId)
            claims
        }
    }

    fun verifyTarget(
        token: String,
        audience: String,
        method: String,
        now: Instant,
    ): ActorClaims =
        checkedActor {
            require(token.length <= MAXIMUM_METADATA && token.all { it.code < ASCII_BOUND })
            val parts = token.split('.')
            require(parts.size == TOKEN_PARTS && parts[0].matches(Regex("[A-Za-z0-9_-]{1,64}")))
            val key = requireNotNull(keys[parts[0]])
            val bytes = ActorEncoding.decode(parts[2])
            require(bytes.size == SIGNATURE_BYTES)
            val signature = Signature.getInstance("Ed25519")
            signature.initVerify(key)
            signature.update("${parts[0]}.${parts[1]}".toByteArray(Charsets.US_ASCII))
            require(signature.verify(bytes))
            ActorEncoding.claims(parts[1]).also { validate(it, audience, method, now.epochSecond) }
        }

    private fun validate(
        claims: ActorClaims,
        audience: String,
        method: String,
        now: Long,
    ) {
        require(claims.audience == audience && claims.method == method)
        require(claims.authEpoch >= 0 && claims.role in setOf("VIEWER", "MAINTAINER"))
        require(claims.clientKind == null || claims.clientKind in setOf("CHATGPT", "CLAUDE"))
        require(claims.expiresAt > claims.issuedAt && claims.expiresAt - claims.issuedAt <= MAXIMUM_LIFETIME)
        require(claims.issuedAt <= now + CLOCK_SKEW && claims.expiresAt >= now - CLOCK_SKEW)
        require(ActorEncoding.decode(claims.nonce).size == NONCE_BYTES)
    }
}

private inline fun <T> checkedActor(operation: () -> T): T =
    try {
        operation()
    } catch (_: Exception) {
        throw ActorRejected()
    }
