package com.spoolin.core.features.session

import com.spoolin.core.features.account.application.AuthenticationAdmission
import com.spoolin.core.features.account.application.AuthenticationDenied
import com.spoolin.core.features.account.application.RegisterAccount
import com.spoolin.core.features.account.application.RegistrationInput
import com.spoolin.core.features.account.persistence.IdentityRules
import com.spoolin.core.features.crypto.Argon2Engine
import com.spoolin.core.features.crypto.PasswordHasher
import com.spoolin.core.operations.receipts.OwnerReceipts
import com.spoolin.core.recovery.crypto.CryptoFixture
import com.spoolin.support.receipts.IntentHashes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID

class RegistrationApplicationTest {
    @Test
    fun `admission is closed by default and generic private replay never revalidates changed password or name`() {
        CryptoFixture("t006-registration-application.json").use { fixture ->
            PasswordHasher(Argon2Engine()).use { hasher ->
                val receipts = OwnerReceipts(fixture.pool, IntentHashes(mapOf("v1" to ByteArray(32) { 11 }), "v1"))
                val rules = IdentityRules()
                val submission = AnonymousCsrf(ByteArray(32) { 10 }).issue()
                val input = RegistrationInput(UUID.randomUUID(), "A Person", "A@Example.org", "exact password é 123")
                val denied = RegisterAccount(fixture.accounts, hasher, rules, receipts)
                assertThrows(AuthenticationDenied::class.java) { denied.register(input, submission, UUID.randomUUID()) }
                assertEquals(0, fixture.scalar("SELECT count(*) FROM appdata.user_account"))
                assertEquals(0, fixture.scalar("SELECT count(*) FROM appdata.operation_receipt"))
                val admitted = RegisterAccount(fixture.accounts, hasher, rules, receipts, AuthenticationAdmission { _, _, _ -> })
                val result = admitted.register(input, submission, UUID.randomUUID())
                val changed = RegistrationInput(input.operationId, "", "a@example.org", "invalid")
                assertEquals(result, admitted.register(changed, submission, UUID.randomUUID()))
                assertEquals(input.operationId, result.result?.resultId)
                assertEquals(1, fixture.scalar("SELECT count(*) FROM appdata.user_account WHERE role='VIEWER'"))
                assertEquals(0, fixture.scalar("SELECT count(*) FROM appdata.browser_session"))
                assertEquals(1, fixture.scalar("SELECT count(*) FROM appdata.operation_receipt WHERE hash_key_id='v1'"))
            }
        }
    }
}
