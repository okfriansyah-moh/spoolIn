package com.spoolin.core.boundary.http

import com.spoolin.support.tracing.OwnerTracing
import com.spoolin.support.tracing.TraceContexts
import jakarta.servlet.AsyncContext
import jakarta.servlet.AsyncEvent
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class BrowserTraceRulesTest {
    @Test
    fun `browser cannot choose trace through headers or ambient context and reused request thread is clear`() {
        OwnerTracing("core").use { tracing ->
            val filter = BrowserTraceFilter(tracing)
            val foreign = tracing.browser()
            var observed: Pair<String, String>? = null
            val request = MockHttpServletRequest("POST", "/graphql")
            request.addHeader("traceparent", "00-$FOREIGN_TRACE-2222222222222222-01")
            request.addHeader("baggage", "secret=private-value")
            val inherited = foreign.scoped {
                val id = TraceContexts.identifiers().first
                filter.doFilter(request, MockHttpServletResponse(), FilterChain { _, _ ->
                    observed = TraceContexts.identifiers()
                    assertNotEquals(id, observed?.first)
                })
                assertEquals(id, TraceContexts.identifiers().first)
                id
            }
            foreign.finish(true)
            assertNotEquals(FOREIGN_TRACE, observed?.first)
            assertNotEquals(inherited, observed?.first)
            assertNotEquals(ZERO_TRACE, observed?.first)
            assertEquals(ZERO_TRACE, TraceContexts.identifiers().first)
        }
    }

    @Test
    fun `async request captures trusted span and clears worker after completion and timeout`() {
        OwnerTracing("core").use { tracing ->
            val filter = BrowserTraceFilter(tracing)
            val request = MockHttpServletRequest("GET", "/artwork/test")
            request.isAsyncSupported = true
            val response = MockHttpServletResponse()
            var async: AsyncContext? = null
            var trace = ZERO_TRACE
            var captured: Runnable? = null
            filter.doFilter(request, response, FilterChain { wrapped, _ ->
                trace = TraceContexts.identifiers().first
                async = (wrapped as HttpServletRequest).startAsync()
                captured = TraceContexts.wrap(Runnable { assertEquals(trace, TraceContexts.identifiers().first) })
            })
            assertNotEquals(ZERO_TRACE, trace)
            assertEquals(ZERO_TRACE, TraceContexts.identifiers().first)
            val executor = Executors.newSingleThreadExecutor()
            try {
                executor.submit(requireNotNull(captured)).get(2, TimeUnit.SECONDS)
                assertEquals(ZERO_TRACE, executor.submit<String> { TraceContexts.identifiers().first }.get())
            } finally { executor.shutdownNow() }
            val mock = request.asyncContext as org.springframework.mock.web.MockAsyncContext
            mock.listeners.forEach { it.onTimeout(AsyncEvent(requireNotNull(async))) }
            mock.listeners.forEach { it.onComplete(AsyncEvent(requireNotNull(async))) }
            assertEquals(ZERO_TRACE, TraceContexts.identifiers().first)
        }
    }

    @Test
    fun `exceptional request closes scope and next request gets an independent trace`() {
        OwnerTracing("core").use { tracing ->
            val filter = BrowserTraceFilter(tracing)
            var failedTrace = ZERO_TRACE
            assertThrows(IllegalStateException::class.java) {
                filter.doFilter(MockHttpServletRequest("GET", "/failed"), MockHttpServletResponse(), FilterChain { _, _ ->
                    failedTrace = TraceContexts.identifiers().first
                    error("private-failure")
                })
            }
            assertEquals(ZERO_TRACE, TraceContexts.identifiers().first)
            filter.doFilter(MockHttpServletRequest("GET", "/next"), MockHttpServletResponse(), FilterChain { _, _ ->
                assertTrue(TraceContexts.identifiers().first !in setOf(ZERO_TRACE, failedTrace))
            })
            assertEquals(ZERO_TRACE, TraceContexts.identifiers().first)
        }
    }

    companion object {
        private const val FOREIGN_TRACE = "11111111111111111111111111111111"
        private const val ZERO_TRACE = "00000000000000000000000000000000"
    }
}
