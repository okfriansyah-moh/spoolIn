package com.spoolin.core.features.session

import com.spoolin.core.features.account.persistence.AccountRole
import com.spoolin.core.features.account.persistence.IdentityRules
import com.spoolin.core.features.crypto.StoredPasswordHash
import com.spoolin.core.recovery.crypto.CryptoFixture
import com.spoolin.support.receipts.committed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class SessionStoreTest {
    private fun credential(issued: IssuedSession): SessionCredential =
        requireNotNull(SessionCredential.parse(issued.cookie().substringAfter('=').substringBefore(';')))

    @Test
    fun `issued sessions rotate expire and retain only hashes with five active bound`() {
        CryptoFixture("t006-session-storage.json").use { fixture ->
            val account = UUID.randomUUID()
            fixture.accounts.insert(
                account,
                IdentityRules().identity("A Person", "a@example.org"),
                StoredPasswordHash("unusable"),
                AccountRole.VIEWER,
            )
            val now = Instant.parse("2026-10-20T00:00:00Z")
            val csrf = SessionCsrf(ByteArray(32) { 12 })
            val store = SessionStore(fixture.pool, csrf, Clock.fixed(now, ZoneOffset.UTC))
            val first = fixture.pool.committed { store.issue(it, account, 0, null) }
            assertNotNull(store.find(credential(first)))
            val rotated = fixture.pool.committed { store.issue(it, account, 0, credential(first)) }
            assertNull(store.find(credential(first)))
            assertNotNull(store.find(credential(rotated)))
            repeat(7) { fixture.pool.committed { store.issue(it, account, 0, null) } }
            assertEquals(5, fixture.scalar("SELECT count(*) FROM appdata.browser_session WHERE revoked_at IS NULL"))
            val last = fixture.pool.committed { store.issue(it, account, 0, null) }
            assertNull(SessionStore(fixture.pool, csrf, Clock.fixed(now.plusSeconds(1800), ZoneOffset.UTC)).find(credential(last)))
            assertNull(SessionStore(fixture.pool, SessionCsrf(ByteArray(32)), Clock.fixed(now, ZoneOffset.UTC)).find(credential(last)))
            fixture.pool.connection.use {
                it.createStatement().use { statement ->
                    statement.executeUpdate("UPDATE appdata.user_account SET auth_epoch=1")
                }
            }
            assertNull(store.find(credential(last)))
            assertEquals(
                0,
                fixture.scalar(
                    "SELECT count(*) FROM appdata.browser_session WHERE octet_length(token_hash)<>32 OR octet_length(csrf_hash)<>32",
                ),
            )
        }
    }
}
