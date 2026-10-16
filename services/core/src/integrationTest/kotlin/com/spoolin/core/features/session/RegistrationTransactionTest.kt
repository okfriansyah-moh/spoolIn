package com.spoolin.core.features.session

import com.spoolin.core.features.account.persistence.AccountRole
import com.spoolin.core.features.account.persistence.IdentityRules
import com.spoolin.core.features.crypto.StoredPasswordHash
import com.spoolin.core.operations.events.ReceiptAction
import com.spoolin.core.operations.events.ReceiptAudit
import com.spoolin.core.operations.receipts.OwnerReceipts
import com.spoolin.core.recovery.crypto.CryptoFixture
import com.spoolin.support.receipts.IntentHashes
import com.spoolin.support.receipts.ReceiptConflict
import com.spoolin.support.receipts.ReceiptHidden
import com.spoolin.support.receipts.RegistrationIntent
import com.spoolin.support.receipts.SafeResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID

class RegistrationTransactionTest {
    @Test
    fun `prepared registration shares receipt transaction and rollback consumes nonce without partial account`() {
        CryptoFixture("t006-registration-transaction.json").use { fixture ->
            val receipts = OwnerReceipts(fixture.pool, IntentHashes(mapOf("v1" to ByteArray(32) { 11 }), "v1"))
            val id = UUID.randomUUID()
            val identity = IdentityRules().identity("Example Person", "A@Example.org")
            val intent =
                RegistrationIntent.create(
                    UUID.randomUUID(),
                    "anonymous:fixture",
                    "auth-v1",
                    identity.emailNormalized,
                    UUID.randomUUID(),
                )
            assertNull(receipts.resolve(intent))
            val prepared = fixture.accounts.prepare(id, identity, StoredPasswordHash("synthetic-unusable-hash"), AccountRole.VIEWER)
            assertThrows(IllegalStateException::class.java) {
                receipts.execute(intent, UUID.randomUUID(), true) { connection ->
                    fixture.accounts.insert(connection, prepared)
                    error("Synthetic failure after encrypted account and aliases")
                }
            }
            listOf("user_account", "account_email_lookup", "operation_receipt", "operational_event")
                .forEach { assertEquals(0, fixture.scalar("SELECT count(*) FROM appdata.$it")) }
            assertEquals(1, fixture.scalar("SELECT next_nonce-1 FROM appdata.crypto_key_version WHERE id='pii-write'"))
            val audit = ReceiptAudit(ReceiptAction.ACCOUNT_REGISTERED, subjectId = id)
            val completed =
                receipts.executeAudited(intent, UUID.randomUUID(), audit) { connection ->
                    fixture.accounts.insert(connection, prepared)
                    SafeResult(intent.id)
                }
            assertEquals(completed, receipts.resolve(intent))
            assertEquals(1, fixture.scalar("SELECT count(*) FROM appdata.user_account WHERE role='VIEWER'"))
            assertEquals(2, fixture.scalar("SELECT count(*) FROM appdata.account_email_lookup"))
            assertEquals(1, fixture.scalar("SELECT count(*) FROM appdata.operational_event WHERE action='ACCOUNT_REGISTERED'"))
            assertThrows(ReceiptConflict::class.java) { receipts.resolve(intent.copy(fields = listOf("identity" to "different"))) }
            assertThrows(ReceiptHidden::class.java) { receipts.resolve(intent.copy(actorScope = "anonymous:other")) }
        }
    }
}
