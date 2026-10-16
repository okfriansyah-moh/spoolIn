package com.spoolin.core.features.crypto

import java.util.Base64

class StoredPasswordHash(
    val encoded: String,
) {
    init {
        require(encoded.length <= MAX_PHC_CHARACTERS)
    }
}

class PasswordVerification(
    val matched: Boolean,
    val replacement: StoredPasswordHash? = null,
)

interface PasswordEngine {
    fun hash(bytes: ByteArray): StoredPasswordHash

    fun verify(
        bytes: ByteArray,
        stored: StoredPasswordHash,
    ): PasswordVerification
}

class PhcParameters private constructor(
    val memory: Int,
    val iterations: Int,
    val parallelism: Int,
    val salt: ByteArray,
    val output: ByteArray,
) {
    fun needsRehash(): Boolean =
        memory != ARGON_MEMORY_KIB || iterations != ARGON_ITERATIONS || parallelism != ARGON_PARALLELISM ||
            salt.size != ARGON_SALT_BYTES ||
            output.size != ARGON_OUTPUT_BYTES

    companion object {
        private val syntax =
            Regex(
                "^\\\$argon2id\\\$v=19\\\$m=(?<memory>[0-9]{1,6}),t=(?<iterations>[0-9]{1,2})," +
                    "p=(?<parallelism>[0-9])\\\$(?<salt>[A-Za-z0-9+/]+)\\\$(?<output>[A-Za-z0-9+/]+)\$",
            )

        fun parse(encoded: String): PhcParameters? =
            try {
                require(encoded.length <= MAX_PHC_CHARACTERS)
                val match = requireNotNull(syntax.matchEntire(encoded))
                val memory = requireNotNull(match.groups["memory"]).value.toInt()
                val iterations = requireNotNull(match.groups["iterations"]).value.toInt()
                val parallelism = requireNotNull(match.groups["parallelism"]).value.toInt()
                require(parallelism in 1..ARGON_MAX_PARALLELISM)
                require(memory in (ARGON_MIN_LANE_MEMORY * parallelism)..ARGON_MAX_MEMORY_KIB)
                require(iterations in 1..ARGON_MAX_ITERATIONS)
                val salt = decode(requireNotNull(match.groups["salt"]).value)
                val output = decode(requireNotNull(match.groups["output"]).value)
                require(salt.size in ARGON_SALT_BYTES..ARGON_MAX_SALT_BYTES)
                require(output.size in ARGON_MIN_OUTPUT_BYTES..ARGON_MAX_OUTPUT_BYTES)
                PhcParameters(memory, iterations, parallelism, salt, output)
            } catch (_: IllegalArgumentException) {
                null
            }

        private fun decode(text: String): ByteArray {
            val bytes = Base64.getDecoder().decode(text)
            require(Base64.getEncoder().withoutPadding().encodeToString(bytes) == text)
            return bytes
        }
    }
}

internal const val MAX_PHC_CHARACTERS = 512
internal const val DEFAULT_PASSWORD_MINIMUM = 15
internal const val DEFAULT_PASSWORD_MAXIMUM = 128
internal const val MAX_NEW_PASSWORD_BYTES = 512
internal const val ARGON_MEMORY_KIB = 65536
internal const val ARGON_ITERATIONS = 3
internal const val ARGON_PARALLELISM = 1
internal const val ARGON_SALT_BYTES = 16
internal const val ARGON_OUTPUT_BYTES = 32
internal const val ARGON_MAX_MEMORY_KIB = 131072
internal const val ARGON_MAX_ITERATIONS = 6
internal const val ARGON_MAX_PARALLELISM = 2
internal const val ARGON_MIN_LANE_MEMORY = 8
internal const val ARGON_MAX_SALT_BYTES = 64
internal const val ARGON_MIN_OUTPUT_BYTES = 16
internal const val ARGON_MAX_OUTPUT_BYTES = 64
