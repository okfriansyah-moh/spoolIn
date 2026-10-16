package com.spoolin.core.features.session

import com.spoolin.core.features.account.persistence.AccountRole
import com.spoolin.core.features.account.persistence.IdentityRules
import com.spoolin.core.features.crypto.StoredPasswordHash
import com.spoolin.core.recovery.crypto.CryptoFixture
import com.spoolin.support.receipts.committed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SessionConcurrencyTest {
    @Test
    fun `concurrent owner issuances serialize account authority and retain exactly five active credentials`() {
        CryptoFixture("t006-session-concurrent-issue.json").use { fixture ->
            val id = UUID.randomUUID()
            fixture.accounts.insert(
                id,
                IdentityRules().identity("A Person", "a@example.test"),
                StoredPasswordHash("unusable"),
                AccountRole.VIEWER,
            )
            val sessions = SessionStore(fixture.pool, SessionCsrf(ByteArray(32) { 11 }))
            val barrier = CyclicBarrier(10)
            val executor = Executors.newFixedThreadPool(10)
            try {
                val issued =
                    (1..10)
                        .map {
                            executor.submit<IssuedSession> {
                                barrier.await(10, TimeUnit.SECONDS)
                                fixture.pool.committed { connection -> sessions.issue(connection, id, 0, null) }
                            }
                        }.map { it.get(15, TimeUnit.SECONDS) }
                assertEquals(10, issued.map { it.cookie() }.distinct().size)
                assertEquals(5, fixture.scalar("SELECT count(*) FROM appdata.browser_session WHERE revoked_at IS NULL"))
                val verified =
                    issued.count {
                        val credential = requireNotNull(SessionCredential.parse(it.cookie().substringAfter('=').substringBefore(';')))
                        sessions.find(credential) != null
                    }
                assertEquals(5, verified)
            } finally {
                executor.shutdownNow()
            }
        }
    }
}
