package com.spoolin.core.features.crypto

import com.spoolin.core.features.session.SessionJdbcFixture
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class ProtectedMaterialBehaviorTest {
    @TempDir lateinit var root: Path
    private val row = UUID.randomUUID()
    private val material = ByteArray(32) { 6 }

    @Test
    fun `mounted material returns independent copies with purpose restriction and erases availability on close`() {
        val file = key("pii", material)
        val keys = MountedKeys(mapOf("pii" to KeyMount(KeyPurpose.PII, file)), null)
        val first = keys.key("pii", KeyPurpose.PII)!!
        first.fill(0)
        assertArrayEquals(material, keys.key("pii", KeyPurpose.PII))
        assertNull(keys.key("pii", KeyPurpose.DRAFT))
        assertNull(keys.key("unknown", KeyPurpose.PII))
        keys.close()
        assertNull(keys.key("pii", KeyPurpose.PII))
    }

    @Test
    fun `mounted material rejects unsafe permissions lengths identifiers and reused key bytes`() {
        val file = key("valid", material)
        assertThrows(IllegalArgumentException::class.java) { MountedKeys(mapOf("../key" to KeyMount(KeyPurpose.PII, file)), null) }
        assertThrows(IllegalArgumentException::class.java) {
            MountedKeys(
                mapOf(
                    "one" to KeyMount(KeyPurpose.PII, file),
                    "two" to KeyMount(KeyPurpose.DRAFT, file),
                ),
                null,
            )
        }
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-r--r--"))
        assertThrows(IllegalArgumentException::class.java) { MountedKeys(mapOf("one" to KeyMount(KeyPurpose.PII, file)), null) }
        val short = key("short", ByteArray(31))
        assertThrows(IllegalArgumentException::class.java) { MountedKeys(mapOf("one" to KeyMount(KeyPurpose.PII, short)), null) }
    }

    @Test
    fun `registry fails closed on missing keys and exposes only registered lookup versions`() {
        val db = SessionJdbcFixture(emptyList())
        val registry = KeyRegistry(db)
        db.enqueue()
        assertThrows(ProtectionUnavailable::class.java) { registry.writeKey(KeyPurpose.PII) }
        db.enqueue(mapOf("id" to "pii"))
        assertEquals(KeyState.WRITE, registry.writeKey(KeyPurpose.PII).state)
        db.enqueue()
        assertFalse(registry.readable("pii", KeyPurpose.PII))
        db.enqueue(mapOf("id" to "pii"))
        assertTrue(registry.readable("pii", KeyPurpose.PII))
        db.enqueue(mapOf("id" to "lookup", "state" to "READ"))
        assertEquals(KeyState.READ, registry.lookupKeys(db.jdbc).single().state)
        db.enqueue()
        assertThrows(ProtectionUnavailable::class.java) { registry.lookupKeys(db.jdbc) }
    }

    @Test
    fun `nonce allocation returns reserved counter and distinguishes exhaustion from unavailable key`() {
        val db = SessionJdbcFixture(emptyList())
        db.enqueue(mapOf("allocated" to 42L))
        assertArrayEquals(NonceCounter.encode("42"), NonceAllocator(db).reserve("pii", KeyPurpose.PII).bytes())
        assertEquals(1, db.commits)
        for ((exhausted, expected) in listOf(true to ProtectionFailure.NONCE_EXHAUSTION, false to ProtectionFailure.UNAVAILABLE)) {
            db.enqueue()
            db.enqueue(mapOf("1" to exhausted))
            assertEquals(
                expected,
                assertThrows(ProtectionUnavailable::class.java) { NonceAllocator(db).reserve("pii", KeyPurpose.PII) }.reason,
            )
        }
    }

    @Test
    fun `account encryption binds row identity and lookup digest uses independent key purpose`() {
        val db = SessionJdbcFixture(emptyList())
        val pii = key("pii", material)
        val lookupBytes = ByteArray(32) { 9 }
        val lookup = key("lookup", lookupBytes)
        val alerts = mutableListOf<UUID>()
        val mounts =
            mapOf(
                "pii" to KeyMount(KeyPurpose.PII, pii),
                "lookup" to KeyMount(KeyPurpose.EMAIL_LOOKUP, lookup),
            )
        MountedKeys(mounts, null).use { keys ->
            val crypto = AccountCryptography(KeyRegistry(db), keys, NonceAllocator(db), AeadCipher(), ProtectionAlert { alerts.add(it) })
            db.enqueue(mapOf("id" to "pii"))
            db.enqueue(mapOf("allocated" to 7L))
            val envelope = crypto.encrypt(row, "private value".toByteArray())
            db.enqueue(mapOf("id" to "pii"))
            assertArrayEquals("private value".toByteArray(), (crypto.decrypt(row, envelope) as ProtectedRead.Value).value)
            db.enqueue(mapOf("id" to "pii"))
            assertEquals(ProtectedRead.Unavailable, crypto.decrypt(UUID.randomUUID(), envelope))
            assertEquals(1, alerts.size)
            val email = "person@example.test"
            val expected =
                Mac.getInstance("HmacSHA256").also { it.init(SecretKeySpec(lookupBytes, "HmacSHA256")) }.doFinal(
                    (
                        "email-lookup-v1\u0000" +
                            email
                    ).toByteArray(),
                )
            assertArrayEquals(expected, crypto.lookupDigest(RegisteredKey("lookup", KeyPurpose.EMAIL_LOOKUP), email))
            assertThrows(IllegalArgumentException::class.java) { crypto.lookupDigest(RegisteredKey("pii", KeyPurpose.PII), email) }
        }
    }

    private fun key(
        name: String,
        bytes: ByteArray,
    ): Path =
        root.resolve(name).also {
            Files.write(it, bytes)
            Files.setPosixFilePermissions(it, PosixFilePermissions.fromString("rw-------"))
        }
}
