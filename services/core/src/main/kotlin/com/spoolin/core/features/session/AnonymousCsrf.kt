package com.spoolin.core.features.session

import java.security.MessageDigest
import java.time.Clock
import java.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private const val ANONYMOUS_SECONDS = 1800L
private const val SCOPE_PARTS = 4
private const val SIGNATURE_INDEX = 3
private const val MAX_SCOPE_LENGTH = 200

class AnonymousSubmission(
    val scope: String,
    val nonce: UUID,
    val csrfToken: String,
    private val cookieValue: String,
) {
    fun scopeCookieValue(): String = cookieValue

    fun cookies(): List<String> =
        listOf(
            "__Host-spoolin_registration=$cookieValue; Secure; HttpOnly; SameSite=Lax; Path=/",
            "__Host-spoolin_csrf=$csrfToken; Secure; SameSite=Lax; Path=/",
        )

    override fun toString(): String = "AnonymousSubmission(content=redacted)"
}

class AnonymousCsrf(
    key: ByteArray,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val secret = SecretKeySpec(key.copyOf(), "HmacSHA256")

    init {
        require(key.size == SESSION_BYTES) { "Anonymous CSRF key must contain 32 bytes" }
    }

    fun issue(): AnonymousSubmission {
        val scope = UUID.randomUUID()
        val nonce = UUID.randomUUID()
        val payload = "$scope.$nonce.${clock.instant().epochSecond + ANONYMOUS_SECONDS}"
        val cookie = "$payload.${sign("anonymous-scope-v1", payload)}"
        return AnonymousSubmission("anonymous:$scope", nonce, sign("anonymous-csrf-v1", cookie), cookie)
    }

    fun verify(
        cookie: String?,
        csrf: String?,
    ): AnonymousSubmission? =
        try {
            require(cookie != null && cookie.length <= MAX_SCOPE_LENGTH)
            require(csrf != null && csrf.length == SESSION_ENCODED_LENGTH)
            val parts = cookie.split('.')
            require(parts.size == SCOPE_PARTS)
            val scope = UUID.fromString(parts[0])
            val nonce = UUID.fromString(parts[1])
            val expires = parts[2].toLong()
            require(clock.instant().epochSecond < expires && expires <= clock.instant().epochSecond + ANONYMOUS_SECONDS)
            val payload = parts.take(SIGNATURE_INDEX).joinToString(".")
            require(equal(parts[SIGNATURE_INDEX], sign("anonymous-scope-v1", payload)))
            require(equal(csrf, sign("anonymous-csrf-v1", cookie)))
            AnonymousSubmission("anonymous:$scope", nonce, csrf, cookie)
        } catch (_: IllegalArgumentException) {
            null
        }

    private fun sign(
        purpose: String,
        value: String,
    ): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(secret)
        val signed = mac.doFinal("$purpose\u0000$value".toByteArray(Charsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signed)
    }

    private fun equal(
        first: String,
        second: String,
    ): Boolean = MessageDigest.isEqual(first.toByteArray(Charsets.UTF_8), second.toByteArray(Charsets.UTF_8))
}
