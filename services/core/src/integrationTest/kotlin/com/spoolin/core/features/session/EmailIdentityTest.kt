package com.spoolin.core.features.session

import com.spoolin.core.features.account.application.AuthenticationAdmission
import com.spoolin.core.features.account.application.RegisterAccount
import com.spoolin.core.features.account.application.RegistrationInput
import com.spoolin.core.features.account.persistence.IdentityRules
import com.spoolin.core.features.crypto.Argon2Engine
import com.spoolin.core.features.crypto.PasswordHasher
import com.spoolin.core.features.crypto.ProtectedRead
import com.spoolin.core.features.crypto.queryBound
import com.spoolin.core.operations.receipts.OwnerReceipts
import com.spoolin.core.recovery.crypto.CryptoFixture
import com.spoolin.support.receipts.IntentHashes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class EmailIdentityTest {
    @Test
    fun `ATDD175 application case race commits one encrypted Viewer and one audit without directory projection`() {
        CryptoFixture("t008-email-race.json").use { fixture ->
            PasswordHasher(Argon2Engine()).use { hasher ->
                raceClaims(fixture, hasher)
            }
        }
    }

    @Test
    fun `ATDD175 plus dots IDNA and encrypted display preserve distinct actual owner identities`() {
        CryptoFixture("t008-email-variants.json").use { fixture ->
            PasswordHasher(Argon2Engine()).use { hasher ->
                variantClaims(fixture, hasher)
            }
        }
    }

    private fun raceClaims(
        fixture: CryptoFixture,
        hasher: PasswordHasher,
    ) {
        val service = service(fixture, hasher)
        val submission = AnonymousCsrf(ByteArray(32) { 11 }).issue()
        val barrier = CyclicBarrier(2)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val results =
                listOf("A@example.test", "a@example.test")
                    .map { email ->
                        executor.submit<Boolean> {
                            registerAtBarrier(service, submission, email, barrier)
                        }
                    }.map { it.get(15, TimeUnit.SECONDS) }
            assertEquals(1, results.count { it })
            assertEquals(1, fixture.scalar("SELECT count(*) FROM appdata.user_account WHERE role='VIEWER'"))
            assertEquals(2, fixture.scalar("SELECT count(*) FROM appdata.account_email_lookup"))
            assertEquals(1, fixture.scalar("SELECT count(*) FROM appdata.operation_receipt"))
            assertEquals(1, fixture.scalar("SELECT count(*) FROM appdata.operational_event"))
        } finally {
            executor.shutdownNow()
        }
    }

    private fun registerAtBarrier(
        service: RegisterAccount,
        submission: AnonymousSubmission,
        email: String,
        barrier: CyclicBarrier,
    ): Boolean {
        barrier.await(10, TimeUnit.SECONDS)
        return try {
            service.register(input(email), submission, UUID.randomUUID())
            true
        } catch (failure: IllegalStateException) {
            check(failure.message == "Account cannot be created")
            false
        }
    }

    private fun variantClaims(
        fixture: CryptoFixture,
        hasher: PasswordHasher,
    ) {
        val service = service(fixture, hasher)
        val submission = AnonymousCsrf(ByteArray(32) { 11 }).issue()
        val emails = listOf("A@example.test", "a+tag@example.test", "a.b@example.test", "ab@example.test", "A@faß.de")
        val ids =
            emails.map { email ->
                service.register(input(email), submission, UUID.randomUUID())
                val normalized = IdentityRules().normalizedEmail(email)
                val credential = (fixture.accounts.credential(normalized) as ProtectedRead.Value).value
                val display = (fixture.accounts.identity(credential.id) as ProtectedRead.Value).value
                assertEquals(email, display.emailDisplay)
                credential.id
            }
        assertEquals(emails.size, ids.distinct().size)
        assertNotEquals(ids[2], ids[3])
        val count = fixture.scalar("SELECT count(*) FROM appdata.user_account")
        listOf("a\r\n@example.test", "A <a@example.test>", "é@example.test", "a..b@example.test", "a@example.test\n")
            .forEach { email ->
                assertThrows(IllegalArgumentException::class.java) { service.register(input(email), submission, UUID.randomUUID()) }
            }
        assertEquals(count, fixture.scalar("SELECT count(*) FROM appdata.user_account"))
        fixture.owner.connection("core").use { connection ->
            val stored =
                connection.queryBound("SELECT row_to_json(a)::text AS row FROM appdata.user_account a", emptyList()) {
                    it.getString("row")
                }
            stored.forEach { row -> assertEmailsAbsent(row, emails) }
        }
    }

    private fun assertEmailsAbsent(
        row: String,
        emails: List<String>,
    ) {
        emails.forEach { assertFalse(row.contains(it)) }
    }

    private fun input(email: String): RegistrationInput =
        RegistrationInput(UUID.randomUUID(), "Permitted Person", email, "exact synthetic password")

    private fun service(
        fixture: CryptoFixture,
        hasher: PasswordHasher,
    ): RegisterAccount =
        RegisterAccount(
            fixture.accounts,
            hasher,
            IdentityRules(),
            OwnerReceipts(fixture.pool, IntentHashes(mapOf("v1" to ByteArray(32) { 17 }), "v1")),
            AuthenticationAdmission { _, _, _ -> },
        )
}
