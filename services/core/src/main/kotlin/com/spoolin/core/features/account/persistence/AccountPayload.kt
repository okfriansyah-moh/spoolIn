package com.spoolin.core.features.account.persistence

import com.spoolin.core.features.crypto.ProtectedRead
import tools.jackson.databind.ObjectMapper

class AccountPayload(
    private val mapper: ObjectMapper = ObjectMapper(),
) {
    fun encode(identity: AccountIdentity): ByteArray =
        mapper.writeValueAsBytes(
            mapper
                .createObjectNode()
                .put("version", 1)
                .put("name", identity.name)
                .put("emailDisplay", identity.emailDisplay)
                .put("emailNormalized", identity.emailNormalized),
        )

    fun decode(bytes: ByteArray): ProtectedRead<AccountIdentity> =
        try {
            val node = mapper.readTree(bytes)
            val fields = node.properties().map { it.key }.toSet()
            require(node.isObject && fields == setOf("version", "name", "emailDisplay", "emailNormalized"))
            require(node.get("version").isIntegralNumber && node.get("version").asInt() == 1)
            require(listOf("name", "emailDisplay", "emailNormalized").all { node.get(it).isString })
            ProtectedRead.Value(
                AccountIdentity(
                    node.get("name").asString(),
                    node.get("emailDisplay").asString(),
                    node.get("emailNormalized").asString(),
                ),
            )
        } catch (_: RuntimeException) {
            ProtectedRead.Unavailable
        }
}
