package com.spoolin.core.features.restore

import com.spoolin.core.features.account.persistence.AccountPayload
import com.spoolin.core.features.crypto.AeadCipher
import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.MountedKeys
import com.spoolin.core.features.crypto.ProtectedEnvelope
import com.spoolin.core.features.crypto.ProtectedRead
import com.spoolin.core.features.crypto.ProtectionBinding
import com.spoolin.core.features.crypto.queryBound
import com.spoolin.core.features.draft.DraftPayload
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.sql.DataSource

/** Authenticates every restored encrypted row and every retained lookup alias without exposing plaintext. */
object RestoreVerification {
    fun verify(
        source: DataSource,
        keys: MountedKeys,
    ): Map<String, Long> =
        mapOf(
            "accounts" to verifyRows(source, keys, KeyPurpose.PII),
            "drafts" to verifyRows(source, keys, KeyPurpose.DRAFT),
        )

    private fun verifyRows(
        source: DataSource,
        keys: MountedKeys,
        purpose: KeyPurpose,
    ): Long {
        val table = RestoredTable.forPurpose(purpose)
        var after: UUID? = null
        var total = 0L
        while (true) {
            val rows = rows(source, table, after)
            if (rows.isEmpty()) return total
            rows.forEach { authenticate(source, keys, purpose, table.name, it) }
            after = rows.last().id
            total += rows.size
        }
    }

    private fun rows(
        source: DataSource,
        table: RestoredTable,
        after: UUID?,
    ): List<Row> {
        val predicate = if (after == null) "" else "WHERE id>? "
        val arguments = if (after == null) emptyList() else listOf(after)
        val prefix = table.prefix
        return source.connection.use { connection ->
            connection.queryBound(
                "SELECT id,${prefix}_key_id AS key_id,${prefix}_nonce AS nonce,${prefix}_cipher AS cipher," +
                    "${table.fields} FROM appdata.${table.name} ${predicate}ORDER BY id LIMIT 100",
                arguments,
            ) { row ->
                Row(
                    row.getObject("id", UUID::class.java),
                    ProtectedEnvelope(1, row.getString("key_id"), row.getBytes("nonce"), row.getBytes("cipher")),
                    row.getString("target_kind"),
                )
            }
        }
    }

    private fun authenticate(
        source: DataSource,
        keys: MountedKeys,
        purpose: KeyPurpose,
        table: String,
        row: Row,
    ) {
        val material = keys.key(row.envelope.keyId, purpose) ?: error("Required restore read key missing")
        try {
            val read = AeadCipher().decrypt(row.envelope, material, ProtectionBinding("core", table, row.id, purpose))
            check(read is ProtectedRead.Value) { "Restored ciphertext authentication failed" }
            try {
                verifyClearContent(source, keys, purpose, row, read.value)
            } finally {
                read.value.fill(0)
            }
        } finally {
            material.fill(0)
        }
    }

    private fun verifyClearContent(
        source: DataSource,
        keys: MountedKeys,
        purpose: KeyPurpose,
        row: Row,
        clear: ByteArray,
    ) {
        if (purpose == KeyPurpose.PII) {
            verifyAliases(source, keys, row.id, clear)
        } else {
            DraftPayload().encode(requireNotNull(row.kind), DraftPayload().decode(clear)).fill(0)
        }
    }

    private fun verifyAliases(
        source: DataSource,
        keys: MountedKeys,
        id: UUID,
        clear: ByteArray,
    ) {
        val identity = AccountPayload().decode(clear)
        check(identity is ProtectedRead.Value) { "Restored identity envelope invalid" }
        val aliases =
            source.connection.use { connection ->
                connection.queryBound(
                    "SELECT e.key_id,e.digest,k.state,k.purpose FROM appdata.account_email_lookup e " +
                        "JOIN appdata.crypto_key_version k ON k.id=e.key_id WHERE e.account_id=?",
                    listOf(id),
                ) {
                    check(it.getString("purpose") == "EMAIL_LOOKUP")
                    Triple(it.getString("key_id"), it.getBytes("digest"), it.getString("state"))
                }
            }
        check(aliases.any { it.third == "WRITE" }) { "Restored account lacks active lookup alias" }
        aliases.filter { it.third in setOf("WRITE", "READ") }.forEach { (key, stored, _) ->
            verifyAlias(keys, identity.value.emailNormalized, key, stored)
        }
    }

    private fun verifyAlias(
        keys: MountedKeys,
        email: String,
        key: String,
        stored: ByteArray,
    ) {
        val material = keys.key(key, KeyPurpose.EMAIL_LOOKUP) ?: error("Required lookup recovery key missing")
        try {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(material, "HmacSHA256"))
            val expected = mac.doFinal(("email-lookup-v1\u0000" + email).toByteArray(Charsets.UTF_8))
            try {
                check(MessageDigest.isEqual(expected, stored)) { "Restored lookup identity differs" }
            } finally {
                expected.fill(0)
            }
        } finally {
            material.fill(0)
        }
    }

    private data class RestoredTable(
        val name: String,
        val prefix: String,
        val fields: String,
    ) {
        companion object {
            fun forPurpose(purpose: KeyPurpose): RestoredTable =
                if (purpose == KeyPurpose.PII) {
                    RestoredTable("user_account", "pii", "NULL::text AS target_kind")
                } else {
                    RestoredTable("form_draft", "body", "target_kind")
                }
        }
    }

    private data class Row(
        val id: UUID,
        val envelope: ProtectedEnvelope,
        val kind: String?,
    )
}
