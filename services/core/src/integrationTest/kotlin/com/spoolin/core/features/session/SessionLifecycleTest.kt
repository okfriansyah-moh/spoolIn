package com.spoolin.core.features.session

import com.spoolin.core.features.account.persistence.AccountRole
import com.spoolin.core.features.account.persistence.IdentityRules
import com.spoolin.core.features.crypto.StoredPasswordHash
import com.spoolin.core.recovery.crypto.CryptoFixture
import com.spoolin.support.receipts.committed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class SessionLifecycleTest {
    @Test
    fun `signing in to another account revokes previous browser credential`() {
        CryptoFixture("t006-session-account-switch.json").use { fixture ->
            val first = seed(fixture, "a@example.test")
            val second = seed(fixture, "b@example.test")
            val sessions = SessionStore(fixture.pool, SessionCsrf(ByteArray(32) { 11 }))
            val issued = fixture.pool.committed { sessions.issue(it, first, 0, null) }
            val credential = credential(issued)
            fixture.pool.committed { sessions.issue(it, second, 0, credential) }
            assertNull(sessions.find(credential))
        }
    }

    @Test
    fun `expired hashes remain seven days and purge at exact idle retention cutoff`() {
        CryptoFixture("t006-session-expiry-retention.json").use { fixture ->
            val id = seed(fixture, "a@example.test")
            val now = Instant.parse("2026-10-20T00:00:00Z")
            val csrf = SessionCsrf(ByteArray(32) { 11 })
            val sessions = SessionStore(fixture.pool, csrf, Clock.fixed(now, ZoneOffset.UTC))
            fixture.pool.committed { sessions.issue(it, id, 0, null) }
            val cutoff = now.plusSeconds(7 * 86400L + 1800)
            val before = SessionStore(fixture.pool, csrf, Clock.fixed(cutoff.minusNanos(1000), ZoneOffset.UTC))
            assertEquals(0, before.purge())
            assertEquals(1, fixture.scalar("SELECT count(*) FROM appdata.browser_session"))
            assertEquals(1, SessionStore(fixture.pool, csrf, Clock.fixed(cutoff, ZoneOffset.UTC)).purge())
            assertEquals(0, fixture.scalar("SELECT count(*) FROM appdata.browser_session"))
        }
    }

    @Test
    fun `revoked hashes purge seven days after revocation without deleting a newer eligible retention row`() {
        CryptoFixture("t006-session-revoked-retention.json").use { fixture ->
            val id = seed(fixture, "a@example.test")
            val now = Instant.parse("2026-10-20T00:00:00Z")
            val csrf = SessionCsrf(ByteArray(32) { 11 })
            val sessions = SessionStore(fixture.pool, csrf, Clock.fixed(now, ZoneOffset.UTC))
            val issued = fixture.pool.committed { sessions.issue(it, id, 0, null) }
            fixture.pool.committed { sessions.issue(it, id, 0, credential(issued)) }
            val cutoff = now.plusSeconds(7 * 86400L)
            val purge = SessionStore(fixture.pool, csrf, Clock.fixed(cutoff, ZoneOffset.UTC))
            assertEquals(1, purge.purge())
            assertEquals(1, fixture.scalar("SELECT count(*) FROM appdata.browser_session"))
        }
    }

    private fun credential(issued: IssuedSession): SessionCredential =
        requireNotNull(SessionCredential.parse(issued.cookie().substringAfter('=').substringBefore(';')))

    private fun seed(
        fixture: CryptoFixture,
        email: String,
    ): UUID {
        val id = UUID.randomUUID()
        fixture.accounts.insert(id, IdentityRules().identity("A Person", email), StoredPasswordHash("unusable"), AccountRole.VIEWER)
        return id
    }
}
