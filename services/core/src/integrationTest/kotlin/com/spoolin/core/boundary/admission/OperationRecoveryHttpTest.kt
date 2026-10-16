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
            assertEquals(3, fixture.crypto.scalar("SELECT count(*) FROM appdata.operation_receipt"))
        }
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
