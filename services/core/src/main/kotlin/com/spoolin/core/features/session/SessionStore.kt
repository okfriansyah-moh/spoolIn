package com.spoolin.core.features.session

import com.spoolin.core.identity.AccountAuthority
import com.spoolin.core.identity.AccountRole
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
private const val SESSION_RETENTION_DAYS = 7L
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
        val account = issuanceAccount(connection, accountId, previous)
        check(account.authEpoch == expectedEpoch) { "Credential authority changed" }
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
        return IssuedSession(SessionPrincipal(id, account, expires, token), credential)
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
            verified(connection, credential, account)
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

    @org.springframework.scheduling.annotation.Scheduled(fixedDelayString = "PT1H")
    fun purge(): Int =
        dataSource.committed { connection ->
            val cutoff = Timestamp.from(clock.instant().minus(Duration.ofDays(SESSION_RETENTION_DAYS)))
            connection.executeBound(
                "DELETE FROM browser_session WHERE revoked_at<=? OR " +
                    "LEAST(expires_at,last_seen_at + INTERVAL '30 minutes')<=?",
                cutoff,
                cutoff,
            )
        }

    private fun verified(
        connection: Connection,
        credential: SessionCredential,
        account: AccountAuthority,
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
        val eligible = stored.window.eligible(now, stored.epoch, account.authEpoch)
        return if (eligible && csrf.matches(token, stored.csrfHash)) {
            if (stored.window.shouldTouch(now)) {
                connection.executeBound(
                    "UPDATE browser_session SET last_seen_at=? WHERE id=?",
                    Timestamp.from(now),
                    stored.id,
                )
            }
            SessionPrincipal(stored.id, account, stored.window.expires, token)
        } else {
            null
        }
    }

    private fun lockAccount(
        connection: Connection,
        id: UUID,
    ): AccountAuthority =
        connection
            .queryBound("SELECT role,auth_epoch FROM user_account WHERE id=? FOR UPDATE", listOf(id)) {
                AccountAuthority(id, AccountRole.valueOf(it.getString("role")), it.getLong("auth_epoch"))
            }.single()

    private fun issuanceAccount(
        connection: Connection,
        accountId: UUID,
        previous: SessionCredential?,
    ): AccountAuthority {
        val prior =
            previous?.let { credential ->
                connection
                    .queryBound(
                        "SELECT account_id FROM browser_session WHERE token_hash=?",
                        listOf(credential.digest()),
                    ) { it.getObject("account_id", UUID::class.java) }
                    .singleOrNull()
            }
        listOfNotNull(accountId, prior).distinct().sortedBy(UUID::toString).forEach { lockAccount(connection, it) }
        return lockAccount(connection, accountId)
    }

    private fun retire(
        connection: Connection,
        account: UUID,
        previous: SessionCredential?,
        now: Instant,
    ) {
        if (previous != null) {
            connection.executeBound(
                "UPDATE browser_session SET revoked_at=? WHERE token_hash=? AND revoked_at IS NULL",
                Timestamp.from(now),
                previous.digest(),
            )
        }
        connection.executeBound(
            "UPDATE browser_session SET revoked_at=? WHERE id IN (SELECT id FROM browser_session " +
                "WHERE account_id=? AND auth_epoch=(SELECT auth_epoch FROM user_account WHERE id=?) " +
                "AND revoked_at IS NULL AND expires_at>? AND last_seen_at>? " +
                "ORDER BY created_at DESC,id DESC OFFSET ?)",
            Timestamp.from(now),
            account,
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
