package com.spoolin.core

import jakarta.servlet.FilterChain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class ListenerBoundaryBehaviorTest {
    @Test
    fun `public routes continue with cache and content type protections`() {
        val request = MockHttpServletRequest("POST", "/graphql")
        val response = MockHttpServletResponse()
        var dispatched = 0
        ListenerBoundary(environment()).doFilter(request, response, FilterChain { _, _ -> dispatched++ })
        assertEquals(1, dispatched)
        assertEquals("no-store", response.getHeader("Cache-Control"))
        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"))
    }

    @Test
    fun `private monitoring and alert paths reject anonymous access even on secure management port`() {
        for (path in listOf("/actuator/health", "/actuator/prometheus", "/internal/alerts")) {
            for (secure in listOf(false, true)) {
                val request =
                    MockHttpServletRequest("POST", path).also {
                        it.isSecure = secure
                        it.localPort = 9090
                    }
                val response = MockHttpServletResponse()
                ListenerBoundary(environment()).doFilter(
                    request,
                    response,
                    FilterChain {
                        _,
                        _,
                        ->
                        fail<Unit>("Private endpoint was dispatched")
                    },
                )
                assertEquals(403, response.status)
            }
        }
        val request = MockHttpServletRequest("POST", "/internal/alerts").also { it.localPort = 8080 }
        val response = MockHttpServletResponse()
        ListenerBoundary(environment()).doFilter(request, response, FilterChain { _, _ -> fail<Unit>("Alert endpoint was dispatched") })
        assertEquals(404, response.status)
    }

    @Test
    fun `service identity allowlist isolates catalog identity checks from MCP gateway methods`() {
        val interceptor = ServiceIdentityInterceptor()
        val identities = listOf("core", "catalog", "media", "mcp", "operator", "prometheus", "alertmanager", "unknown", null)
        for (identity in identities) {
            assertEquals(identity == "catalog", interceptor.allowed("spoolin.v1.IdentityService/CheckActor", identity))
            for (method in listOf(
                "GetPolicy",
                "GetMovieContext",
                "ProposeMovie",
                "PrepareMoviePoster",
                "ReadMoviePoster",
                "ListMovieProposals",
                "GetReviewIdentity",
                "ApproveMovie",
                "RejectMovie",
                "GetMovieOperation",
            )) {
                assertEquals(identity == "mcp", interceptor.allowed("spoolin.v1.McpGatewayService/$method", identity))
            }
            assertFalse(interceptor.allowed("spoolin.v1.McpGatewayService/Unknown", identity))
            assertFalse(interceptor.allowed("spoolin.v1.IdentityService/CheckActor/extra", identity))
            assertEquals(
                identity in listOf("core", "catalog", "media", "mcp", "operator"),
                interceptor.allowed("grpc.health.v1.Health/Check", identity),
            )
        }
    }

    @Test
    fun `boundary errors use generic safe response with supplied status or internal failure default`() {
        val request = MockHttpServletRequest().also { it.setAttribute("jakarta.servlet.error.status_code", 403) }
        val response = BoundaryErrors().error(request)
        assertEquals(403, response.statusCode.value())
        assertEquals(mapOf("error" to "REQUEST_FAILED"), response.body)
        assertEquals("no-store", response.headers.getFirst("Cache-Control"))
        assertEquals(500, BoundaryErrors().error(MockHttpServletRequest()).statusCode.value())
        for (id in listOf("invalid", "AAAAAAAA-0000-0000-0000-000000000001", "../manage")) {
            assertEquals(404, FrontendNavigation().recordNavigation(id).statusCode.value())
        }
    }

    private fun environment() =
        StandardEnvironment().also {
            it.propertySources.addFirst(MapPropertySource("fixture", mapOf("local.management.port" to 9090)))
        }
}
