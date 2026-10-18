package com.spoolin.core.features.connector

import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

private const val TOKEN_HASH_BYTES = 32

class ConnectorStore(
    private val dataSource: DataSource,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun findByCredential(
        credentialId: UUID,
        accountId: UUID,
        authEpoch: Long,
    ): ConnectorRecord? =
        dataSource.connection.use { connection ->
            connection
                .queryBound(
                    "SELECT c.id, c.account_id, c.client_kind, c.auth_epoch, c.scope, c.expires_at, c.revoked_at " +
                        "FROM mcp_credential c JOIN user_account a ON a.id = c.account_id " +
                        "WHERE c.id = ? AND c.account_id = ? AND c.auth_epoch = ? AND a.auth_epoch = c.auth_epoch",
                    listOf(credentialId, accountId, authEpoch),
                ) { row ->
                    ConnectorRecord(
                        id = row.getObject("id", UUID::class.java),
                        accountId = row.getObject("account_id", UUID::class.java),
                        clientKind = row.getString("client_kind"),
                        authEpoch = row.getLong("auth_epoch"),
                        scope = row.getString("scope"),
                        expiresAt = row.getTimestamp("expires_at").toInstant(),
                        revokedAt = row.getTimestamp("revoked_at")?.toInstant(),
                    )
                }.singleOrNull()
        }

    fun findByTokenHash(tokenHash: ByteArray): ConnectorRecord? {
        require(tokenHash.size == TOKEN_HASH_BYTES)
        return dataSource.connection.use { connection ->
            connection
                .queryBound(
                    "SELECT c.id, c.account_id, c.client_kind, c.auth_epoch, c.scope, c.expires_at, c.revoked_at " +
                        "FROM mcp_credential c JOIN user_account a ON a.id = c.account_id " +
                        "WHERE c.token_hash = ? AND a.auth_epoch = c.auth_epoch",
                    listOf(tokenHash),
                ) { row ->
                    ConnectorRecord(
                        id = row.getObject("id", UUID::class.java),
                        accountId = row.getObject("account_id", UUID::class.java),
                        clientKind = row.getString("client_kind"),
                        authEpoch = row.getLong("auth_epoch"),
                        scope = row.getString("scope"),
                        expiresAt = row.getTimestamp("expires_at").toInstant(),
                        revokedAt = row.getTimestamp("revoked_at")?.toInstant(),
                    )
                }.singleOrNull()
        }
    }

    fun insert(credential: ConnectorInsert) {
        require(credential.tokenHash.size == TOKEN_HASH_BYTES)
        require(credential.clientKind in setOf("CLAUDE", "CHATGPT"))
        dataSource.connection.use { connection ->
            connection.executeBound(
                "INSERT INTO mcp_credential " +
                    "(id, token_hash, account_id, client_kind, auth_epoch, scope, expires_at) " +
                    "VALUES (?, ?, ?, ?, ?, 'movie:propose movie:approve', ?)",
                credential.id,
                credential.tokenHash,
                credential.accountId,
                credential.clientKind,
                credential.authEpoch,
                Timestamp.from(credential.expiresAt),
            )
        }
    }

    fun live(record: ConnectorRecord): Boolean {
        val now = clock.instant()
        return record.revokedAt == null && record.expiresAt.isAfter(now)
    }

    fun accountRole(accountId: UUID): String? =
        dataSource.connection.use { connection ->
            connection
                .queryBound("SELECT role FROM user_account WHERE id = ?", listOf(accountId)) {
                    it.getString("role")
                }.singleOrNull()
        }
}

/** One credential insertion; token hashes are never diagnostic output. */
data class ConnectorInsert(
    val id: UUID,
    val tokenHash: ByteArray,
    val accountId: UUID,
    val clientKind: String,
    val authEpoch: Long,
    val expiresAt: Instant,
) {
    override fun toString(): String = "ConnectorInsert(redacted)"
}
