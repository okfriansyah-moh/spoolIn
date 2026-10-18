package com.spoolin.core.boundary.http

import jakarta.annotation.PreDestroy
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.context.request.async.DeferredResult
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

private const val REQUEST_THREADS = 8
private const val REQUEST_QUEUE = 8
private const val IDENTITY_TIMEOUT_MS = 5000L

@Component
class BrowserRequests {
    private val executor =
        ThreadPoolExecutor(
            REQUEST_THREADS,
            REQUEST_THREADS,
            0L,
            TimeUnit.MILLISECONDS,
            ArrayBlockingQueue(REQUEST_QUEUE),
            { task -> Thread(task, "browser-request").also { it.isDaemon = true } },
            ThreadPoolExecutor.AbortPolicy(),
        )

    fun run(
        timeoutMs: Long = IDENTITY_TIMEOUT_MS,
        action: (Long) -> ResponseEntity<Map<String, Any?>>,
    ): DeferredResult<ResponseEntity<Map<String, Any?>>> {
        require(timeoutMs in 1..IDENTITY_TIMEOUT_MS)
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
        val timeout = BrowserResponses.failure(BrowserBoundary.UNAVAILABLE, "DEADLINE_EXCEEDED")
        val result = DeferredResult<ResponseEntity<Map<String, Any?>>>(timeoutMs, timeout)
        try {
            val future =
                executor.submit(
                    com.spoolin.support.tracing.TraceContexts.wrap(
                        Runnable {
                            try {
                                result.setResult(action(deadline))
                            } catch (expectedExecutionFailure: Exception) {
                                // The executor boundary must finish the deferred response even when an action throws.
                                org.slf4j.LoggerFactory
                                    .getLogger(BrowserRequests::class.java)
                                    .warn("Browser execution failed", expectedExecutionFailure)
                                result.setResult(BrowserResponses.failure(BrowserBoundary.UNAVAILABLE, "UNAVAILABLE"))
                            }
                        },
                    ),
                )
            result.onTimeout {
                result.setResult(timeout)
                future.cancel(true)
            }
        } catch (_: RejectedExecutionException) {
            result.setResult(BrowserResponses.failure(BrowserBoundary.UNAVAILABLE, "UNAVAILABLE"))
        }
        return result
    }

    @PreDestroy
    fun close() {
        executor.shutdownNow()
    }
}
