package com.spoolin.core.boundary.http

import com.spoolin.support.tracing.OwnerSpan
import com.spoolin.support.tracing.OwnerTracing
import jakarta.servlet.AsyncContext
import jakarta.servlet.AsyncEvent
import jakarta.servlet.AsyncListener
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.ServletResponse
import org.springframework.web.filter.OncePerRequestFilter

/** Browser trace headers are ignored; only this filter creates the ingress trace and async ownership. */
class BrowserTraceFilter(private val tracing: OwnerTracing) : OncePerRequestFilter() {
    override fun shouldNotFilterAsyncDispatch(): Boolean = false
    override fun shouldNotFilterErrorDispatch(): Boolean = false

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val retained = request.getAttribute(TRACE_ATTRIBUTE) as? OwnerSpan
        val span = retained ?: tracing.browser().also { request.setAttribute(TRACE_ATTRIBUTE, it) }
        val observed = TracedHttpRequest(request, response, span)
        var returned = false
        try {
            span.scoped { chain.doFilter(observed, response) }
            returned = true
        } finally {
            if (!returned || !request.isAsyncStarted) span.finish(returned && response.status < SERVER_ERROR)
        }
    }

    companion object {
        private const val TRACE_ATTRIBUTE = "com.spoolin.browser.ownerTrace"
        private const val SERVER_ERROR = 500
    }
}

private class TracedHttpRequest(
    request: HttpServletRequest,
    private val response: HttpServletResponse,
    private val span: OwnerSpan,
) : HttpServletRequestWrapper(request) {
    override fun startAsync(): jakarta.servlet.AsyncContext = startAsync(this, response)

    override fun startAsync(request: ServletRequest, response: ServletResponse): jakarta.servlet.AsyncContext =
        super.startAsync(request, response).also { context ->
            if (getAttribute(LISTENER_ATTRIBUTE) == null) {
                val listener = TracedHttpAsync(span, this.response)
                setAttribute(LISTENER_ATTRIBUTE, listener)
                context.addListener(listener)
            }
        }.let { TracedAsyncContext(it, span) }

    companion object { private const val LISTENER_ATTRIBUTE = "com.spoolin.browser.traceListener" }
}

private class TracedHttpAsync(private val span: OwnerSpan, private val response: HttpServletResponse) : AsyncListener {
    override fun onComplete(event: AsyncEvent) { span.finish(response.status < SERVER_ERROR) }
    override fun onError(event: AsyncEvent) { span.finish(false) }
    override fun onTimeout(event: AsyncEvent) { span.finish(false) }
    override fun onStartAsync(event: AsyncEvent) { event.asyncContext.addListener(this) }

    companion object { private const val SERVER_ERROR = 500 }
}

private class TracedAsyncContext(private val delegate: AsyncContext, private val span: OwnerSpan) : AsyncContext by delegate {
    override fun start(action: Runnable) { delegate.start(span.wrap(action)) }
}
