package com.spoolin.core.boundary.admission

import com.spoolin.core.features.session.IdentityHttpFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

class OperationRecoveryHttpTest {
    private val mapper = JsonMapper()

    @Test
    fun `anonymous receipt remains source scoped after sign in and aliases consume once`() {
        IdentityHttpFixture(false, true, mapOf("RATE_LIMIT_READ_WINDOW_SECONDS" to "86400")).use { fixture ->
            val csrf = sessionCsrf(fixture)
            val operation = UUID.randomUUID()
            assertEquals(200, fixture.request(envelope(register(operation)), csrf).statusCode())
            val signIn = """mutation { signIn(operationId:"${UUID.randomUUID()}",email:"a@example.test",
                password:"valid exact password") { operation { state } } }"""
            assertEquals(200, fixture.request(envelope(signIn), csrf).statusCode())
            assertTrue(fixture.cookies.getValue("__Host-spoolin_session").isNotEmpty())
            val buckets = fixture.crypto.scalar("SELECT count(*) FROM appdata.request_rate_window")
            val before = fixture.crypto.scalar("SELECT sum(hits) FROM appdata.request_rate_window")
            val query = """query { a:operation(id:"$operation",owner:CORE){state}
                ...Recovery } fragment Recovery on Query { b:operation(id:"$operation",owner:CORE){state} }"""
            val result = fixture.request(envelope(query))
            assertEquals(200, result.statusCode())
            val data = mapper.readTree(result.body()).path("data")
            assertEquals("SUCCEEDED", data.path("a").path("state").asString())
            assertEquals("SUCCEEDED", data.path("b").path("state").asString())
            assertEquals(buckets, fixture.crypto.scalar("SELECT count(*) FROM appdata.request_rate_window"))
            assertEquals(before + 1, fixture.crypto.scalar("SELECT sum(hits) FROM appdata.request_rate_window"))
        }
    }

    @Test
    fun `operation variable defaults and explicit null match actual GraphQL execution`() {
        IdentityHttpFixture(false, true).use { fixture ->
            val csrf = sessionCsrf(fixture)
            val operation = UUID.randomUUID()
            assertEquals(200, fixture.request(envelope(register(operation)), csrf).statusCode())
            val query = """query(${'$'}id:UUID!="$operation",${'$'}owner:OperationOwner=CORE){
                operation(id:${'$'}id,owner:${'$'}owner){state}}"""
            val omitted = fixture.request(envelope(query))
            assertEquals(
                "SUCCEEDED",
                mapper
                    .readTree(omitted.body())
                    .path("data")
                    .path("operation")
                    .path("state")
                    .asString(),
            )
            val explicitNull = mapper.writeValueAsString(mapOf("query" to query, "variables" to mapOf("owner" to null)))
            val unscoped = fixture.request(explicitNull)
            assertEquals(
                "SUCCEEDED",
                mapper
                    .readTree(unscoped.body())
                    .path("data")
                    .path("operation")
                    .path("state")
                    .asString(),
            )
            val unknown = mapper.writeValueAsString(mapOf("query" to query, "variables" to mapOf("id" to UUID.randomUUID().toString())))
            assertTrue(
                mapper
                    .readTree(fixture.request(unknown).body())
                    .path("data")
                    .path("operation")
                    .isNull,
            )
            val nullId = mapper.writeValueAsString(mapOf("query" to query, "variables" to mapOf("id" to null)))
            assertTrue(mapper.readTree(fixture.request(nullId).body()).path("errors").isArray)
        }
    }

    @Test
    fun `lost sign out is reconciled only for original session and original intent`() {
        IdentityHttpFixture(false, true).use { fixture ->
            val anonymous = sessionCsrf(fixture)
            assertEquals(200, fixture.request(envelope(register(UUID.randomUUID())), anonymous).statusCode())
            val signIn = """mutation { signIn(operationId:"${UUID.randomUUID()}",email:"a@example.test",
                password:"valid exact password") { operation { state } } }"""
            assertEquals(200, fixture.request(envelope(signIn), anonymous).statusCode())
            val csrf = sessionCsrf(fixture)
            val cookie = fixture.cookies.getValue("__Host-spoolin_session")
            val operation = UUID.randomUUID()
            val signOut = """mutation { signOut(operationId:"$operation") { operation { state } } }"""
            val signedOut = fixture.request(envelope(signOut), csrf)
            assertEquals(200, signedOut.statusCode())
            fixture.cookies["__Host-spoolin_session"] = cookie
            val original = fixture.request(envelope("""query { operation(id:"$operation",owner:CORE){state} }"""), csrf)
            assertEquals(200, original.statusCode())
            assertEquals(
                "SUCCEEDED",
                mapper
                    .readTree(original.body())
                    .path("data")
                    .path("operation")
                    .path("state")
                    .asString(),
            )
            assertTrue(
                original.headers().allValues("Set-Cookie").any { it.startsWith("__Host-spoolin_session=") && it.contains("Max-Age=0") },
            )
            assertTrue("__Host-spoolin_session" !in fixture.cookies)
            deniedOriginalQueries(fixture, operation, csrf, cookie)
            assertEquals(3, fixture.crypto.scalar("SELECT count(*) FROM appdata.operation_receipt"))
            otherSessionRetained(fixture, operation, anonymous, false)
            otherSessionRetained(fixture, operation, anonymous, true)
        }
    }

    private fun deniedOriginalQueries(
        fixture: IdentityHttpFixture,
        operation: UUID,
        csrf: String,
        cookie: String,
    ) {
        fixture.cookies["__Host-spoolin_session"] = cookie
        val unknown = fixture.request(envelope("""query { operation(id:"${UUID.randomUUID()}",owner:CORE){state} }"""), csrf)
        assertTrue(
            mapper
                .readTree(unknown.body())
                .path("data")
                .path("operation")
                .isNull,
        )
        val wrong = fixture.request(envelope("""query { operation(id:"$operation",owner:CORE){state} }"""), "wrong")
        assertTrue(
            mapper
                .readTree(wrong.body())
                .path("data")
                .path("operation")
                .isNull,
        )
        assertNoDeletion(unknown)
        assertNoDeletion(wrong)
    }

    private fun otherSessionRetained(
        fixture: IdentityHttpFixture,
        operation: UUID,
        anonymous: String,
        foreign: Boolean,
    ) {
        fixture.cookies.remove("__Host-spoolin_session")
        val email = if (foreign) "b@example.test" else "a@example.test"
        if (foreign) {
            assertEquals(
                200,
                fixture.request(envelope(register(UUID.randomUUID()).replace("a@example.test", email)), anonymous).statusCode(),
            )
        }
        val login = """mutation { signIn(operationId:"${UUID.randomUUID()}",email:"$email",
            password:"valid exact password") { operation { state } } }"""
        assertEquals(200, fixture.request(envelope(login), anonymous).statusCode())
        val cookie = fixture.cookies.getValue("__Host-spoolin_session")
        val currentCsrf = sessionCsrf(fixture)
        val response = fixture.request(envelope("""query { operation(id:"$operation",owner:CORE){state} }"""), currentCsrf)
        assertTrue(
            mapper
                .readTree(response.body())
                .path("data")
                .path("operation")
                .isNull,
        )
        assertNoDeletion(response)
        assertTrue(fixture.cookies["__Host-spoolin_session"] == cookie)
    }

    private fun assertNoDeletion(response: java.net.http.HttpResponse<String>) {
        assertTrue(response.headers().allValues("Set-Cookie").none { it.startsWith("__Host-spoolin_session=") && it.contains("Max-Age=0") })
    }

    private fun sessionCsrf(fixture: IdentityHttpFixture): String {
        val result = fixture.request(envelope("query { session { csrfToken } }"))
        assertEquals(200, result.statusCode())
        return mapper
            .readTree(result.body())
            .path("data")
            .path("session")
            .path("csrfToken")
            .asString()
    }

    private fun register(id: UUID) =
        """mutation { register(operationId:"$id",input:{
        name:"A Person",email:"a@example.test",password:"valid exact password"
    }) { operation { state } } }"""

    private fun envelope(query: String) = mapper.writeValueAsString(mapOf("query" to query))
}
