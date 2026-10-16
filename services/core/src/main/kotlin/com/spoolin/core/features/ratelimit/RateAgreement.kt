package com.spoolin.core.features.ratelimit

import tools.jackson.core.StreamReadFeature
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private const val HEX_RADIX = 16
private const val MAXIMUM_AGREEMENT_BYTES = 4096L
private const val AGREEMENT_KEY_BYTES = 32

class RateAgreement(
    private val path: Path,
    private val revision: String,
    secret: ByteArray,
) {
    private val canary =
        Mac.getInstance("HmacSHA256").run {
            require(secret.size == AGREEMENT_KEY_BYTES)
            init(SecretKeySpec(secret, "HmacSHA256"))
            doFinal("spoolin-rate-agreement-v1".toByteArray(Charsets.US_ASCII))
        }
    private val mapper =
        JsonMapper
            .builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build()

    init {
        check()
    }

    fun check() {
        try {
            require(Files.isRegularFile(path, NOFOLLOW_LINKS))
            require(Files.getPosixFilePermissions(path, NOFOLLOW_LINKS) == PosixFilePermissions.fromString("rw-------"))
            require(Files.size(path) in 1..MAXIMUM_AGREEMENT_BYTES)
            val values = mapper.readValue(Files.readAllBytes(path), object : TypeReference<Map<String, Any?>>() {})
            require(values.keys == setOf("profileRevision", "keyCanary"))
            require(values["profileRevision"] == revision)
            val expected = requireNotNull(values["keyCanary"] as? String)
            require(expected.matches(Regex("[0-9a-f]{64}")))
            val decoded = expected.chunked(2).map { it.toInt(HEX_RADIX).toByte() }.toByteArray()
            require(MessageDigest.isEqual(canary, decoded))
        } catch (_: Exception) {
            throw RateUnavailable()
        }
    }

    override fun toString(): String = "RateAgreement(redacted)"
}
