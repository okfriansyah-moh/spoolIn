package com.spoolin.core.features.crypto.rotation

import com.spoolin.core.features.account.persistence.AccountPayload
import com.spoolin.core.features.crypto.AeadCipher
import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.MountedKeys
import com.spoolin.core.features.crypto.ProtectedEnvelope
import com.spoolin.core.features.crypto.ProtectedRead
import com.spoolin.core.features.crypto.ProtectionBinding
import com.spoolin.core.features.crypto.queryBound
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.sql.DataSource

/** Authenticates a real source row before target activation, retaining purpose-bound alias checks. */
internal class RotationSourceCanary(
    private val dataSource: DataSource,
    private val keys: MountedKeys,
    private val id: String,
    private val purpose: KeyPurpose,
) {
    private val table = if (purpose == KeyPurpose.DRAFT) "form_draft" else "user_account"
    private val prefix = if (purpose == KeyPurpose.DRAFT) "body" else "pii"
    private val encryptionPurpose = if (purpose == KeyPurpose.EMAIL_LOOKUP) KeyPurpose.PII else purpose

    fun verify() {
        val source = read() ?: return
        val material =
            keys.key(source.envelope.keyId, encryptionPurpose)
                ?: error("Canary decryption key is unavailable")
        try {
            authenticate(source, material)
        } finally {
            material.fill(0)
        }
    }

    private fun read(): Source? =
        dataSource.connection.use { connection ->
            val from =
                if (purpose == KeyPurpose.EMAIL_LOOKUP) {
                    "appdata.user_account r JOIN appdata.account_email_lookup e ON e.account_id=r.id"
                } else {
                    "appdata.$table r"
                }
            val predicate = if (purpose == KeyPurpose.EMAIL_LOOKUP) "e.key_id=?" else "r.${prefix}_key_id=?"
            val digest = if (purpose == KeyPurpose.EMAIL_LOOKUP) "e.digest" else "NULL::bytea"
            connection
                .queryBound(
                    "SELECT r.id,r.${prefix}_key_id AS key_id,r.${prefix}_nonce AS nonce," +
                        "r.${prefix}_cipher AS cipher,$digest AS digest FROM $from WHERE $predicate ORDER BY r.id LIMIT 1",
                    listOf(id),
                ) {
                    Source(
                        it.getObject("id", UUID::class.java),
                        ProtectedEnvelope(1, it.getString("key_id"), it.getBytes("nonce"), it.getBytes("cipher")),
                        it.getBytes("digest"),
                    )
                }.singleOrNull()
        }

    private fun authenticate(
        source: Source,
        material: ByteArray,
    ) {
        val binding = ProtectionBinding("core", table, source.id, encryptionPurpose)
        val read = AeadCipher().decrypt(source.envelope, material, binding)
        check(read is ProtectedRead.Value) { "Existing protected canary failed authentication" }
        try {
            if (purpose == KeyPurpose.EMAIL_LOOKUP) verifyAlias(source, read.value)
        } finally {
            read.value.fill(0)
        }
    }

    private fun verifyAlias(
        source: Source,
        clear: ByteArray,
    ) {
        val identity = AccountPayload().decode(clear)
        check(identity is ProtectedRead.Value) { "Lookup canary identity is invalid" }
        val material = keys.key(id, KeyPurpose.EMAIL_LOOKUP) ?: error("Lookup key is unavailable")
        try {
            val expected = digest(material, identity.value.emailNormalized)
            try {
                check(MessageDigest.isEqual(expected, source.digest)) {
                    "Lookup canary does not match existing aliases"
                }
            } finally {
                expected.fill(0)
            }
        } finally {
            material.fill(0)
        }
    }

    private fun digest(
        material: ByteArray,
        normalized: String,
    ): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(material, "HmacSHA256"))
        return mac.doFinal(("email-lookup-v1\u0000" + normalized).toByteArray(Charsets.UTF_8))
    }

    private data class Source(
        val id: UUID,
        val envelope: ProtectedEnvelope,
        val digest: ByteArray?,
    )
}
