package com.spoolin.core.boundary.http

import com.spoolin.core.features.account.application.AuthenticationRequest
import com.spoolin.core.features.artworkproxy.ArtworkReadProxy
import com.spoolin.core.features.ratelimit.RateLimited
import com.spoolin.core.features.ratelimit.RateUnavailable
import com.spoolin.core.features.ratelimit.TrustedAdmission
import io.grpc.Context
import io.grpc.Status
import io.grpc.StatusRuntimeException
import jakarta.annotation.PreDestroy
import jakarta.servlet.AsyncEvent
import jakarta.servlet.AsyncListener
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.ObjectProvider
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.nio.file.Files
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@RestController
class ArtworkServing(
    private val artwork: ArtworkReadProxy,
    private val identity: ObjectProvider<BrowserIdentity>,
    private val admission: ObjectProvider<TrustedAdmission>,
) {
    private val workers = ThreadPoolExecutor(4, 4, 0, TimeUnit.SECONDS, ArrayBlockingQueue(8))

    @PreDestroy
    fun close() { workers.shutdownNow() }

    @GetMapping("/artwork/{assetId}")
    fun get(@PathVariable assetId: String, request: HttpServletRequest, response: HttpServletResponse) {
        response.setHeader("Cache-Control", "no-store")
        response.setHeader("X-Content-Type-Options", "nosniff")
        if (request.getHeader("Range") != null) { response.sendError(416); return }
        try {
            val id = UUID.fromString(assetId).also { require(it.toString() == assetId) }
            val view = request.parameterMap
            require(view.keys.all { it == "view" } && view.values.all { it.size == 1 })
            val management = view["view"]?.single()?.also { require(it == "management") } != null
            val context = browser(request, management)
            val limiter = admission.ifAvailable ?: throw RateUnavailable()
            limiter.reads(context.request, !management, management)
            serve(id, management, context, request, response)
        } catch (failure: RateLimited) {
            response.setHeader("Retry-After", ((failure.retryAfterMs + 999) / 1000).coerceAtLeast(1).toString())
            response.sendError(429)
        } catch (_: RateUnavailable) { response.sendError(503)
        } catch (_: BrowserRejected) { response.sendError(403)
        } catch (failure: StatusRuntimeException) { response.sendError(status(failure))
        } catch (_: IllegalArgumentException) { response.sendError(404) }
    }

    private fun browser(request: HttpServletRequest, management: Boolean): BrowserContext {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15)
        if (!management) return BrowserContext(null, null, null, null, AuthenticationRequest(request.remoteAddr, null, deadline))
        val identities = identity.ifAvailable ?: throw RateUnavailable()
        val context = identities.context(request, false, false, bootstrap = false, deadline = deadline)
        if (context.principal == null) throw Status.UNAUTHENTICATED.asRuntimeException()
        if (context.principal.role.name != "MAINTAINER") throw Status.PERMISSION_DENIED.asRuntimeException()
        return context
    }

    private fun serve(id: UUID, management: Boolean, browser: BrowserContext, request: HttpServletRequest, response: HttpServletResponse) {
        val cancellation = Context.current().withCancellation()
        val async = request.startAsync()
        async.timeout = 15_000
        async.addListener(object : AsyncListener {
            override fun onError(event: AsyncEvent) { cancellation.cancel(event.throwable); runCatching { async.complete() } }
            override fun onTimeout(event: AsyncEvent) {
                cancellation.cancel(java.util.concurrent.TimeoutException())
                if (!response.isCommitted) response.status = 503
                async.complete()
            }
            override fun onComplete(event: AsyncEvent) { cancellation.cancel(null) }
            override fun onStartAsync(event: AsyncEvent) = Unit
        })
        try {
            workers.execute {
                try { cancellation.run { transfer(id, management, browser, response) } }
                catch (failure: StatusRuntimeException) { if (!response.isCommitted) response.status = if (!management && failure.status.code == Status.Code.PERMISSION_DENIED) 404 else status(failure) }
                catch (_: Exception) { if (!response.isCommitted) response.status = 503 }
                finally { cancellation.cancel(null); runCatching { async.complete() } }
            }
        } catch (_: RejectedExecutionException) {
            cancellation.cancel(null)
            response.status = 503
            async.complete()
        }
    }

    private fun transfer(id: UUID, management: Boolean, context: BrowserContext, response: HttpServletResponse) {
        artwork.read(id, management, context).use { asset ->
            if (Context.current().isCancelled) return
            response.contentType = asset.contentType
            response.setContentLengthLong(asset.bytes)
            Files.newInputStream(asset.path).use { input ->
                val buffer = ByteArray(65536)
                while (!Context.current().isCancelled) {
                    val size = input.read(buffer)
                    if (size < 0) break
                    response.outputStream.write(buffer, 0, size)
                }
            }
        }
    }

    private fun status(failure: StatusRuntimeException): Int = when (failure.status.code) {
        Status.Code.NOT_FOUND -> 404
        Status.Code.UNAUTHENTICATED -> 401
        Status.Code.PERMISSION_DENIED -> 403
        else -> 503
    }
}
