package com.spoolin.core.features.connector

import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

class ConnectorStore(
    private val dataSource: DataSource,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun findByTokenHash(tokenHash: ByteArray): ConnectorRecord? {
        require(tokenHash.size == 32)
        return dataSource.connection.use { connection ->
            connection
                .queryBound(
                    "SELECT id, account_id, client_kind, auth_epoch, scope, expires_at, revoked_at " +
                        "FROM mcp_credential WHERE token_hash = ?",
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

    fun insert(
        id: UUID,
        tokenHash: ByteArray,
        accountId: UUID,
        clientKind: String,
        authEpoch: Long,
        expiresAt: Instant,
    ) {
        require(tokenHash.size == 32)
        require(clientKind in setOf("CLAUDE", "CHATGPT"))
        dataSource.connection.use { connection ->
            connection.executeBound(
                "INSERT INTO mcp_credential " +
                    "(id, token_hash, account_id, client_kind, auth_epoch, scope, expires_at) " +
                    "VALUES (?, ?, ?, ?, ?, 'movie:propose movie:approve', ?)",
                id,
                tokenHash,
                accountId,
                clientKind,
                authEpoch,
                Timestamp.from(expiresAt),
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
