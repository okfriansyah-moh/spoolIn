package com.spoolin.core.operations.crypto

import com.spoolin.core.features.account.persistence.AccountPayload
import com.spoolin.core.features.account.persistence.IdentityRules
import com.spoolin.core.features.crypto.*
import com.spoolin.core.features.session.SessionJdbcFixture
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class RotationCanaryBehaviorTest {
    @TempDir lateinit var root: Path
    private val id = UUID.randomUUID()
    private val piiBytes = ByteArray(32) { 23 }
    private val lookupBytes = ByteArray(32) { 29 }

    @Test
    fun `canary accepts authenticated source and rejects wrong row or damaged ciphertext`() = fixture { keys ->
        val envelope = envelope("value".toByteArray())
        for (record in listOf(record(envelope), record(envelope) + ("id" to UUID.randomUUID()), record(envelope) + ("cipher" to ByteArray(32)))) {
            val db = SessionJdbcFixture(listOf(listOf(record)))
            val canary = RotationSourceCanary(db, keys, "pii", KeyPurpose.PII)
            if (record["id"] == id && (record["cipher"] as ByteArray).contentEquals(envelope.cipher())) canary.verify()
            else assertThrows(IllegalStateException::class.java) { canary.verify() }
        }
        RotationSourceCanary(SessionJdbcFixture(listOf(emptyList())), keys, "pii", KeyPurpose.PII).verify()
    }

    @Test
    fun `lookup canary authenticates encrypted identity and independent email alias digest`() = fixture { keys ->
        val identity = IdentityRules().identity("Person", "Person@example.test")
        val envelope = envelope(AccountPayload().encode(identity))
        val digest = Mac.getInstance("HmacSHA256").also { it.init(SecretKeySpec(lookupBytes, "HmacSHA256")) }
            .doFinal(("email-lookup-v1\u0000" + identity.emailNormalized).toByteArray())
        RotationSourceCanary(SessionJdbcFixture(listOf(listOf(record(envelope) + ("digest" to digest)))), keys, "lookup", KeyPurpose.EMAIL_LOOKUP).verify()
        assertThrows(IllegalStateException::class.java) {
            RotationSourceCanary(SessionJdbcFixture(listOf(listOf(record(envelope) + ("digest" to ByteArray(32))))), keys, "lookup", KeyPurpose.EMAIL_LOOKUP).verify()
        }
        assertThrows(IllegalStateException::class.java) {
            RotationSourceCanary(SessionJdbcFixture(listOf(listOf(record(envelope("invalid".toByteArray())) + ("digest" to digest)))), keys, "lookup", KeyPurpose.EMAIL_LOOKUP).verify()
        }
    }

    private fun envelope(value: ByteArray) = AeadCipher().encrypt("pii", piiBytes, ByteArray(12) { 3 }, ProtectionBinding("core", "user_account", id, KeyPurpose.PII), value)
    private fun record(envelope: ProtectedEnvelope): Map<String, Any?> = mapOf("id" to id, "key_id" to "pii", "nonce" to envelope.nonce(), "cipher" to envelope.cipher(), "digest" to null)
    private fun fixture(test: (MountedKeys) -> Unit) {
        val pii = root.resolve("pii"); Files.write(pii, piiBytes)
        val lookup = root.resolve("lookup"); Files.write(lookup, lookupBytes)
        listOf(pii, lookup).forEach { Files.setPosixFilePermissions(it, PosixFilePermissions.fromString("rw-------")) }
        MountedKeys(mapOf("pii" to KeyMount(KeyPurpose.PII, pii), "lookup" to KeyMount(KeyPurpose.EMAIL_LOOKUP, lookup)), null).use(test)
    }
}
