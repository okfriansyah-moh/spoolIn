package com.spoolin.support.identity

import tools.jackson.core.StreamReadFeature
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.DeserializationFeature
import java.util.Base64
import java.util.UUID

internal object ActorEncoding {
    private val mapper = JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build()
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()
    private val fields =
        setOf(
            "issuer",
            "audience",
            "method",
            "accountId",
            "credentialId",
            "clientKind",
            "role",
            "authEpoch",
            "issuedAt",
            "expiresAt",
            "nonce",
            "correlationId",
            "operationId",
        )

    fun encode(bytes: ByteArray): String = encoder.encodeToString(bytes)

    fun decode(text: String): ByteArray {
        require(text.matches(Regex("[A-Za-z0-9_-]+")))
        return decoder.decode(text).also { require(encode(it) == text) }
    }

    fun payload(claims: ActorClaims): String = encode(mapper.writeValueAsBytes(values(claims)))

    fun claims(text: String): ActorClaims {
        val value = mapper.readValue(decode(text), object : TypeReference<Map<String, Any?>>() {})
        require(value.keys == fields && value["issuer"] == "core")
        return ActorClaims(
            string(value, "audience"),
            string(value, "method"),
            uuid(value, "accountId"),
            uuid(value, "credentialId"),
            nullableString(value, "clientKind"),
            string(value, "role"),
            decimal(value, "authEpoch"),
            seconds(value, "issuedAt"),
            seconds(value, "expiresAt"),
            string(value, "nonce"),
            uuid(value, "correlationId"),
            nullableString(value, "operationId")?.let { uuid(value, "operationId") },
        )
    }

    private fun values(claims: ActorClaims): Map<String, Any?> =
        linkedMapOf(
            "issuer" to "core",
            "audience" to claims.audience,
            "method" to claims.method,
            "accountId" to claims.accountId.toString(),
            "credentialId" to claims.credentialId.toString(),
            "clientKind" to claims.clientKind,
            "role" to claims.role,
            "authEpoch" to claims.authEpoch.toString(),
            "issuedAt" to claims.issuedAt,
            "expiresAt" to claims.expiresAt,
            "nonce" to claims.nonce,
            "correlationId" to claims.correlationId.toString(),
            "operationId" to claims.operationId?.toString(),
        )

    private fun string(
        value: Map<String, Any?>,
        field: String,
    ): String = requireNotNull(value[field] as? String)

    private fun nullableString(
        value: Map<String, Any?>,
        field: String,
    ): String? {
        require(value[field] == null || value[field] is String)
        return value[field] as? String
    }

    private fun decimal(
        value: Map<String, Any?>,
        field: String,
    ): Long {
        val text = string(value, field)
        require(text.matches(Regex("0|[1-9][0-9]*")))
        return text.toLong()
    }

    private fun seconds(
        value: Map<String, Any?>,
        field: String,
    ): Long {
        val number = value[field]
        require(number is Int || number is Long)
        return (number as Number).toLong().also { require(it >= 0) }
    }

    private fun uuid(
        value: Map<String, Any?>,
        field: String,
    ): UUID = UUID.fromString(string(value, field)).also { require(it.toString() == value[field]) }
}
