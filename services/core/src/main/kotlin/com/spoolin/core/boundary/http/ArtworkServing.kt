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
import jakarta.servlet.AsyncContext
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

private const val ARTWORK_WORKERS = 4
private const val ARTWORK_QUEUE_SIZE = 8
private const val ARTWORK_SECONDS = 15L
private const val ARTWORK_TIMEOUT_MS = 15_000L
private const val TRANSFER_BYTES = 65_536
private const val HTTP_RATE_LIMITED = 429
private const val SECOND_MS = 1000L

@RestController
class ArtworkServing(
    private val artwork: ArtworkReadProxy,
    private val identity: ObjectProvider<BrowserIdentity>,
    private val admission: ObjectProvider<TrustedAdmission>,
) {
    private val workers =
        ThreadPoolExecutor(
            ARTWORK_WORKERS,
            ARTWORK_WORKERS,
            0,
            TimeUnit.SECONDS,
            ArrayBlockingQueue(ARTWORK_QUEUE_SIZE),
        )

    @PreDestroy
    fun close() {
        workers.shutdownNow()
    }

    @GetMapping("/artwork/{assetId}")
    fun get(
        @PathVariable assetId: String,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        response.setHeader("Cache-Control", "no-store")
        response.setHeader("X-Content-Type-Options", "nosniff")
        if (request.getHeader("Range") != null) {
            response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE)
            return
        }
        try {
            val id = UUID.fromString(assetId).also { require(it.toString() == assetId) }
            val management = management(request)
            val context = browser(request, management)
            admit(context, management)
            serve(id, management, context, request, response)
        } catch (failure: RateLimited) {
            retryAfter(response, failure)
        } catch (_: RateUnavailable) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
        } catch (_: BrowserRejected) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN)
        } catch (failure: StatusRuntimeException) {
            response.sendError(artworkStatus(failure, false))
        } catch (_: IllegalArgumentException) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
        }
    }

    private fun admit(
        context: BrowserContext,
        management: Boolean,
    ) {
        val limiter = admission.ifAvailable ?: throw RateUnavailable()
        limiter.reads(context.request, !management, management)
    }

    private fun browser(
        request: HttpServletRequest,
        management: Boolean,
    ): BrowserContext {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(ARTWORK_SECONDS)
        if (!management) {
            return BrowserContext(null, null, null, null, AuthenticationRequest(request.remoteAddr, null, deadline))
        }
        val identities = identity.ifAvailable ?: throw RateUnavailable()
        val context = identities.context(request, false, false, bootstrap = false, deadline = deadline)
        requireMaintainer(context)
        return context
    }

    private fun requireMaintainer(context: BrowserContext) {
        if (context.principal == null) throw Status.UNAUTHENTICATED.asRuntimeException()
        if (context.principal.role.name != "MAINTAINER") throw Status.PERMISSION_DENIED.asRuntimeException()
    }

    private fun serve(
        id: UUID,
        management: Boolean,
        browser: BrowserContext,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        val cancellation = Context.current().withCancellation()
        val async = ArtworkHttpAsync(request.startAsync(), cancellation, response)
        try {
            workers.execute(
                com.spoolin.support.tracing.TraceContexts.wrap(
                    Runnable {
                        runTransfer(id, management, browser, response, async)
                    },
                ),
            )
        } catch (_: RejectedExecutionException) {
            async.unavailable()
            async.close()
        }
    }

    private fun runTransfer(
        id: UUID,
        management: Boolean,
        browser: BrowserContext,
        response: HttpServletResponse,
        async: ArtworkHttpAsync,
    ) {
        var handled = false
        try {
            async.cancellation.run { transfer(id, management, browser, response) }
            handled = true
        } catch (failure: StatusRuntimeException) {
            if (!response.isCommitted) response.status = artworkStatus(failure, !management)
            handled = true
        } finally {
            if (!handled) async.unavailable()
            async.close()
        }
    }

    private fun transfer(
        id: UUID,
        management: Boolean,
        context: BrowserContext,
        response: HttpServletResponse,
    ) {
        artwork.read(id, management, context).use { asset ->
            if (!Context.current().isCancelled) {
                response.contentType = asset.contentType
                response.setContentLengthLong(asset.bytes)
                write(asset, response)
            }
        }
    }

    private fun write(
        asset: ArtworkReadProxy.VerifiedAsset,
        response: HttpServletResponse,
    ) {
        Files.newInputStream(asset.path).use { input ->
            copyArtwork(input, response)
        }
    }

    private fun copyArtwork(
        input: java.io.InputStream,
        response: HttpServletResponse,
    ) {
        val buffer = ByteArray(TRANSFER_BYTES)
        while (!Context.current().isCancelled) {
            val size = input.read(buffer)
            if (size < 0) break
            response.outputStream.write(buffer, 0, size)
        }
    }
}

private fun retryAfter(
    response: HttpServletResponse,
    failure: RateLimited,
) {
    val seconds = ((failure.retryAfterMs + SECOND_MS - 1) / SECOND_MS).coerceAtLeast(1)
    response.setHeader("Retry-After", seconds.toString())
    response.sendError(HTTP_RATE_LIMITED)
}

private fun artworkStatus(
    failure: StatusRuntimeException,
    publicRead: Boolean,
): Int =
    when (failure.status.code) {
        Status.Code.NOT_FOUND -> HttpServletResponse.SC_NOT_FOUND
        Status.Code.UNAUTHENTICATED -> HttpServletResponse.SC_UNAUTHORIZED
        Status.Code.PERMISSION_DENIED ->
            if (publicRead) HttpServletResponse.SC_NOT_FOUND else HttpServletResponse.SC_FORBIDDEN
        else -> HttpServletResponse.SC_SERVICE_UNAVAILABLE
    }

private class ArtworkHttpAsync(
    private val async: AsyncContext,
    val cancellation: Context.CancellableContext,
    private val response: HttpServletResponse,
) : AsyncListener,
    AutoCloseable {
    init {
        async.timeout = ARTWORK_TIMEOUT_MS
        async.addListener(this)
    }

    fun unavailable() {
        if (!response.isCommitted) response.status = HttpServletResponse.SC_SERVICE_UNAVAILABLE
    }

    override fun onError(event: AsyncEvent) {
        cancellation.cancel(event.throwable)
        close()
    }

    override fun onTimeout(event: AsyncEvent) {
        cancellation.cancel(java.util.concurrent.TimeoutException())
        unavailable()
        close()
    }

    override fun onComplete(event: AsyncEvent) {
        cancellation.cancel(null)
    }

    override fun onStartAsync(event: AsyncEvent) = Unit

    override fun close() {
        cancellation.cancel(null)
        try {
            async.complete()
        } catch (_: IllegalStateException) {
            // Container already completed the request.
        }
    }
}

private fun management(request: HttpServletRequest): Boolean {
    val view = request.parameterMap
    require(view.keys.all { it == "view" } && view.values.all { it.size == 1 })
    return view["view"]?.single()?.also { require(it == "management") } != null
}
