package com.spoolin.core.features.session

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

internal const val SESSION_BYTES = 32
internal const val SESSION_ENCODED_LENGTH = 43
private const val COOKIE_ATTRIBUTES = "; Secure; HttpOnly; SameSite=Lax; Path=/"
private const val COOKIE_NAME = "__Host-spoolin_session"

class SessionCredential private constructor(
    private val bytes: ByteArray,
) {
    fun digest(): ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes)

    fun cookie(): String = "$COOKIE_NAME=${encode(bytes)}$COOKIE_ATTRIBUTES"

    internal fun csrfInput(): ByteArray = "session-csrf-v1".toByteArray(Charsets.UTF_8) + byteArrayOf(0) + bytes

    override fun toString(): String = "SessionCredential(content=redacted)"

    companion object {
        fun issue(): SessionCredential {
            val bytes = ByteArray(SESSION_BYTES)
            SecureRandom().nextBytes(bytes)
            return SessionCredential(bytes)
        }

        fun parse(value: String?): SessionCredential? =
            try {
                require(value != null && value.length == SESSION_ENCODED_LENGTH)
                val decoded = Base64.getUrlDecoder().decode(value)
                require(decoded.size == SESSION_BYTES && encode(decoded) == value)
                SessionCredential(decoded)
            } catch (_: IllegalArgumentException) {
                null
            }

        fun deletion(): String = "$COOKIE_NAME=$COOKIE_ATTRIBUTES; Max-Age=0"

        private fun encode(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
