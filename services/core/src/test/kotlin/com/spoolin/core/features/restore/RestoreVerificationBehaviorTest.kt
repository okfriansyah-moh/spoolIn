package com.spoolin.core.features.restore

import com.spoolin.core.features.account.persistence.AccountPayload
import com.spoolin.core.features.account.persistence.IdentityRules
import com.spoolin.core.features.crypto.AeadCipher
import com.spoolin.core.features.crypto.KeyMount
import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.MountedKeys
import com.spoolin.core.features.crypto.ProtectionBinding
import com.spoolin.core.features.session.SessionJdbcFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class RestoreVerificationBehaviorTest {
    @TempDir lateinit var root: Path
    private val id = UUID.randomUUID()
    private val pii = ByteArray(32) { 47 }
    private val draft = ByteArray(32) { 53 }
    private val lookup = ByteArray(32) { 59 }
    private val identity = IdentityRules().identity("Person", "person@example.test")

    @Test
    fun `complete restore authenticates accounts drafts and active lookup aliases and counts all rows`() =
        fixture { db, keys ->
            db.enqueue(row(KeyPurpose.PII, AccountPayload().encode(identity)))
            db.enqueue(alias(true))
            db.enqueue()
            db.enqueue(row(KeyPurpose.DRAFT, "{\"name\":\"incomplete form\"}".toByteArray()))
            db.enqueue()
            assertEquals(mapOf("accounts" to 1L, "drafts" to 1L), RestoreVerification.verify(db, keys))
        }

    @Test
    fun `empty restored tables return zero counts without inventing records`() =
        fixture { db, keys ->
            db.enqueue()
            db.enqueue()
            assertEquals(mapOf("accounts" to 0L, "drafts" to 0L), RestoreVerification.verify(db, keys))
        }

    @Test
    fun `wrong alias digest absent active alias or malformed decrypted identity stops restore`() {
        for (mode in listOf("digest", "missing", "identity")) {
            fixture { db, keys ->
                db.enqueue(row(KeyPurpose.PII, if (mode == "identity") "{}".toByteArray() else AccountPayload().encode(identity)))
                if (mode == "digest") db.enqueue(alias(false))
                if (mode == "missing") db.enqueue()
                assertThrows(IllegalStateException::class.java) { RestoreVerification.verify(db, keys) }
            }
        }
    }

    @Test
    fun `damaged ciphertext or wrong retained key purpose stops restore before plaintext projection`() =
        fixture { db, keys ->
            db.enqueue(row(KeyPurpose.PII, AccountPayload().encode(identity)) + ("cipher" to ByteArray(32)))
            assertThrows(IllegalStateException::class.java) { RestoreVerification.verify(db, keys) }
            db.enqueue(row(KeyPurpose.PII, AccountPayload().encode(identity)) + ("key_id" to "draft"))
            assertThrows(IllegalStateException::class.java) { RestoreVerification.verify(db, keys) }
        }

    @Test
    fun `decrypted draft is validated against allowed target fields`() =
        fixture { db, keys ->
            db.enqueue()
            db.enqueue(row(KeyPurpose.DRAFT, "{\"password\":\"secret\"}".toByteArray()))
            assertThrows(IllegalArgumentException::class.java) { RestoreVerification.verify(db, keys) }
        }

    private fun alias(valid: Boolean): Map<String, Any?> {
        val digest =
            Mac.getInstance("HmacSHA256").also { it.init(SecretKeySpec(lookup, "HmacSHA256")) }.doFinal(
                (
                    "email-lookup-v1\u0000" +
                        identity.emailNormalized
                ).toByteArray(),
            )
        return mapOf(
            "key_id" to "lookup",
            "digest" to if (valid) digest else ByteArray(32),
            "state" to "WRITE",
            "purpose" to "EMAIL_LOOKUP",
        )
    }

    private fun row(
        purpose: KeyPurpose,
        clear: ByteArray,
    ): Map<String, Any?> {
        val key = if (purpose == KeyPurpose.PII) pii else draft
        val name = if (purpose == KeyPurpose.PII) "pii" else "draft"
        val table = if (purpose == KeyPurpose.PII) "user_account" else "form_draft"
        val envelope = AeadCipher().encrypt(name, key, ByteArray(12) { 4 }, ProtectionBinding("core", table, id, purpose), clear)
        return mapOf(
            "id" to id,
            "key_id" to name,
            "nonce" to envelope.nonce(),
            "cipher" to envelope.cipher(),
            "target_kind" to if (purpose == KeyPurpose.DRAFT) "PERSON" else null,
        )
    }

    private fun fixture(test: (SessionJdbcFixture, MountedKeys) -> Unit) {
        val mounts =
            listOf(
                Triple("pii", KeyPurpose.PII, pii),
                Triple("draft", KeyPurpose.DRAFT, draft),
                Triple("lookup", KeyPurpose.EMAIL_LOOKUP, lookup),
            ).associate { (id, purpose, bytes) ->
                val file = root.resolve(id)
                Files.write(file, bytes)
                Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"))
                id to KeyMount(purpose, file)
            }
        MountedKeys(mounts, null).use { test(SessionJdbcFixture(emptyList()), it) }
    }
}
