package com.spoolin.core.features.session

import com.spoolin.core.features.account.application.AuthenticationAdmission
import com.spoolin.core.features.account.application.RegisterAccount
import com.spoolin.core.features.account.application.RegistrationInput
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
                val service = RegisterAccount(fixture.accounts, hasher, IdentityRules(), receipts, AuthenticationAdmission { _, _ -> })
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
                    listOf(input.name, input.email, "a@example.test", input.password, "argon2", submission.csrfToken)
                        .forEach { assertFalse(row.third.contains(it)) }
                }
                val replay = RegistrationInput(input.operationId, "", "a@example.test", "changed invalid password")
                assertEquals(result, service.register(replay, submission, UUID.randomUUID()))
                assertEquals(input.operationId, result.result?.resultId)
                assertThrows(ReceiptConflict::class.java) {
                    receipts.resolve(
                        intent.copy(
                            fields =
                                intent.fields + ("nonce" to UUID.randomUUID().toString()),
                        ),
                    )
                }
                assertEquals(0, fixture.scalar("SELECT count(*) FROM appdata.browser_session"))
                assertEquals(1, fixture.scalar("SELECT count(*) FROM appdata.operational_event WHERE action='ACCOUNT_REGISTERED'"))
            }
        }
    }
}
