package com.spoolin.core.boundary.admission

import com.spoolin.core.features.ratelimit.RateAgreement
import com.spoolin.core.features.ratelimit.RateUnavailable
import com.spoolin.support.identity.RateProfiles
import com.spoolin.support.policy.ConfigurationSources
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class RateAgreementTest {
    @TempDir lateinit var directory: Path

    @Test
    fun `protected persisted agreement binds actual resolved profiles and key across restart`() {
        val profiles = RateProfiles(ConfigurationSources())
        val secret = ByteArray(32) { 21 }
        val path = write(profiles.revision, secret)
        RateAgreement(path, profiles.revision, secret).check()
        RateAgreement(path, profiles.revision, secret).check()
        val changed = RateProfiles(ConfigurationSources(mapOf("RATE_LIMIT_READ_MAX_REQUESTS" to "121")))
        assertThrows(RateUnavailable::class.java) { RateAgreement(path, changed.revision, secret) }
        assertThrows(RateUnavailable::class.java) { RateAgreement(path, profiles.revision, ByteArray(32) { 22 }) }
        val current = RateAgreement(path, profiles.revision, secret)
        Files.delete(path)
        val failure = assertThrows(RateUnavailable::class.java) { current.check() }
        assertFalse(failure.toString().contains(directory.toString()))
    }

    @Test
    fun `links public permissions duplicates trailing bytes and malformed maps fail safely`() {
        val profiles = RateProfiles(ConfigurationSources())
        val secret = ByteArray(32) { 23 }
        val path = write(profiles.revision, secret)
        val json = Files.readString(path)
        listOf("{}", json + "{}", json.replace("{", "{\"profileRevision\":\"duplicate\","), "x".repeat(4097))
            .forEach { invalid ->
                Files.writeString(path, invalid)
                assertThrows(RateUnavailable::class.java) { RateAgreement(path, profiles.revision, secret) }
            }
        Files.writeString(path, json)
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-r--r--"))
        assertThrows(RateUnavailable::class.java) { RateAgreement(path, profiles.revision, secret) }
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"))
        val link = directory.resolve("link")
        Files.createSymbolicLink(link, path)
        assertThrows(RateUnavailable::class.java) { RateAgreement(link, profiles.revision, secret) }
    }

    private fun write(
        revision: String,
        secret: ByteArray,
    ): Path {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret, "HmacSHA256"))
        val canary = mac.doFinal("spoolin-rate-agreement-v1".toByteArray()).joinToString("") { "%02x".format(it) }
        val path = directory.resolve("agreement.json")
        Files.writeString(path, """{"profileRevision":"$revision","keyCanary":"$canary"}""")
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"))
        return path
    }
}
