package com.spoolin.core.features.session

import com.spoolin.core.features.account.application.AuthenticationAdmission
import com.spoolin.core.features.account.application.CredentialRejected
import com.spoolin.core.features.account.application.CredentialVerifier
import com.spoolin.core.features.account.application.LoginInput
import com.spoolin.core.features.account.application.SignInAccount
import com.spoolin.core.features.account.persistence.AccountRole
import com.spoolin.core.features.account.persistence.IdentityRules
import com.spoolin.core.features.crypto.Argon2Engine
import com.spoolin.core.features.crypto.PasswordHasher
import com.spoolin.core.operations.receipts.OwnerReceipts
import com.spoolin.core.recovery.crypto.CryptoFixture
import com.spoolin.support.receipts.IntentHashes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID

class SignInApplicationTest {
    @Test
    fun `lost sign in response rechecks credential issues fresh opaque session and never stores raw cookie`() {
        CryptoFixture("t006-signin-application.json").use { fixture ->
            PasswordHasher(Argon2Engine()).use { hasher ->
                val rules = IdentityRules()
                val accountId = UUID.randomUUID()
                fixture.accounts.insert(
                    accountId,
                    rules.identity("A Person", "a@example.org"),
                    hasher.hashNew("valid old password"),
                    AccountRole.VIEWER,
                )
                val receipts = OwnerReceipts(fixture.pool, IntentHashes(mapOf("v1" to ByteArray(32) { 11 }), "v1"))
                val store = SessionStore(fixture.pool, SessionCsrf(ByteArray(32) { 12 }))
                val service =
                    SignInAccount(
                        CredentialVerifier(fixture.accounts, hasher, rules),
                        store,
                        receipts,
                        AuthenticationAdmission {
                            _,
                            _,
                            ->
                        },
                    )
                val submission = AnonymousCsrf(ByteArray(32) { 10 }).issue()
                val input = LoginInput(UUID.randomUUID(), "A@Example.org", "valid old password")
                val first = service.signIn(input, submission, null, UUID.randomUUID())
                val replay = service.signIn(input, submission, null, UUID.randomUUID())
                assertNull(replay.issued)
                assertEquals(first.outcome, replay.outcome)
                val retry = LoginInput(UUID.randomUUID(), input.email, input.password)
                val second = service.signIn(retry, submission, null, UUID.randomUUID())
                assertNotEquals(requireNotNull(first.issued).cookie(), requireNotNull(second.issued).cookie())
                assertNotEquals(first.outcome.id, second.outcome.id)
                assertEquals(2, fixture.scalar("SELECT count(*) FROM appdata.browser_session WHERE revoked_at IS NULL"))
                assertEquals(2, fixture.scalar("SELECT count(*) FROM appdata.operational_event WHERE action='SESSION_SIGNED_IN'"))
                assertEquals(2, fixture.scalar("SELECT count(*) FROM appdata.operation_receipt"))
                val wrong = LoginInput(input.operationId, input.email, "wrong password")
                assertThrows(CredentialRejected::class.java) { service.signIn(wrong, submission, null, UUID.randomUUID()) }
                assertEquals(2, fixture.scalar("SELECT count(*) FROM appdata.browser_session"))
            }
        }
    }
}
