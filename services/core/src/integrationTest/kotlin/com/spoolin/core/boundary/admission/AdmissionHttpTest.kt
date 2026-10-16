package com.spoolin.core.boundary.admission

import com.spoolin.core.features.session.IdentityHttpFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

class AdmissionHttpTest {
    private val mapper = JsonMapper()

    @Test
    fun `actual production source quota denies before account hashing and receipt insertion`() {
        IdentityHttpFixture(false, true, mapOf("RATE_LIMIT_REGISTER_MAX_REQUESTS" to "1")).use { fixture ->
            val bootstrap = fixture.request(envelope("query { session { csrfToken } }"))
            assertEquals(200, bootstrap.statusCode())
            val csrf =
                mapper
                    .readTree(bootstrap.body())
                    .path("data")
                    .path("session")
                    .path("csrfToken")
                    .asString()
            val first = fixture.request(envelope(register("a@example.test")), csrf)
            assertEquals(200, first.statusCode())
            assertEquals(
                "SUCCEEDED",
                mapper
                    .readTree(first.body())
                    .path("data")
                    .path("register")
                    .path("operation")
                    .path("state")
                    .asString(),
            )
            val denied = fixture.request(envelope(register("b@example.test")), csrf)
            assertEquals(429, denied.statusCode())
            assertTrue(
                denied
                    .headers()
                    .firstValue("Retry-After")
                    .orElse("0")
                    .toLong() >= 1,
            )
            assertEquals(
                "RATE_LIMITED",
                mapper
                    .readTree(denied.body())
                    .path("errors")
                    .first()
                    .path("extensions")
                    .path("code")
                    .asString(),
            )
            assertEquals(1, fixture.crypto.scalar("SELECT count(*) FROM appdata.user_account"))
            assertEquals(1, fixture.crypto.scalar("SELECT count(*) FROM appdata.operation_receipt"))
            assertEquals(1, fixture.crypto.scalar("SELECT count(*) FROM appdata.operational_event"))
        }
    }

    @Test
    fun `policy source quota returns no partial data and safe retry timing`() {
        IdentityHttpFixture(false, true, mapOf("RATE_LIMIT_READ_MAX_REQUESTS" to "1")).use { fixture ->
            val query = envelope("query { policy { revision } }")
            assertEquals(200, fixture.request(query).statusCode())
            val denied = fixture.request(query)
            assertEquals(429, denied.statusCode())
            assertTrue(denied.headers().firstValue("Retry-After").isPresent)
            assertEquals(0, fixture.crypto.scalar("SELECT count(*) FROM appdata.operation_receipt"))
        }
    }

    private fun envelope(query: String) = mapper.writeValueAsString(mapOf("query" to query))

    private fun register(email: String) =
        """mutation { register(operationId:"${UUID.randomUUID()}",input:{
        name:"A Person",email:"$email",password:"valid exact password"
    }) { operation { state } } }"""
}
