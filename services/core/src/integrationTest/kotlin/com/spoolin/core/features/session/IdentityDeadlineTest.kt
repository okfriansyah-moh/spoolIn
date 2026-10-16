package com.spoolin.core.features.session

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.net.http.HttpResponse
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class IdentityDeadlineTest {
    @Test
    fun `real HTTPS enforces five second identity budget and finite dispatch capacity without late effects`() {
        IdentityHttpFixture(true).use { fixture ->
            val (headers, body) = requestInput(fixture)
            verifyDeadline(fixture, headers, body)
        }
    }

    private fun requestInput(fixture: IdentityHttpFixture): Pair<Map<String, String>, String> {
        val mapper = ObjectMapper()
        val query = mapper.writeValueAsString(mapOf("query" to "query { session { csrfToken } }"))
        val bootstrap = fixture.request(query)
        val csrf =
            mapper
                .readTree(bootstrap.body())
                .path("data")
                .path("session")
                .path("csrfToken")
                .asString()
        val headers =
            mapOf(
                "Origin" to fixture.origin,
                "X-Spoolin-Client" to "web",
                "Content-Type" to "application/json",
                "X-CSRF-Token" to csrf,
                "Cookie" to fixture.cookies.entries.joinToString("; ") { "${it.key}=${it.value}" },
                "X-Forwarded-For" to "203.0.113.99",
            )
        val mutation =
            """
            mutation { register(operationId: "${UUID.randomUUID()}", input: {
              name: "A Person", email: "a@example.test", password: "valid exact password"
            }) { operation { state } } }
            """.trimIndent()
        val body = mapper.writeValueAsString(mapOf("query" to mutation))
        return headers to body
    }

    private fun verifyDeadline(
        fixture: IdentityHttpFixture,
        headers: Map<String, String>,
        body: String,
    ) {
        val admission = fixture.context.getBean(AdmissionFixture::class.java)
        admission.observed.clear()
        admission.block = true
        val executor = Executors.newFixedThreadPool(17)
        try {
            val barrier = CyclicBarrier(18)
            val completion = ExecutorCompletionService<HttpResponse<String>>(executor)
            repeat(17) {
                completion.submit {
                    barrier.await(5, TimeUnit.SECONDS)
                    fixture.raw(body, headers, timeout = Duration.ofSeconds(9))
                }
            }
            barrier.await(5, TimeUnit.SECONDS)
            assertTrue(admission.entered.await(4, TimeUnit.SECONDS))
            val rejected = requireNotNull(completion.poll(4, TimeUnit.SECONDS)).get()
            assertFailure(rejected, "UNAVAILABLE")
            repeat(16) { assertFailure(requireNotNull(completion.poll(8, TimeUnit.SECONDS)).get(), "DEADLINE_EXCEEDED") }
            assertTrue(admission.observed.all { it.sourceAddress == "127.0.0.1" && it.credentialId == null })
            assertEquals(0, fixture.crypto.scalar("SELECT count(*) FROM appdata.user_account"))
            assertEquals(0, fixture.crypto.scalar("SELECT count(*) FROM appdata.operation_receipt"))
        } finally {
            admission.release.countDown()
            executor.shutdownNow()
        }
    }

    private fun assertFailure(
        response: HttpResponse<String>,
        code: String,
    ) {
        assertEquals(503, response.statusCode())
        val result =
            ObjectMapper()
                .readTree(response.body())
                .path("errors")
                .get(0)
                .path("extensions")
                .path("code")
                .asString()
        assertEquals(code, result)
        assertEquals("no-store", response.headers().firstValue("Cache-Control").orElse(""))
    }
}
