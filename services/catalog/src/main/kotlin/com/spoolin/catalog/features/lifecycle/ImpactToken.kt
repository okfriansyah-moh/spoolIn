package com.spoolin.catalog.features.lifecycle

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Purpose-separated authentication of the entire current consequence set, with a five-minute lifetime. */
class ImpactToken(
    keyId: String,
    sourceKey: ByteArray,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val selector = keyId.also { require(it.matches(Regex("[A-Za-z0-9_-]{1,64}"))) }
    private val key = mac(sourceKey.also { require(it.size == KEY_BYTES) }, "spoolin-deletion-impact-v1".toByteArray())

    fun issue(snapshot: ImpactSnapshot): SignedImpact {
        val now = clock.instant()
        val expires = Instant.ofEpochSecond(now.epochSecond + LIFETIME_SECONDS)
        val payload =
            ByteArrayOutputStream()
                .also { buffer ->
                    DataOutputStream(buffer).use { output ->
                        output.writeByte(FORMAT_VERSION)
                        output.writeLong(snapshot.person.mostSignificantBits)
                        output.writeLong(snapshot.person.leastSignificantBits)
                        output.writeLong(snapshot.version)
                        output.writeLong(snapshot.impactVersion)
                        output.writeLong(snapshot.creditCount)
                        output.write(snapshot.digest())
                        output.writeLong(now.epochSecond)
                        output.writeLong(expires.epochSecond)
                    }
                }.toByteArray()
        val body = "$selector.${encode(payload)}"
        return SignedImpact("$body.${encode(mac(key, body.toByteArray(Charsets.US_ASCII)))}", expires)
    }

    fun matches(
        token: String?,
        snapshot: ImpactSnapshot,
    ): Boolean =
        runCatching {
            require(token != null && token.length <= MAX_TOKEN_LENGTH)
            val parts = token.split('.')
            require(parts.size == TOKEN_PARTS && parts[0] == selector)
            val body = "${parts[0]}.${parts[1]}".toByteArray(Charsets.US_ASCII)
            require(MessageDigest.isEqual(decode(parts[2]), mac(key, body)))
            val payload = decode(parts[1])
            require(payload.size == PAYLOAD_BYTES)
            DataInputStream(ByteArrayInputStream(payload)).use { input ->
                require(input.readUnsignedByte() == FORMAT_VERSION)
                require(UUID(input.readLong(), input.readLong()) == snapshot.person)
                require(input.readLong() == snapshot.version && input.readLong() == snapshot.impactVersion)
                require(input.readLong() == snapshot.creditCount)
                require(MessageDigest.isEqual(input.readNBytes(DIGEST_BYTES), snapshot.digest()))
                val issued = input.readLong()
                val expires = input.readLong()
                val now = clock.instant().epochSecond
                require(expires - issued == LIFETIME_SECONDS && issued <= now + CLOCK_SKEW_SECONDS && now < expires)
            }
            true
        }.getOrDefault(false)

    companion object {
        private const val KEY_BYTES = 32
        private const val DIGEST_BYTES = 32
        private const val LIFETIME_SECONDS = 300L
        private const val MAX_TOKEN_LENGTH = 512
        private const val TOKEN_PARTS = 3
        private const val PAYLOAD_BYTES = 89
        private const val FORMAT_VERSION = 1
        private const val CLOCK_SKEW_SECONDS = 2

        fun fromKeyFile(
            path: Path,
            keyId: String,
        ): ImpactToken {
            require(Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) && Files.size(path) == KEY_BYTES.toLong())
            require(
                Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS) ==
                    PosixFilePermissions.fromString("rw-------"),
            )
            val source = Files.readAllBytes(path)
            return try {
                ImpactToken(keyId, source)
            } finally {
                source.fill(0)
            }
        }

        private fun mac(
            key: ByteArray,
            value: ByteArray,
        ): ByteArray =
            Mac.getInstance("HmacSHA256").run {
                init(SecretKeySpec(key, "HmacSHA256"))
                doFinal(value)
            }

        private fun encode(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

        private fun decode(value: String): ByteArray {
            require(value.matches(Regex("[A-Za-z0-9_-]+")))
            return Base64.getUrlDecoder().decode(value).also { require(encode(it) == value) }
        }
    }
}

data class SignedImpact(
    val token: String,
    val expires: Instant,
) {
    override fun toString(): String = "SignedImpact(redacted)"
}
