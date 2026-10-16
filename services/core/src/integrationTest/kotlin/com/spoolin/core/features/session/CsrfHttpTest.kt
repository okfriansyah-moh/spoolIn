package com.spoolin.core.features.session

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.util.UUID

class CsrfHttpTest {
    private val mapper = ObjectMapper()

    @Test
    fun `actual anonymous bootstrap permits only one matching CSRF header before mutation dispatch`() {
        IdentityHttpFixture(true).use { fixture ->
            val bootstrap = fixture.request(mapper.writeValueAsString(mapOf("query" to "query { session { csrfToken } }")))
            val csrf =
                mapper
                    .readTree(bootstrap.body())
                    .path("data")
                    .path("session")
                    .path("csrfToken")
                    .asString()
            val query =
                """
                mutation { register(operationId: "${UUID.randomUUID()}", input: {
                  name: "A Person", email: "a@example.test", password: "valid exact password"
                }) { operation { state } } }
                """.trimIndent()
            val body = mapper.writeValueAsString(mapOf("query" to query))
            assertEquals(403, fixture.request(body).statusCode())
            assertEquals(403, fixture.request(body, "incorrect").statusCode())
            val headers =
                mapOf(
                    "Origin" to fixture.origin,
                    "X-Spoolin-Client" to "web",
                    "Content-Type" to "application/json",
                    "X-CSRF-Token" to csrf,
                    "Cookie" to fixture.cookies.entries.joinToString("; ") { "${it.key}=${it.value}" },
                )
            assertEquals(403, fixture.raw(body, headers, repeatCsrf = csrf).statusCode())
            val wrongName = headers - "X-CSRF-Token" + ("X-CSRF" to csrf)
            assertEquals(403, fixture.raw(body, wrongName).statusCode())
            assertEquals(0, fixture.crypto.scalar("SELECT count(*) FROM appdata.user_account"))
            assertEquals(0, fixture.crypto.scalar("SELECT count(*) FROM appdata.operation_receipt"))
            val accepted = fixture.request(body, csrf)
            assertEquals(200, accepted.statusCode())
            assertEquals(1, fixture.crypto.scalar("SELECT count(*) FROM appdata.user_account"))
        }
    }
}
