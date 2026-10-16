package com.spoolin.core.features.session

import com.spoolin.core.features.account.application.AuthenticationAdmission
import com.spoolin.core.features.account.application.CredentialRejected
import com.spoolin.core.features.account.application.SignOutAccount
import com.spoolin.core.features.account.persistence.AccountRole
import com.spoolin.core.features.account.persistence.IdentityRules
import com.spoolin.core.features.crypto.StoredPasswordHash
import com.spoolin.core.operations.receipts.OwnerReceipts
import com.spoolin.core.recovery.crypto.CryptoFixture
import com.spoolin.support.receipts.IntentHashes
import com.spoolin.support.receipts.committed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID

class SignOutApplicationTest {
    @Test
    fun `lost logout reconciles only original session intent and never restores protected authority`() {
        CryptoFixture("t006-signout-application.json").use { fixture ->
            val id = UUID.randomUUID()
            fixture.accounts.insert(
                id,
                IdentityRules().identity("A Person", "a@example.org"),
                StoredPasswordHash("unusable"),
                AccountRole.VIEWER,
            )
            val sessions = SessionStore(fixture.pool, SessionCsrf(ByteArray(32) { 12 }))
            val issued = fixture.pool.committed { sessions.issue(it, id, 0, null) }
            val credential = requireNotNull(SessionCredential.parse(issued.cookie().substringAfter('=').substringBefore(';')))
            val receipts = OwnerReceipts(fixture.pool, IntentHashes(mapOf("v1" to ByteArray(32) { 11 }), "v1"))
            val service = SignOutAccount(sessions, receipts, AuthenticationAdmission { _, _, _ -> })
            val operation = UUID.randomUUID()
            assertThrows(CredentialRejected::class.java) { service.signOut(operation, credential, "wrong", UUID.randomUUID()) }
            val result = service.signOut(operation, credential, issued.principal.csrfToken, UUID.randomUUID())
            assertNull(sessions.find(credential))
            assertEquals(result, service.signOut(operation, credential, issued.principal.csrfToken, UUID.randomUUID()))
            assertThrows(CredentialRejected::class.java) {
                service.signOut(UUID.randomUUID(), credential, issued.principal.csrfToken, UUID.randomUUID())
            }
            assertEquals(1, fixture.scalar("SELECT count(*) FROM appdata.operational_event WHERE action='SESSION_SIGNED_OUT'"))
            assertEquals(1, fixture.scalar("SELECT count(*) FROM appdata.browser_session WHERE revoked_at IS NOT NULL"))
        }
    }
}
