package com.spoolin.core.features.crypto.rotation

import com.spoolin.core.features.account.persistence.AccountPayload
import com.spoolin.core.features.crypto.AccountCryptography
import com.spoolin.core.features.crypto.AeadCipher
import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.KeyRegistry
import com.spoolin.core.features.crypto.MountedKeys
import com.spoolin.core.features.crypto.NonceAllocator
import com.spoolin.core.features.crypto.ProtectedEnvelope
import com.spoolin.core.features.crypto.ProtectedRead
import com.spoolin.core.features.crypto.ProtectionBinding
import com.spoolin.core.features.crypto.RegisteredKey
import com.spoolin.core.features.crypto.committed
import com.spoolin.core.features.crypto.executeBound
import com.spoolin.core.features.crypto.queryBound
import java.security.MessageDigest
import java.util.UUID
import javax.sql.DataSource

class RotationBackfill(
    private val dataSource: DataSource,
    private val registry: KeyRegistry,
    private val keys: MountedKeys,
    private val plan: KeyRotationPlan,
) {
    private val nonces = NonceAllocator(dataSource)
    private val cipher = AeadCipher()
    private val table = if (plan.purpose == KeyPurpose.DRAFT) "form_draft" else "user_account"
    private val prefix = if (plan.purpose == KeyPurpose.DRAFT) "body" else "pii"
    private val accountCrypto = AccountCryptography(registry, keys, nonces, cipher) { }

    fun run(maxBatches: Int): RotationOutcome {
        var changed = 0
        repeat(maxBatches) {
            val rows = batch()
            if (rows.isEmpty()) return completed(changed)
            rows.forEach { row ->
                changed += if (plan.purpose == KeyPurpose.EMAIL_LOOKUP) alias(row) else reencrypt(row)
            }
        }
        return completed(changed)
    }

    private fun batch(): List<Row> =
        dataSource.connection.use { connection ->
            val predicate =
                if (plan.purpose == KeyPurpose.EMAIL_LOOKUP) {
                    "NOT EXISTS(SELECT 1 FROM appdata.account_email_lookup e WHERE e.account_id=r.id AND e.key_id=?)"
                } else {
                    "r.${prefix}_key_id=?"
                }
            connection.queryBound(
                "SELECT r.id,r.version,r.${prefix}_key_id AS key_id,r.${prefix}_nonce AS nonce," +
                    "r.${prefix}_cipher AS cipher " +
                    "FROM appdata.$table r WHERE $predicate ORDER BY r.id LIMIT 100",
                listOf(
                    if (plan.purpose ==
                        KeyPurpose.EMAIL_LOOKUP
                    ) {
                        plan.toAlias
                    } else {
                        plan.fromAlias
                    },
                ),
            ) {
                Row(
                    it.getObject("id", UUID::class.java),
                    it.getLong("version"),
                    ProtectedEnvelope(1, it.getString("key_id"), it.getBytes("nonce"), it.getBytes("cipher")),
                )
            }
        }

    private fun plaintext(row: Row): ByteArray {
        val purpose = if (plan.purpose == KeyPurpose.EMAIL_LOOKUP) KeyPurpose.PII else plan.purpose
        check(registry.readable(row.envelope.keyId, purpose)) { "A required read key is unavailable" }
        val material = keys.key(row.envelope.keyId, purpose) ?: error("A required protected key is unavailable")
        return try {
            val binding = ProtectionBinding("core", table, row.id, purpose)
            when (val read = cipher.decrypt(row.envelope, material, binding)) {
                is ProtectedRead.Value -> read.value
                ProtectedRead.Unavailable -> error("Protected rotation input could not be authenticated")
            }
        } finally {
            material.fill(0)
        }
    }

    private fun reencrypt(row: Row): Int {
        val clear = plaintext(row)
        val material = keys.key(plan.toAlias, plan.purpose) ?: error("Rotation target material is unavailable")
        try {
            check(registry.writeKey(plan.purpose).id == plan.toAlias) { "Rotation target is no longer active" }
            val nonce = nonces.reserve(plan.toAlias, plan.purpose)
            val envelope =
                cipher.encrypt(
                    plan.toAlias,
                    material,
                    nonce.bytes(),
                    ProtectionBinding("core", table, row.id, plan.purpose),
                    clear,
                )
            return dataSource.committed { connection ->
                currentWrite(connection)
                connection.executeBound(
                    "UPDATE appdata.$table SET ${prefix}_key_id=?,${prefix}_nonce=?,${prefix}_cipher=? " +
                        "WHERE id=? AND version=? AND ${prefix}_key_id=? AND ${prefix}_nonce=? AND ${prefix}_cipher=?",
                    envelope.keyId,
                    envelope.nonce(),
                    envelope.cipher(),
                    row.id,
                    row.version,
                    row.envelope.keyId,
                    row.envelope.nonce(),
                    row.envelope.cipher(),
                )
            }
        } finally {
            clear.fill(0)
            material.fill(0)
        }
    }

    private fun alias(row: Row): Int {
        val clear = plaintext(row)
        try {
            val identity = AccountPayload().decode(clear)
            check(identity is ProtectedRead.Value) { "Protected identity is invalid" }
            val target =
                accountCrypto.lookupDigest(
                    RegisteredKey(plan.toAlias, KeyPurpose.EMAIL_LOOKUP),
                    identity.value.emailNormalized,
                )
            try {
                return persistAlias(row, target)
            } finally {
                target.fill(0)
            }
        } finally {
            clear.fill(0)
        }
    }

    private fun persistAlias(
        row: Row,
        target: ByteArray,
    ): Int =
        dataSource.committed { connection ->
            val compatible = registry.lookupKeys(connection)
            check(compatible.any { it.id == plan.toAlias && it.state == com.spoolin.core.features.crypto.KeyState.WRITE })
            if (!unchanged(connection, row)) return@committed 0
            val inserted =
                connection.executeBound(
                    "INSERT INTO appdata.account_email_lookup(key_id,digest,account_id) " +
                        "VALUES(?,?,?) ON CONFLICT DO NOTHING",
                    plan.toAlias,
                    target,
                    row.id,
                )
            verifyAlias(connection, row, target)
            inserted
        }

    private fun unchanged(
        connection: java.sql.Connection,
        row: Row,
    ): Boolean =
        connection
            .queryBound(
                "SELECT id FROM appdata.user_account WHERE id=? AND version=? AND pii_key_id=? " +
                    "AND pii_nonce=? AND pii_cipher=? FOR UPDATE",
                listOf(row.id, row.version, row.envelope.keyId, row.envelope.nonce(), row.envelope.cipher()),
            ) { 1 }
            .isNotEmpty()

    private fun verifyAlias(
        connection: java.sql.Connection,
        row: Row,
        target: ByteArray,
    ) {
        val stored =
            connection
                .queryBound(
                    "SELECT digest FROM appdata.account_email_lookup WHERE key_id=? AND account_id=?",
                    listOf(plan.toAlias, row.id),
                ) { it.getBytes("digest") }
                .singleOrNull()
        check(stored != null && MessageDigest.isEqual(stored, target)) {
            "Lookup alias collision or incompatible identity; rotation stopped"
        }
    }

    private fun currentWrite(connection: java.sql.Connection) {
        check(
            connection
                .queryBound(
                    "SELECT id FROM appdata.crypto_key_version WHERE id=? AND purpose=? AND state='WRITE' FOR SHARE",
                    listOf(plan.toAlias, plan.purpose.name),
                ) {
                    1
                }.size ==
                1,
        )
    }

    private fun completed(changed: Int): RotationOutcome {
        val remaining =
            dataSource.connection.use { connection ->
                val sql =
                    if (plan.purpose ==
                        KeyPurpose.EMAIL_LOOKUP
                    ) {
                        "SELECT count(*) AS total FROM appdata.user_account a WHERE NOT EXISTS(" +
                            "SELECT 1 FROM appdata.account_email_lookup e WHERE e.account_id=a.id AND e.key_id=?)"
                    } else {
                        "SELECT count(*) AS total FROM appdata.$table WHERE ${prefix}_key_id=?"
                    }
                connection
                    .queryBound(
                        sql,
                        listOf(
                            if (plan.purpose ==
                                KeyPurpose.EMAIL_LOOKUP
                            ) {
                                plan.toAlias
                            } else {
                                plan.fromAlias
                            },
                        ),
                    ) { it.getLong("total") }
                    .single()
            }
        return RotationOutcome(
            remaining == 0L,
            if (remaining ==
                0L
            ) {
                "Write key active and live backfill verified; old key retained READ for backup review"
            } else {
                "Write key active; backfill remains pending and can resume; old key retained READ"
            },
            changed,
            remaining,
        )
    }

    private data class Row(
        val id: UUID,
        val version: Long,
        val envelope: ProtectedEnvelope,
    )
}
