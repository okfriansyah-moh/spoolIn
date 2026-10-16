package com.spoolin.support.identity

import com.spoolin.support.policy.ActorKeyRules
import tools.jackson.core.StreamReadFeature
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions
import java.security.PrivateKey
import java.security.PublicKey
import java.util.Base64

private const val TRUST_FILE_BYTES = 4096L

object ActorTrust {
    fun signer(path: Path): PrivateKey =
        trust {
            require(Files.getPosixFilePermissions(path, NOFOLLOW_LINKS) == PosixFilePermissions.fromString("rw-------"))
            val bytes = read(path)
            try {
                ActorKeyRules.signing(bytes)
            } finally {
                bytes.fill(0)
            }
        }

    fun verifiers(path: Path): Map<String, PublicKey> =
        trust {
            val permissions = Files.getPosixFilePermissions(path, NOFOLLOW_LINKS)
            require(PosixFilePermission.GROUP_WRITE !in permissions && PosixFilePermission.OTHERS_WRITE !in permissions)
            val mapper =
                JsonMapper
                    .builder()
                    .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                    .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .build()
            val values = mapper.readValue(read(path), object : TypeReference<Map<String, String>>() {})
            ActorKeyRules.verifiers(
                values.mapValues { (_, encoded) ->
                    Base64
                        .getDecoder()
                        .decode(
                            encoded,
                        ).also { require(Base64.getEncoder().encodeToString(it) == encoded) }
                },
            )
        }

    private fun read(path: Path): ByteArray {
        require(Files.isRegularFile(path, NOFOLLOW_LINKS) && Files.size(path) in 1..TRUST_FILE_BYTES)
        return Files.readAllBytes(path)
    }

    private inline fun <T> trust(read: () -> T): T =
        try {
            read()
        } catch (_: Exception) {
            throw ActorRejected()
        }
}
