package com.spoolin.core.features.session

import com.spoolin.core.features.account.application.AuthenticationAdmission
import com.spoolin.core.features.account.application.CredentialVerifier
import com.spoolin.core.features.account.application.LoginInput
import com.spoolin.core.features.account.application.RegisterAccount
import com.spoolin.core.features.account.application.RegistrationInput
import com.spoolin.core.features.account.application.SignInAccount
import com.spoolin.core.features.account.persistence.IdentityRules
import com.spoolin.core.features.crypto.Argon2Engine
import com.spoolin.core.features.crypto.PasswordHasher
import com.spoolin.core.features.crypto.queryBound
import com.spoolin.core.operations.receipts.OwnerReceipts
import com.spoolin.core.recovery.crypto.CryptoFixture
import com.spoolin.support.receipts.IntentHashes
import com.spoolin.support.receipts.ReceiptConflict
import com.spoolin.support.receipts.RegistrationIntent
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class PrivateRegistrationTest {
    @Test
    fun `ATDD153 persisted receipt binds keyed identity and nonce without retaining private credential channels`() {
        CryptoFixture("t006-private-registration.json").use { fixture ->
            PasswordHasher(Argon2Engine()).use { hasher ->
                val key = ByteArray(32) { 17 }
                val receipts = OwnerReceipts(fixture.pool, IntentHashes(mapOf("v1" to key), "v1"))
                val service = RegisterAccount(fixture.accounts, hasher, IdentityRules(), receipts, AuthenticationAdmission { _, _, _ -> })
                val submission = AnonymousCsrf(ByteArray(32) { 11 }).issue()
                val input = RegistrationInput(UUID.randomUUID(), "Private Name Canary", "A@Example.test", "private exact password canary")
                val result = service.register(input, submission, UUID.randomUUID())
                val intent =
                    RegistrationIntent.create(
                        input.operationId,
                        submission.scope,
                        "registration-v1",
                        "a@example.test",
                        submission.nonce,
                    )
                val mac = Mac.getInstance("HmacSHA256")
                mac.init(SecretKeySpec(key, "HmacSHA256"))
                inspectRegistrationReceipt(fixture, input, mac, intent, submission)
                val replay = RegistrationInput(input.operationId, "", "a@example.test", "changed invalid password")
                assertEquals(result, service.register(replay, submission, UUID.randomUUID()))
                assertEquals(input.operationId, result.result?.resultId)
                val changedNonce =
                    RegistrationIntent.create(
                        input.operationId,
                        submission.scope,
                        "registration-v1",
                        "a@example.test",
                        UUID.randomUUID(),
                    )
                assertThrows(ReceiptConflict::class.java) { receipts.resolve(changedNonce) }
                assertEquals(0, fixture.scalar("SELECT count(*) FROM appdata.browser_session"))
                assertEquals(1, fixture.scalar("SELECT count(*) FROM appdata.operational_event WHERE action='ACCOUNT_REGISTERED'"))
                verifySignIn(fixture, hasher, receipts, submission, input)
            }
        }
    }

    private fun inspectRegistrationReceipt(
        fixture: CryptoFixture,
        input: RegistrationInput,
        mac: Mac,
        intent: com.spoolin.support.receipts.OperationIntent,
        submission: AnonymousSubmission,
    ) {
        fixture.owner.connection("core").use { connection ->
            val row =
                connection
                    .queryBound(
                        "SELECT request_hash,hash_key_id,row_to_json(r)::text AS receipt FROM appdata.operation_receipt r WHERE id=?",
                        listOf(input.operationId),
                    ) { Triple(it.getBytes("request_hash"), it.getString("hash_key_id"), it.getString("receipt")) }
                    .single()
            assertArrayEquals(mac.doFinal(intent.serialized()), row.first)
            assertEquals("v1", row.second)
            assertPrivateValuesAbsent(
                row.third,
                listOf(input.name, input.email, "a@example.test", input.password, "argon2", submission.csrfToken),
            )
        }
    }

    private fun assertPrivateValuesAbsent(
        row: String,
        privateValues: List<String>,
    ) {
        privateValues.forEach { assertFalse(row.contains(it)) }
    }

    private fun verifySignIn(
        fixture: CryptoFixture,
        hasher: PasswordHasher,
        receipts: OwnerReceipts,
        submission: AnonymousSubmission,
        registration: RegistrationInput,
    ) {
        val service =
            SignInAccount(
                CredentialVerifier(fixture.accounts, hasher, IdentityRules()),
                SessionStore(fixture.pool, SessionCsrf(ByteArray(32) { 11 })),
                receipts,
                AuthenticationAdmission { _, _, _ -> },
            )
        val result =
            service.signIn(
                LoginInput(UUID.randomUUID(), registration.email, registration.password),
                submission,
                null,
                UUID.randomUUID(),
            )
        val raw = requireNotNull(result.issued).cookie().substringAfter('=').substringBefore(';')
        fixture.owner.connection("core").use { connection ->
            val rows =
                connection.queryBound(
                    "SELECT row_to_json(r)::text AS receipt FROM appdata.operation_receipt r",
                    emptyList(),
                ) { it.getString("receipt") }
            assertEquals(2, rows.size)
            rows.forEach { row ->
                assertPrivateValuesAbsent(
                    row,
                    listOf(raw, registration.password, registration.email, registration.name, "argon2", submission.csrfToken),
                )
            }
        }
        assertEquals(2, fixture.scalar("SELECT count(*) FROM appdata.operation_receipt WHERE hash_key_id='v1'"))
    }
}
