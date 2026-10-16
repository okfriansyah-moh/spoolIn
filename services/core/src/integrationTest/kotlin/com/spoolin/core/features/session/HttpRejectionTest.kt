package com.spoolin.core.features.session

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import javax.net.ssl.SSLHandshakeException

class HttpRejectionTest {
    @Test
    fun `actual trusted HTTPS rejects transport and envelope faults before account dispatch with no store`() {
        IdentityHttpFixture(true).use { fixture ->
            val query = """{"query":"query { session { authenticated csrfToken } }"}"""
            val headers = mapOf("Origin" to fixture.origin, "X-Spoolin-Client" to "web", "Content-Type" to "application/json")
            val cases =
                listOf(
                    headers - "Origin",
                    headers + ("Origin" to "https://foreign.example"),
                    headers - "X-Spoolin-Client",
                    headers + ("Content-Type" to "text/plain"),
                )
            cases.forEachIndexed { index, values ->
                val response = fixture.raw(query, values)
                assertEquals(if (index == 3) 400 else 403, response.statusCode())
                assertEquals("no-store", response.headers().firstValue("Cache-Control").orElse(""))
            }
            assertEquals(400, fixture.raw(query, headers, "GET").statusCode())
            val wrongHost = fixture.wrongHost()
            assertEquals(403, wrongHost.first)
            assertEquals("no-store", wrongHost.second["cache-control"])
            listOf("null", "[]", "{}", "{broken", """{"query":"x","query":"y"}""").forEach { body ->
                assertEquals(400, fixture.raw(body, headers).statusCode())
            }
            assertEquals(413, fixture.raw("""{"query":"${"x".repeat(16385)}"}""", headers).statusCode())
            val deep = """{"query":"query { policy { revision } }","variables":{"x":${"[".repeat(40)}0${"]".repeat(40)}}} """
            assertEquals(400, fixture.raw(deep, headers).statusCode())
            assertEquals(0, fixture.crypto.scalar("SELECT count(*) FROM appdata.user_account"))
            assertEquals(0, fixture.crypto.scalar("SELECT count(*) FROM appdata.operation_receipt"))
        }
    }

    @Test
    fun `actual HTTPS client without fixture CA cannot establish a connection`() {
        IdentityHttpFixture(false).use { fixture ->
            val request = HttpRequest.newBuilder(URI("${fixture.origin}/graphql")).GET().build()
            assertThrows(
                SSLHandshakeException::class.java,
            ) { HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()) }
        }
    }
}
