package com.spoolin.core.features.session

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.jdbc.datasource.AbstractDataSource
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

/** Database responses are supplied at the JDBC boundary; session eligibility and cryptography stay real. */
class SessionStoreBehaviorTest {
    private val now = Instant.parse("2026-10-25T10:00:00Z")
    private val account = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001")
    private val session = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001")
    private val csrf = SessionCsrf(ByteArray(32) { 12 })
    private val credential = SessionCredential.issue()

    @Test
    fun `issuance returns fresh credential twelve hour lifetime and authoritative identity`() {
        val db = SessionJdbcFixture(listOf(listOf(authority()), listOf(authority())))
        val issued = store(db).issue(db.jdbc, account, 3, null, session)
        assertEquals(session, issued.principal.id)
        assertEquals(account, issued.principal.accountId)
        assertEquals(now.plusSeconds(12 * 3600), issued.principal.expiresAt)
        assertEquals(csrf.derive(issued.credential), issued.principal.csrfToken)
        assertNotEquals(credential.toString(), issued.credential.digest().contentToString())
    }

    @Test
    fun `issuance rejects authority epoch change before persisting new session`() {
        val db = SessionJdbcFixture(listOf(listOf(authority()), listOf(authority())))
        assertThrows(IllegalStateException::class.java) { store(db).issue(db.jdbc, account, 2, null, session) }
        assertEquals(0, db.updates)
    }

    @Test
    fun `find returns authenticated principal only with eligible lifetime epoch and csrf`() {
        val db = SessionJdbcFixture(listOf(listOf(mapOf("account_id" to account)), listOf(authority()), listOf(row())))
        val principal = store(db).find(credential)!!
        assertEquals(session, principal.id)
        assertEquals(account, principal.accountId)
        assertEquals(csrf.derive(credential), principal.csrfToken)
        assertEquals(1, db.commits)
    }

    @Test
    fun `missing expired revoked epoch mismatch and corrupted csrf never authenticate`() {
        val missing = SessionJdbcFixture(listOf(emptyList()))
        assertNull(store(missing).find(credential))
        val invalid = listOf(
            row() + ("expires_at" to Timestamp.from(now)),
            row() + ("revoked_at" to Timestamp.from(now.minusSeconds(1))),
            row() + ("auth_epoch" to 2L),
            row() + ("csrf_hash" to ByteArray(32)),
            row() + ("last_seen_at" to Timestamp.from(now.minusSeconds(1801))),
        )
        for (record in invalid) {
            val db = SessionJdbcFixture(listOf(listOf(mapOf("account_id" to account)), listOf(authority()), listOf(record)))
            assertNull(store(db).find(credential))
            assertEquals(0, db.updates)
        }
    }

    @Test
    fun `logout rejects invalid csrf before database and returns matching identity`() {
        val db = SessionJdbcFixture(listOf(listOf(mapOf("id" to session, "account_id" to account))))
        assertNull(store(db).logoutIdentity(credential, "bad"))
        assertEquals(0, db.queries)
        val identity = store(db).logoutIdentity(credential, csrf.derive(credential))!!
        assertEquals(session, identity.sessionId)
        assertEquals(account, identity.accountId)
    }

    @Test
    fun `actor authority is reconstructed from active database row and absence stays absent`() {
        val db = SessionJdbcFixture(listOf(listOf(authority() + ("expires" to Timestamp.from(now.plusSeconds(30))))))
        val actor = store(db).checkActor(account, session, 3)!!
        assertEquals(account, actor.account.id)
        val absent = SessionJdbcFixture(listOf(emptyList()))
        assertNull(store(absent).checkActor(account, session, 3))
    }

    @Test
    fun `revoke and purge return database effects within committed transaction`() {
        val db = SessionJdbcFixture(emptyList())
        store(db).revoke(db.jdbc, session, account)
        assertEquals(1, store(db).purge())
        assertEquals(2, db.updates)
        assertEquals(1, db.commits)
    }

    private fun store(db: SessionJdbcFixture) = SessionStore(db, csrf, Clock.fixed(now, ZoneOffset.UTC))
    private fun authority() = mapOf("role" to "MAINTAINER", "auth_epoch" to 3L)
    private fun row(): Map<String, Any?> = mapOf(
        "id" to session, "auth_epoch" to 3L, "csrf_hash" to csrf.digest(csrf.derive(credential)),
        "created_at" to Timestamp.from(now.minusSeconds(300)),
        "last_seen_at" to Timestamp.from(now.minusSeconds(120)),
        "expires_at" to Timestamp.from(now.plusSeconds(3600)), "revoked_at" to null,
    )

}
