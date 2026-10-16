package com.spoolin.core.features.session

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.util.UUID

class IdentityHttpTest {
    private val mapper = ObjectMapper()

    @Test
    fun `trusted HTTPS identities deliver credentials only through scoped headers`() {
        IdentityHttpFixture(true).use { fixture ->
            val csrf = bootstrap(fixture)
            val missingOrigin = fixture.request(envelope("query { policy { revision } }"), originValue = null)
            assertEquals(403, missingOrigin.statusCode())
            register(fixture, csrf)
            val signedIn = fixture.request(envelope(loginQuery()), csrf)
            assertSuccess(signedIn.body(), "signIn")
            val raw = requireNotNull(fixture.cookies["__Host-spoolin_session"])
            assertFalse(signedIn.body().contains(raw))
            val issuing = signedIn.headers().allValues("Set-Cookie").single { it.startsWith("__Host-spoolin_session=") }
            assertCookie(issuing)
            val query = "query { session { authenticated role displayName csrfToken } }"
            val session = fixture.request(envelope(query))
            val details = mapper.readTree(session.body()).path("data").path("session")
            assertTrue(details.path("authenticated").asBoolean())
            assertEquals("VIEWER", details.path("role").asString())
            assertEquals("A Person", details.path("displayName").asString())
            val logout = "mutation { signOut(operationId: \"${UUID.randomUUID()}\") { operation { state } } }"
            val signedOut = fixture.request(envelope(logout), details.path("csrfToken").asString())
            assertSuccess(signedOut.body(), "signOut")
            assertTrue(
                signedOut
                    .headers()
                    .allValues("Set-Cookie")
                    .single()
                    .contains("Max-Age=0"),
            )
            assertFalse(fixture.cookies.containsKey("__Host-spoolin_session"))
            val revoked = fixture.crypto.scalar("SELECT count(*) FROM appdata.browser_session WHERE revoked_at IS NOT NULL")
            assertEquals(1, revoked)
        }
    }

    @Test
    fun `missing admission denies registration before hashing receipt or account effects`() {
        IdentityHttpFixture(false).use { fixture ->
            val csrf = bootstrap(fixture)
            val query =
                """
                mutation { register(operationId: "${UUID.randomUUID()}", input: {
                  name: "A Person", email: "a@example.org", password: "valid exact password"
                }) { operation { state } } }
                """.trimIndent()
            val denied = fixture.request(envelope(query), csrf)
            assertEquals(503, denied.statusCode())
            assertEquals("no-store", denied.headers().firstValue("Cache-Control").orElse(""))
            assertEquals(0, fixture.crypto.scalar("SELECT count(*) FROM appdata.operation_receipt"))
            assertEquals(0, fixture.crypto.scalar("SELECT count(*) FROM appdata.user_account"))
        }
    }

    private fun bootstrap(fixture: IdentityHttpFixture): String {
        val bootstrap = fixture.request(envelope("query { session { authenticated csrfToken } }"))
        assertEquals(200, bootstrap.statusCode())
        assertEquals("no-store", bootstrap.headers().firstValue("Cache-Control").orElse(""))
        val csrf =
            mapper
                .readTree(bootstrap.body())
                .path("data")
                .path("session")
                .path("csrfToken")
                .asString()
        assertTrue(csrf.isNotEmpty())
        return csrf
    }

    private fun register(
        fixture: IdentityHttpFixture,
        csrf: String,
    ) {
        val query =
            """
            mutation { register(operationId: "${UUID.randomUUID()}", input: {
              name: "A Person", email: "a@example.org", password: "valid exact password"
            }) { operation { state } } }
            """.trimIndent()
        assertSuccess(fixture.request(envelope(query), csrf).body(), "register")
        assertFalse(fixture.cookies.containsKey("__Host-spoolin_session"))
    }

    private fun loginQuery(): String =
        """
        mutation { signIn(operationId: "${UUID.randomUUID()}", email: "a@example.org",
          password: "valid exact password") { operation { state } } }
        """.trimIndent()

    private fun envelope(query: String): String = mapper.writeValueAsString(mapOf("query" to query))

    private fun assertSuccess(
        body: String,
        field: String,
    ) {
        val state =
            mapper
                .readTree(body)
                .path("data")
                .path(field)
                .path("operation")
                .path("state")
                .asString()
        assertEquals("SUCCEEDED", state)
    }

    private fun assertCookie(header: String) {
        listOf("Secure", "HttpOnly", "SameSite=Lax", "Path=/").forEach { assertTrue(header.contains("; $it")) }
        listOf("Domain=", "Expires=", "Max-Age=").forEach { assertFalse(header.contains(it)) }
        assertNotNull(SessionCredential.parse(header.substringAfter('=').substringBefore(';')))
    }
}
