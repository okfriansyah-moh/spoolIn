package com.spoolin.media

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import java.lang.reflect.Proxy

class HttpBoundaryRulesTest {
    private fun request(
        path: String,
        secure: Boolean = false,
        port: Int = 0,
        status: Int? = null,
    ): HttpServletRequest =
        proxy(HttpServletRequest::class.java) { method, args ->
            when (method) {
                "getRequestURI" -> path
                "isSecure" -> secure
                "getLocalPort" -> port
                "getAttribute" -> if (args?.firstOrNull() == "jakarta.servlet.error.status_code") status else null
                "getDispatcherType" -> jakarta.servlet.DispatcherType.REQUEST
                else -> null
            }
        }

    private class Response {
        var status = 200
        val headers = mutableMapOf<String, String>()
        val value: HttpServletResponse =
            proxy(HttpServletResponse::class.java) { method, args ->
                when (method) {
                    "sendError" -> {
                        status = args!![0] as Int
                        null
                    }
                    "setHeader" -> {
                        headers[args!![0] as String] =
                            args[1] as String
                        null
                    }
                    else -> null
                }
            }
    }

    @Test fun `ordinary HTTP requests retain downstream flow and disable content sniffing and caching`() {
        val environment = StandardEnvironment()
        val response = Response()
        var downstream = false
        ListenerBoundary(environment).doFilter(request("/public"), response.value, FilterChain { _, _ -> downstream = true })
        assertTrue(downstream)
        assertEquals("nosniff", response.headers["X-Content-Type-Options"])
        assertEquals("no-store", response.headers["Cache-Control"])
    }

    @Test fun `management requests without verified certificate are rejected on every port and protocol`() {
        val environment =
            StandardEnvironment().apply {
                propertySources.addFirst(
                    MapPropertySource(
                        "unit",
                        mapOf("local.management.port" to 9090),
                    ),
                )
            }
        for (secure in listOf(false, true)) {
            for (port in listOf(8080, 9090)) {
                for (path in listOf("/actuator/health", "/actuator/prometheus")) {
                    val response = Response()
                    ListenerBoundary(environment).doFilter(
                        request(path, secure, port),
                        response.value,
                        FilterChain {
                            _,
                            _,
                            ->
                            error("must not enter protected management endpoint")
                        },
                    )
                    assertEquals(403, response.status)
                }
            }
        }
    }

    @Test fun `error controller preserves safe status and never exposes diagnostic payloads`() {
        for (status in listOf<Int?>(null, 400, 404, 503)) {
            val response = BoundaryErrors().error(request("/error", status = status))
            assertEquals(status ?: 500, response.statusCode.value())
            assertEquals(mapOf("error" to "REQUEST_FAILED"), response.body)
            assertEquals("no-store", response.headers.getFirst("Cache-Control"))
        }
    }

    companion object {
        private fun <T> proxy(
            type: Class<T>,
            invoke: (String, Array<out Any?>?) -> Any?,
        ): T =
            type.cast(
                Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, args ->
                    when (method.name) {
                        "toString" -> "HTTP unit boundary"
                        "hashCode" -> 1
                        "equals" -> false
                        else ->
                            invoke(method.name, args)
                                ?: when (method.returnType) {
                                    java.lang.Boolean.TYPE -> false
                                    java.lang.Integer.TYPE -> 0
                                    java.lang.Long.TYPE -> 0L
                                    else -> null
                                }
                    }
                },
            )
    }
}
