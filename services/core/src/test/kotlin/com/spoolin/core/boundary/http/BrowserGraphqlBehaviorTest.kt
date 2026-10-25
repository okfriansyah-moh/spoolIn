package com.spoolin.core.boundary.http

import com.spoolin.core.boundary.graphql.PolicyGraphql
import com.spoolin.core.features.policy.PolicyProvider
import com.spoolin.support.policy.PolicyEnvironment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertResponse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.support.StaticListableBeanFactory
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockHttpServletRequest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class BrowserGraphqlBehaviorTest {
    private val policy = PolicyProvider(PolicyEnvironment.resolve { null })

    @Test
    fun `browser request boundary enforces origin security headers method and content type`() =
        fixture { controller ->
            for (mutate in listOf<(MockHttpServletRequest) -> Unit>(
                { it.isSecure = false },
                { it.removeHeader("Origin") },
                { it.addHeader("Host", "attacker.test") },
                { it.removeHeader("X-Spoolin-Client") },
            )) {
                val request = request("{\"query\":\"{ policy { revision } }\"}").also(mutate)
                assertResponse(controller, request, 403, "FORBIDDEN")
            }
            for (mutate in listOf<(MockHttpServletRequest) -> Unit>(
                { it.method = "GET" },
                { it.queryString = "query=x" },
                { it.contentType = "text/plain" },
            )) {
                assertResponse(controller, request("{}").also(mutate), 400, "FIELD_INVALID")
            }
        }

    @Test
    fun `malformed duplicate nonobject and invalid GraphQL bodies return safe client failures`() =
        fixture { controller ->
            for (body in listOf(
                "{",
                "[]",
                "null",
                "{\"query\":1}",
                "{\"query\":\"{\"}",
                "{\"query\":\"{policy{revision}}\",\"query\":\"{policy{revision}}\"}",
            )) {
                assertResponse(controller, request(body), 400, "FIELD_INVALID")
            }
            val oversized = "x".repeat(policy.policy.requestLimits.getValue("uploadEnvelopeBytes") + 1)
            assertResponse(controller, request(oversized), 413, "RESOURCE_EXHAUSTED")
        }

    @Test
    fun `valid query fails safely when required identity service is unavailable`() =
        fixture { controller ->
            assertResponse(controller, request("{\"query\":\"{ policy { revision } }\"}"), 503, "UNAVAILABLE")
        }

    private fun assertResponse(
        controller: BrowserGraphql,
        request: MockHttpServletRequest,
        status: Int,
        code: String,
    ) {
        val complete = CountDownLatch(1)
        var response: ResponseEntity<Map<String, Any?>>? = null
        controller.execute(request).setResultHandler { value ->
            @Suppress("UNCHECKED_CAST")
            response = value as ResponseEntity<Map<String, Any?>>
            complete.countDown()
        }
        assertTrue(complete.await(5, TimeUnit.SECONDS))
        assertEquals(status, response!!.statusCode.value())
        val error = (response!!.body!!["errors"] as List<*>).single() as Map<*, *>
        assertEquals(code, (error["extensions"] as Map<*, *>)["code"])
        assertNull(response!!.body!!["data"])
    }

    private fun request(body: String) =
        MockHttpServletRequest("POST", "/graphql").also {
            it.isSecure = true
            it.contentType = "application/json"
            it.addHeader("Host", "localhost:8443")
            it.addHeader("Origin", "https://localhost:8443")
            it.addHeader("X-Spoolin-Client", "web")
            it.setContent(body.toByteArray())
        }

    private fun fixture(test: (BrowserGraphql) -> Unit) {
        val beans = StaticListableBeanFactory()
        val requests = BrowserRequests()
        try {
            val services =
                BrowserServices(
                    beans.getBeanProvider(BrowserIdentity::class.java),
                    beans.getBeanProvider(BrowserAdmission::class.java),
                    beans.getBeanProvider(com.spoolin.support.observability.LogicalMetrics::class.java),
                )
            val engine =
                PolicyGraphql(
                    "type Query { policy: Policy! operation(id: ID!, owner: String): String } type Policy { revision: String! }",
                    policy,
                )
            test(BrowserGraphql(policy, engine, BrowserBoundary("https://localhost:8443"), requests, services))
        } finally {
            requests.close()
        }
    }
}
