package com.spoolin.core.features.crypto

import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction.REPORT
import java.nio.charset.StandardCharsets.UTF_8

class NewPasswordPolicy(
    val minimum: Int = DEFAULT_PASSWORD_MINIMUM,
    val maximum: Int = 128,
) {
    init {
        require(minimum > 0 && maximum >= minimum && maximum <= DEFAULT_PASSWORD_MAXIMUM)
    }

    fun accepts(value: String): Boolean =
        try {
            val bytes = PasswordInput.exactBytes(value)
            try {
                value.codePointCount(0, value.length) in minimum..maximum && bytes.size <= MAX_NEW_PASSWORD_BYTES
            } finally {
                bytes.fill(0)
            }
        } catch (_: IllegalArgumentException) {
            false
        }
}

object PasswordInput {
    fun exactBytes(value: String): ByteArray =
        try {
            val buffer =
                UTF_8
                    .newEncoder()
                    .onMalformedInput(REPORT)
                    .onUnmappableCharacter(REPORT)
                    .encode(CharBuffer.wrap(value))
            ByteArray(buffer.remaining()).also { buffer.get(it) }
        } catch (_: java.nio.charset.CharacterCodingException) {
            throw IllegalArgumentException("Password contains unsupported Unicode")
        }

    fun verificationEligible(value: String): Boolean =
        try {
            exactBytes(value).fill(0)
            true
        } catch (_: IllegalArgumentException) {
            false
        }
}
