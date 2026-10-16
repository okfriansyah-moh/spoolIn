package com.spoolin.core.features.session

import com.spoolin.core.features.account.persistence.AccountInsert
import com.spoolin.core.features.account.persistence.AccountRole
import com.spoolin.core.features.account.persistence.IdentityRules
import com.spoolin.core.features.crypto.Argon2Engine
import com.spoolin.core.features.crypto.PasswordHasher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.util.UUID

class EmailPolicyHttpTest {
    private val mapper = ObjectMapper()

    @Test
    fun `effective email maximum rejects direct registration without owner effects`() {
        IdentityHttpFixture(true, rateOverrides = mapOf("EMAIL_MAX_CODE_POINTS" to "20")).use { fixture ->
            val csrf = bootstrap(fixture)
            val counts = ownerCounts(fixture)
            val rejected = fixture.request(envelope(registration("abcdefghij@sample.org")), csrf)
            assertTrue(mapper.readTree(rejected.body()).path("errors").isArray)
            assertEquals(counts, ownerCounts(fixture))
            val accepted = fixture.request(envelope(registration("abcdefghi@sample.org")), csrf)
            assertSuccess(accepted.body(), "register")
            assertEquals(counts.zip(listOf(1, 2, 1, 1)) { before, added -> before + added }, ownerCounts(fixture))
        }
    }

    @Test
    fun `existing longer email can sign in after registration policy narrows`() {
        IdentityHttpFixture(true, rateOverrides = mapOf("EMAIL_MAX_CODE_POINTS" to "20")).use { fixture ->
            val email = "historical-account@sample.org"
            PasswordHasher(Argon2Engine()).use { hasher ->
                val stored =
                    fixture.crypto.accounts.insert(
                        UUID.randomUUID(),
                        IdentityRules().identity("Existing Person", email),
                        hasher.hashNew("synthetic exact password"),
                        AccountRole.VIEWER,
                    )
                assertTrue(stored is AccountInsert.Stored)
            }
            val csrf = bootstrap(fixture)
            val query = """mutation { signIn(operationId: "${UUID.randomUUID()}", email: "$email",
                password: "synthetic exact password") { operation { state } } }"""
            assertSuccess(fixture.request(envelope(query), csrf).body(), "signIn")
            assertTrue(fixture.cookies.containsKey("__Host-spoolin_session"))
        }
    }

    private fun ownerCounts(fixture: IdentityHttpFixture): List<Long> =
        listOf("user_account", "account_email_lookup", "operation_receipt", "operational_event").map {
            fixture.crypto.scalar("SELECT count(*) FROM appdata.$it")
        }

    private fun bootstrap(fixture: IdentityHttpFixture): String =
        mapper
            .readTree(fixture.request(envelope("query { session { csrfToken } }")).body())
            .path("data")
            .path("session")
            .path("csrfToken")
            .asString()

    private fun registration(email: String): String =
        """mutation { register(operationId: "${UUID.randomUUID()}", input: {
            name: "A Person", email: "$email", password: "synthetic exact password"
        }) { operation { state } } }"""

    private fun envelope(query: String): String = mapper.writeValueAsString(mapOf("query" to query))

    private fun assertSuccess(
        body: String,
        field: String,
    ) {
        assertEquals(
            "SUCCEEDED",
            mapper
                .readTree(body)
                .path("data")
                .path(field)
                .path("operation")
                .path("state")
                .asString(),
        )
    }
}
