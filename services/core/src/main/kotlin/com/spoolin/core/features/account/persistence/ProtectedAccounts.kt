package com.spoolin.core.features.account.persistence

import com.spoolin.core.features.crypto.AccountCryptography
import com.spoolin.core.features.crypto.KeyRegistry
import com.spoolin.core.features.crypto.KeyState
import com.spoolin.core.features.crypto.ProtectedEnvelope
import com.spoolin.core.features.crypto.ProtectedRead
import com.spoolin.core.features.crypto.ProtectionAlert
import com.spoolin.core.features.crypto.ProtectionFailure
import com.spoolin.core.features.crypto.ProtectionUnavailable
import com.spoolin.core.features.crypto.StoredPasswordHash
import com.spoolin.core.features.crypto.committed
import com.spoolin.core.features.crypto.executeBound
import com.spoolin.core.features.crypto.observeProtection
import com.spoolin.core.features.crypto.queryBound
import com.spoolin.core.features.crypto.readRows
import com.spoolin.core.features.crypto.recordFailure
import java.sql.Connection
import java.sql.SQLException
import java.util.UUID
import javax.sql.DataSource

class ProtectedAccounts(
    private val dataSource: DataSource,
    private val registry: KeyRegistry,
    private val crypto: AccountCryptography,
    private val payload: AccountPayload,
    private val alert: ProtectionAlert,
    private val rules: IdentityRules = IdentityRules(),
) {
    fun insert(
        id: UUID,
        identity: AccountIdentity,
        password: StoredPasswordHash,
        role: AccountRole,
    ): AccountInsert =
        try {
            val prepared = prepare(id, identity, password, role)
            AccountInsert.Stored(dataSource.committed { connection -> insert(connection, prepared) })
        } catch (_: AccountIdentityConflict) {
            AccountInsert.Conflict
        } catch (failure: SQLException) {
            if (failure.sqlState == "23505") AccountInsert.Conflict else AccountInsert.Unavailable
        } catch (_: ProtectionUnavailable) {
            AccountInsert.Unavailable
        }

    fun prepare(
        id: UUID,
        identity: AccountIdentity,
        password: StoredPasswordHash,
        role: AccountRole,
    ): PreparedAccount {
        val checked = rules.identity(identity.name, identity.emailDisplay)
        require(checked.emailNormalized == identity.emailNormalized) { "Account identity is invalid" }
        val plaintext = payload.encode(checked)
        val envelope =
            try {
                crypto.encrypt(id, plaintext)
            } finally {
                plaintext.fill(0)
            }
        return PreparedAccount(id, identity.emailNormalized, envelope, password, role)
    }

    fun insert(
        connection: Connection,
        prepared: PreparedAccount,
    ): AccountProjection {
        val lookupKeys =
            observeProtection(alert, ProtectionFailure.KEY) {
                registry.requireCurrentWrite(connection, prepared.envelope.keyId, com.spoolin.core.features.crypto.KeyPurpose.PII)
                registry.lookupKeys(connection).also { keys ->
                    if (keys.none { it.state == KeyState.WRITE }) throw ProtectionUnavailable()
                }
            }
        val aliases = lookupKeys.map { it.id to crypto.lookupDigest(it, prepared.normalizedEmail) }
        insertAccount(connection, prepared.id, prepared.envelope, prepared.password, prepared.role)
        aliases.forEach { (key, digest) -> insertAlias(connection, prepared.id, key, digest) }
        return AccountProjection(prepared.id, prepared.role, 0)
    }

    fun identity(id: UUID): ProtectedRead<AccountIdentity> {
        val envelope =
            dataSource.readRows { connection ->
                connection
                    .queryBound(
                        "SELECT pii_key_id,pii_nonce,pii_cipher FROM appdata.user_account WHERE id=?",
                        listOf(id),
                    ) {
                        ProtectedEnvelope(
                            1,
                            it.getString("pii_key_id"),
                            it.getBytes("pii_nonce"),
                            it.getBytes("pii_cipher"),
                        )
                    }.singleOrNull()
            }
        return if (envelope == null) ProtectedRead.Unavailable else decodeIdentity(id, envelope)
    }

    fun credential(normalizedEmail: String): ProtectedRead<AccountCredential> =
        try {
            dataSource.committed { connection ->
                val matches =
                    observeProtection(alert, ProtectionFailure.KEY) { registry.lookupKeys(connection) }
                        .flatMap { key ->
                            credential(connection, key.id, crypto.lookupDigest(key, normalizedEmail))
                        }.distinctBy { it.id }
                if (matches.size == 1) ProtectedRead.Value(matches.single()) else ProtectedRead.Unavailable
            }
        } catch (_: ProtectionUnavailable) {
            ProtectedRead.Unavailable
        }

    fun replacePassword(
        id: UUID,
        previous: StoredPasswordHash,
        replacement: StoredPasswordHash,
    ): Boolean =
        dataSource.readRows { connection ->
            connection.executeBound(
                "UPDATE appdata.user_account SET password_hash=?,version=version+1,updated_at=CURRENT_TIMESTAMP " +
                    "WHERE id=? AND password_hash=?",
                replacement.encoded,
                id,
                previous.encoded,
            ) == 1
        }

    private fun decodeIdentity(
        id: UUID,
        envelope: ProtectedEnvelope,
    ): ProtectedRead<AccountIdentity> {
        val decrypted = crypto.decrypt(id, envelope)
        if (decrypted !is ProtectedRead.Value) return ProtectedRead.Unavailable
        return try {
            val decoded = payload.decode(decrypted.value)
            if (decoded == ProtectedRead.Unavailable) {
                alert.recordFailure(ProtectionFailure.UNAVAILABLE)
                alert.unavailable(id)
            }
            decoded
        } finally {
            decrypted.value.fill(0)
        }
    }

    private fun insertAccount(
        connection: Connection,
        id: UUID,
        envelope: ProtectedEnvelope,
        password: StoredPasswordHash,
        role: AccountRole,
    ) {
        connection.executeBound(
            "INSERT INTO appdata.user_account(id,pii_key_id,pii_nonce,pii_cipher,password_hash,role) " +
                "VALUES (?,?,?,?,?,?)",
            id,
            envelope.keyId,
            envelope.nonce(),
            envelope.cipher(),
            password.encoded,
            role.name,
        )
    }

    private fun insertAlias(
        connection: Connection,
        id: UUID,
        keyId: String,
        digest: ByteArray,
    ) {
        val inserted =
            connection.executeBound(
                "INSERT INTO appdata.account_email_lookup(key_id,digest,account_id) VALUES (?,?,?) " +
                    "ON CONFLICT (key_id,digest) DO NOTHING",
                keyId,
                digest,
                id,
            )
        // An expected duplicate must roll back the account without putting its lookup digest in database error logs.
        if (inserted != 1) throw AccountIdentityConflict()
    }

    private fun credential(
        connection: Connection,
        keyId: String,
        digest: ByteArray,
    ): List<AccountCredential> =
        connection.queryBound(
            "SELECT a.id,a.password_hash,a.auth_epoch FROM appdata.account_email_lookup e " +
                "JOIN appdata.user_account a ON a.id=e.account_id WHERE e.key_id=? AND e.digest=?",
            listOf(keyId, digest),
        ) {
            AccountCredential(
                it.getObject("id", UUID::class.java),
                StoredPasswordHash(it.getString("password_hash")),
                it.getLong("auth_epoch"),
            )
        }
}

internal class AccountIdentityConflict : IllegalStateException("Account cannot be created")

class PreparedAccount internal constructor(
    internal val id: UUID,
    internal val normalizedEmail: String,
    internal val envelope: ProtectedEnvelope,
    internal val password: StoredPasswordHash,
    internal val role: AccountRole,
) {
    override fun toString(): String = "PreparedAccount(content=redacted)"
}
