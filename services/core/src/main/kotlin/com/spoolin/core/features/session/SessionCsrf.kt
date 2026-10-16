package com.spoolin.core.features.session

import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class SessionCsrf(
    key: ByteArray,
) {
    private val secret = SecretKeySpec(key.copyOf(), "HmacSHA256")

    init {
        require(key.size == SESSION_BYTES) { "Session CSRF key must contain 32 bytes" }
    }

    fun derive(credential: SessionCredential): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(secret)
        val input = credential.csrfInput()
        return try {
            Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(input))
        } finally {
            input.fill(0)
        }
    }

    fun digest(value: String): ByteArray {
        val encoded = value.toByteArray(Charsets.UTF_8)
        return MessageDigest.getInstance("SHA-256").digest(encoded)
    }

    fun matches(
        value: String,
        expected: ByteArray,
    ): Boolean = value.length == SESSION_ENCODED_LENGTH && MessageDigest.isEqual(digest(value), expected)

    override fun toString(): String = "SessionCsrf(content=redacted)"
}
