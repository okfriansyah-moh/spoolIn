package com.spoolin.support.identity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.security.KeyPairGenerator
import java.util.Base64

class ActorTrustTest {
    @TempDir
    lateinit var root: Path

    private val keys = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()

    @Test
    fun `private signer accepts only protected regular Ed25519 DER`() {
        val path = file("private", keys.private.encoded)
        assertEquals(keys.private, ActorTrust.signer(path))
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-r--r--"))
        assertThrows(ActorRejected::class.java) { ActorTrust.signer(path) }
        val link = Files.createSymbolicLink(root.resolve("link"), path)
        assertThrows(ActorRejected::class.java) { ActorTrust.signer(link) }
        listOf(ByteArray(0), ByteArray(4097), keys.public.encoded, "not DER".toByteArray()).forEach { bytes ->
            Files.write(path, bytes)
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"))
            assertThrows(ActorRejected::class.java) { ActorTrust.signer(path) }
        }
    }

    @Test
    fun `public verifier map rejects duplicate malformed excessive and writable trust`() {
        val encoded = Base64.getEncoder().encodeToString(keys.public.encoded)
        val path = file("public", """{"key_A":"$encoded"}""".toByteArray())
        assertEquals(keys.public, ActorTrust.verifiers(path).getValue("key_A"))
        val excessive = (1..5).joinToString(",", "{", "}") { "\"key_$it\":\"$encoded\"" }
        listOf(
            "{}",
            "[]",
            "null",
            "{\"key_A\":123}",
            "{\"key_A\":null}",
            """{"key_A":"$encoded","key_A":"$encoded"}""",
            """{"key_A":"$encoded"} {}""",
            excessive,
            """{"key_A":"not-base64"}""",
            """{"bad.id":"$encoded"}""",
        ).forEach { body ->
            Files.writeString(path, body)
            assertThrows(ActorRejected::class.java) { ActorTrust.verifiers(path) }
        }
        Files.writeString(path, """{"key_A":"$encoded"}""")
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-rw-r--"))
        assertThrows(ActorRejected::class.java) { ActorTrust.verifiers(path) }
    }

    private fun file(
        name: String,
        bytes: ByteArray,
    ): Path =
        Files.write(root.resolve(name), bytes).also {
            Files.setPosixFilePermissions(it, PosixFilePermissions.fromString("rw-------"))
        }
}
