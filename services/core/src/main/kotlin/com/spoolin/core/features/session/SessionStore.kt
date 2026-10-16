package com.spoolin.core.features.session

import com.spoolin.core.features.account.persistence.AccountRole
import com.spoolin.support.receipts.committed
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import java.sql.Connection
import java.sql.Timestamp
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

private const val SESSION_HOURS = 12L
private const val SESSION_LIMIT_OFFSET = 4
private const val SESSION_IDLE_MINUTES = 30L
private val SESSION_IDLE = Duration.ofMinutes(SESSION_IDLE_MINUTES)

class SessionStore(
    private val dataSource: DataSource,
    private val csrf: SessionCsrf,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun issue(
        connection: Connection,
        accountId: UUID,
        expectedEpoch: Long,
        previous: SessionCredential?,
        id: UUID = UUID.randomUUID(),
    ): IssuedSession {
        val account = lockAccount(connection, accountId)
        check(account.second == expectedEpoch) { "Credential authority changed" }
        val now = clock.instant()
        retire(connection, accountId, previous, now)
        val credential = SessionCredential.issue()
        val token = csrf.derive(credential)
        val expires = now.plus(Duration.ofHours(SESSION_HOURS))
        connection.executeBound(
            "INSERT INTO browser_session(token_hash,id,account_id,auth_epoch,csrf_hash," +
                "created_at,last_seen_at,expires_at) " +
                "VALUES (?,?,?,?,?,?,?,?)",
            credential.digest(),
            id,
            accountId,
            expectedEpoch,
            csrf.digest(token),
            Timestamp.from(now),
            Timestamp.from(now),
            Timestamp.from(expires),
        )
        return IssuedSession(SessionPrincipal(id, accountId, account.first, expires, token), credential)
    }

    fun find(credential: SessionCredential): SessionPrincipal? =
        dataSource.committed { connection ->
            val accountId =
                connection
                    .queryBound(
                        "SELECT account_id FROM browser_session WHERE token_hash=?",
                        listOf(credential.digest()),
                    ) { it.getObject("account_id", UUID::class.java) }
                    .singleOrNull() ?: return@committed null
            val account = lockAccount(connection, accountId)
            verified(connection, credential, accountId, account)
        }

    fun logoutIdentity(
        credential: SessionCredential,
        suppliedCsrf: String,
    ): LogoutIdentity? {
        if (!csrf.matches(suppliedCsrf, csrf.digest(csrf.derive(credential)))) return null
        return dataSource.connection.use { connection ->
            connection
                .queryBound(
                    "SELECT id,account_id FROM browser_session WHERE token_hash=? AND csrf_hash=?",
                    listOf(credential.digest(), csrf.digest(suppliedCsrf)),
                ) { LogoutIdentity(it.getObject("id", UUID::class.java), it.getObject("account_id", UUID::class.java)) }
                .singleOrNull()
        }
    }

    fun revoke(
        connection: Connection,
        id: UUID,
        accountId: UUID,
    ) {
        connection.executeBound(
            "UPDATE browser_session SET revoked_at=COALESCE(revoked_at,?) WHERE id=? AND account_id=?",
            Timestamp.from(clock.instant()),
            id,
            accountId,
        )
    }

    private fun verified(
        connection: Connection,
        credential: SessionCredential,
        accountId: UUID,
        account: Pair<AccountRole, Long>,
    ): SessionPrincipal? {
        val stored =
            connection
                .queryBound(
                    "SELECT * FROM browser_session WHERE token_hash=? FOR UPDATE",
                    listOf(credential.digest()),
                ) {
                    SessionRow(
                        it.getObject("id", UUID::class.java),
                        it.getLong("auth_epoch"),
                        it.getBytes("csrf_hash"),
                        SessionWindow(
                            it.getTimestamp("created_at").toInstant(),
                            it.getTimestamp("last_seen_at").toInstant(),
                            it.getTimestamp("expires_at").toInstant(),
                            it.getTimestamp("revoked_at")?.toInstant(),
                        ),
                    )
                }.singleOrNull() ?: return null
        val now = clock.instant()
        val token = csrf.derive(credential)
        return if (stored.window.eligible(now, stored.epoch, account.second) && csrf.matches(token, stored.csrfHash)) {
            if (stored.window.shouldTouch(now)) {
                connection.executeBound(
                    "UPDATE browser_session SET last_seen_at=? WHERE id=?",
                    Timestamp.from(now),
                    stored.id,
                )
            }
            SessionPrincipal(stored.id, accountId, account.first, stored.window.expires, token)
        } else {
            null
        }
    }

    private fun lockAccount(
        connection: Connection,
        id: UUID,
    ): Pair<AccountRole, Long> =
        connection
            .queryBound("SELECT role,auth_epoch FROM user_account WHERE id=? FOR UPDATE", listOf(id)) {
                AccountRole.valueOf(it.getString("role")) to it.getLong("auth_epoch")
            }.single()

    private fun retire(
        connection: Connection,
        account: UUID,
        previous: SessionCredential?,
        now: Instant,
    ) {
        if (previous != null) {
            connection.executeBound(
                "UPDATE browser_session SET revoked_at=? WHERE account_id=? AND token_hash=? AND revoked_at IS NULL",
                Timestamp.from(now),
                account,
                previous.digest(),
            )
        }
        connection.executeBound(
            "UPDATE browser_session SET revoked_at=? WHERE id IN (SELECT id FROM browser_session " +
                "WHERE account_id=? AND revoked_at IS NULL AND expires_at>? AND last_seen_at>? " +
                "ORDER BY created_at DESC,id DESC OFFSET ?)",
            Timestamp.from(now),
            account,
            Timestamp.from(now),
            Timestamp.from(now.minus(SESSION_IDLE)),
            SESSION_LIMIT_OFFSET,
        )
    }
}

private class SessionRow(
    val id: UUID,
    val epoch: Long,
    val csrfHash: ByteArray,
    val window: SessionWindow,
)
