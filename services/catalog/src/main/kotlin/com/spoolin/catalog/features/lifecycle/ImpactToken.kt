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
class ImpactToken(keyId: String, sourceKey: ByteArray, private val clock: Clock = Clock.systemUTC()) {
    private val selector = keyId.also { require(it.matches(Regex("[A-Za-z0-9_-]{1,64}"))) }
    private val key = mac(sourceKey.also { require(it.size == 32) }, "spoolin-deletion-impact-v1".toByteArray())

    fun issue(snapshot: ImpactSnapshot): SignedImpact {
        val now = clock.instant()
        val expires = Instant.ofEpochSecond(now.epochSecond + 300)
        val payload = ByteArrayOutputStream().also { buffer -> DataOutputStream(buffer).use { output ->
            output.writeByte(1); output.writeLong(snapshot.person.mostSignificantBits)
            output.writeLong(snapshot.person.leastSignificantBits); output.writeLong(snapshot.version)
            output.writeLong(snapshot.impactVersion); output.writeLong(snapshot.creditCount)
            output.write(snapshot.digest()); output.writeLong(now.epochSecond); output.writeLong(expires.epochSecond)
        } }.toByteArray()
        val body = "$selector.${encode(payload)}"
        return SignedImpact("$body.${encode(mac(key, body.toByteArray(Charsets.US_ASCII)))}", expires)
    }

    fun matches(token: String?, snapshot: ImpactSnapshot): Boolean = runCatching {
        require(token != null && token.length <= 512)
        val parts = token.split('.')
        require(parts.size == 3 && parts[0] == selector)
        val body = "${parts[0]}.${parts[1]}".toByteArray(Charsets.US_ASCII)
        require(MessageDigest.isEqual(decode(parts[2]), mac(key, body)))
        val payload = decode(parts[1])
        require(payload.size == 89)
        DataInputStream(ByteArrayInputStream(payload)).use { input ->
            require(input.readUnsignedByte() == 1)
            require(UUID(input.readLong(), input.readLong()) == snapshot.person)
            require(input.readLong() == snapshot.version && input.readLong() == snapshot.impactVersion)
            require(input.readLong() == snapshot.creditCount)
            require(MessageDigest.isEqual(input.readNBytes(32), snapshot.digest()))
            val issued = input.readLong(); val expires = input.readLong(); val now = clock.instant().epochSecond
            require(expires - issued == 300L && issued <= now + 2 && now < expires)
        }
        true
    }.getOrDefault(false)

    companion object {
        fun fromKeyFile(path: Path, keyId: String): ImpactToken {
            require(Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) && Files.size(path) == 32L)
            require(Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS) == PosixFilePermissions.fromString("rw-------"))
            val source = Files.readAllBytes(path)
            return try { ImpactToken(keyId, source) } finally { source.fill(0) }
        }
        private fun mac(key: ByteArray, value: ByteArray): ByteArray = Mac.getInstance("HmacSHA256").run {
            init(SecretKeySpec(key, "HmacSHA256")); doFinal(value)
        }
        private fun encode(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        private fun decode(value: String): ByteArray {
            require(value.matches(Regex("[A-Za-z0-9_-]+")))
            return Base64.getUrlDecoder().decode(value).also { require(encode(it) == value) }
        }
    }
}

data class SignedImpact(val token: String, val expires: Instant) {
    override fun toString(): String = "SignedImpact(redacted)"
}
