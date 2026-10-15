package com.spoolin.core.features.crypto

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

class Argon2Engine(
    private val random: SecureRandom = SecureRandom(),
) : PasswordEngine {
    override fun hash(bytes: ByteArray): StoredPasswordHash {
        val salt = ByteArray(ARGON_SALT_BYTES).also { random.nextBytes(it) }
        val output = derive(bytes, salt, ArgonWorkSpec())
        return try {
            StoredPasswordHash("\$argon2id\$v=19\$m=65536,t=3,p=1\$${encode(salt)}\$${encode(output)}")
        } finally {
            output.fill(0)
        }
    }

    override fun verify(
        bytes: ByteArray,
        stored: StoredPasswordHash,
    ): PasswordVerification {
        val parameters = PhcParameters.parse(stored.encoded) ?: return PasswordVerification(false)
        val actual =
            derive(
                bytes,
                parameters.salt,
                ArgonWorkSpec(parameters.memory, parameters.iterations, parameters.parallelism, parameters.output.size),
            )
        return try {
            val matched = MessageDigest.isEqual(parameters.output, actual)
            PasswordVerification(matched, if (matched && parameters.needsRehash()) hash(bytes) else null)
        } finally {
            actual.fill(0)
            parameters.output.fill(0)
        }
    }

    private fun derive(
        bytes: ByteArray,
        salt: ByteArray,
        work: ArgonWorkSpec,
    ): ByteArray {
        val parameters =
            Argon2Parameters
                .Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withMemoryAsKB(work.memory)
                .withIterations(work.iterations)
                .withParallelism(work.parallelism)
                .withSalt(salt)
                .build()
        return try {
            ByteArray(work.size).also { output ->
                Argon2BytesGenerator().also { it.init(parameters) }.generateBytes(bytes, output)
            }
        } finally {
            parameters.clear()
        }
    }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().withoutPadding().encodeToString(bytes)
}

private class ArgonWorkSpec(
    val memory: Int = ARGON_MEMORY_KIB,
    val iterations: Int = ARGON_ITERATIONS,
    val parallelism: Int = ARGON_PARALLELISM,
    val size: Int = ARGON_OUTPUT_BYTES,
)
