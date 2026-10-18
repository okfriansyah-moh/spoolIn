package com.spoolin.integration.observability

import com.spoolin.support.observability.LogicalMetrics
import com.spoolin.support.observability.LogicalResult
import com.spoolin.support.observability.LogicalRpcMetricsInterceptor
import com.spoolin.support.observability.MetricCode
import com.spoolin.support.observability.MetricOutcome
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.netty.NettyChannelBuilder
import io.grpc.netty.NettyServerBuilder
import io.grpc.stub.StreamObserver
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.MediaServiceGrpc
import spoolin.v1.Spoolin
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Real loopback gRPC exercises the metrics interceptor, not production mTLS, persistence or host acceptance. */
class LogicalRpcMetricsBoundaryTest {
    private val operationId = UUID.randomUUID().toString()

    @Test
    fun `gRPC OK typed domain failure increments actual domain result and duration once`() =
        fixture { registry, channel, probe ->
            probe.reply = failed("FIELD_INVALID")
            val returned = stub(channel).writeMovie(Spoolin.MovieWrite.getDefaultInstance())
            assertEquals("FAILED", returned.state)
            assertEquals(1.0, count(registry, "WriteMovie", "DOMAIN_REJECTED", "FIELD_INVALID"))
            assertEquals(
                0,
                registry
                    .find("spoolin.operation.results")
                    .tag("outcome", "SUCCESS")
                    .counters()
                    .size,
            )
            val timer = registry.get("spoolin.operation.duration").tag("operation", "WriteMovie").timer()
            assertEquals(1, timer.count())
            assertTrue(timer.totalTime(TimeUnit.NANOSECONDS) > 0)
            assertEquals(10, timer.takeSnapshot().histogramCounts().size)
        }

    @Test
    fun `unknown application error code never becomes success or a request derived label`() =
        fixture { registry, channel, probe ->
            val untrusted = "private-account-" + UUID.randomUUID()
            probe.reply = failed(untrusted)
            stub(channel).writeMovie(Spoolin.MovieWrite.getDefaultInstance())
            assertEquals(1.0, count(registry, "WriteMovie", "UNCERTAIN", "UNKNOWN"))
            assertFalse(registry.meters.flatMap { it.id.tags }.any { it.value.contains(untrusted) })
        }

    @Test
    fun `actual terminal dependency failure is recorded once without successful fallback`() =
        fixture { registry, channel, probe ->
            probe.failure = Status.UNAVAILABLE
            assertThrows(StatusRuntimeException::class.java) {
                stub(channel).writeMovie(Spoolin.MovieWrite.getDefaultInstance())
            }
            assertEquals(1.0, count(registry, "WriteMovie", "SYSTEM_FAILED", "UNAVAILABLE"))
            assertEquals(1, registry.get("spoolin.operation.duration").timer().count())
        }

    @Test
    fun `PENDING replies and missing unary replies are uncertain while receipt lookups preserve query meaning`() =
        fixture { registry, channel, probe ->
            probe.reply =
                Spoolin.Outcome
                    .newBuilder()
                    .setOperationId(operationId)
                    .setState("PENDING")
                    .build()
            stub(channel).writeMovie(Spoolin.MovieWrite.getDefaultInstance())
            assertEquals(1.0, count(registry, "WriteMovie", "UNCERTAIN", "PENDING"))
            probe.reply = failed("FIELD_INVALID")
            stub(channel).getOperation(
                Spoolin.Id
                    .newBuilder()
                    .setId(operationId)
                    .build(),
            )
            assertEquals(1.0, count(registry, "GetOperation", "SUCCESS", "OK"))
            probe.omit = true
            assertThrows(StatusRuntimeException::class.java) {
                stub(channel).writeMovie(Spoolin.MovieWrite.getDefaultInstance())
            }
            assertEquals(1.0, count(registry, "WriteMovie", "UNCERTAIN", "UNKNOWN"))
        }

    @Test
    fun `streamed response followed by actual terminal deadline is failed rather than counted as early success`() {
        SimpleMeterRegistry().useRegistry { registry ->
            val metrics = LogicalMetrics(registry, "media")
            val media =
                object : MediaServiceGrpc.MediaServiceImplBase() {
                    override fun readAsset(
                        request: Spoolin.AssetReadRequest,
                        response: StreamObserver<Spoolin.AssetReadChunk>,
                    ) {
                        response.onNext(
                            Spoolin.AssetReadChunk
                                .newBuilder()
                                .setHeader(
                                    Spoolin.AssetReadHeader
                                        .newBuilder()
                                        .setOutputBytes(3)
                                        .setFormat("PNG")
                                        .setWidth(1)
                                        .setHeight(1),
                                ).build(),
                        )
                        response.onError(Status.DEADLINE_EXCEEDED.asRuntimeException())
                    }
                }
            val server =
                NettyServerBuilder
                    .forAddress(InetSocketAddress("127.0.0.1", 0))
                    .addService(media)
                    .intercept(LogicalRpcMetricsInterceptor(metrics))
                    .build()
                    .start()
            val channel = NettyChannelBuilder.forAddress("127.0.0.1", server.port).usePlaintext().build()
            try {
                val frames =
                    MediaServiceGrpc
                        .newBlockingStub(channel)
                        .withDeadlineAfter(3, TimeUnit.SECONDS)
                        .readAsset(Spoolin.AssetReadRequest.getDefaultInstance())
                assertThrows(StatusRuntimeException::class.java) { while (frames.hasNext()) frames.next() }
                assertEquals(1.0, count(registry, "ReadAsset", "SYSTEM_FAILED", "DEADLINE_EXCEEDED"))
            } finally {
                close(channel)
                server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
            }
        }
    }

    @Test
    fun `actual client cancellation closes one measured streaming request without early success`() {
        SimpleMeterRegistry().useRegistry { registry ->
            val media =
                object : MediaServiceGrpc.MediaServiceImplBase() {
                    override fun readAsset(
                        request: Spoolin.AssetReadRequest,
                        response: StreamObserver<Spoolin.AssetReadChunk>,
                    ) {
                        response.onNext(
                            Spoolin.AssetReadChunk
                                .newBuilder()
                                .setHeader(
                                    Spoolin.AssetReadHeader
                                        .newBuilder()
                                        .setOutputBytes(3)
                                        .setFormat("PNG")
                                        .setWidth(1)
                                        .setHeight(1),
                                ).build(),
                        )
                    }
                }
            val server =
                NettyServerBuilder
                    .forAddress(InetSocketAddress("127.0.0.1", 0))
                    .addService(media)
                    .intercept(LogicalRpcMetricsInterceptor(LogicalMetrics(registry, "media")))
                    .build()
                    .start()
            val channel = NettyChannelBuilder.forAddress("127.0.0.1", server.port).usePlaintext().build()
            try {
                val call = channel.newCall(MediaServiceGrpc.getReadAssetMethod(), io.grpc.CallOptions.DEFAULT)
                val frame = java.util.concurrent.CountDownLatch(1)
                io.grpc.stub.ClientCalls.asyncServerStreamingCall(
                    call,
                    Spoolin.AssetReadRequest.getDefaultInstance(),
                    object : StreamObserver<Spoolin.AssetReadChunk> {
                        override fun onNext(value: Spoolin.AssetReadChunk) {
                            frame.countDown()
                        }

                        override fun onError(error: Throwable) = Unit

                        override fun onCompleted() = Unit
                    },
                )
                assertTrue(frame.await(3, TimeUnit.SECONDS))
                call.cancel("Synthetic metrics cancellation", null)
                assertEquals(1.0, count(registry, "ReadAsset", "SYSTEM_FAILED", "CANCELLED"))
                assertEquals(1, registry.get("spoolin.operation.duration").timer().count())
            } finally {
                close(channel)
                server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
            }
        }
    }

    @Test
    fun `HTTP200 GraphQL response classifier distinguishes domain pending and malformed replies`() {
        val failed =
            mapOf(
                "data" to
                    mapOf(
                        "createMovie" to
                            mapOf(
                                "operation" to mapOf("state" to "FAILED"),
                                "problem" to mapOf("code" to "FIELD_INVALID"),
                            ),
                    ),
            )
        val result = LogicalResult.fromGraphql(failed, 200, setOf("createMovie"))
        assertEquals(MetricOutcome.DOMAIN_REJECTED, result.outcome)
        assertEquals(MetricCode.FIELD_INVALID, result.code)
        val pending = mapOf("data" to mapOf("uploadArtwork" to mapOf("operation" to mapOf("state" to "PENDING"))))
        assertEquals(MetricOutcome.UNCERTAIN, LogicalResult.fromGraphql(pending, 200, setOf("uploadArtwork")).outcome)
        assertEquals(
            MetricCode.UNKNOWN,
            LogicalResult.fromGraphql(mapOf("data" to emptyMap<String, Any>()), 200, setOf("createMovie")).code,
        )
        val error = mapOf("errors" to listOf(mapOf("extensions" to mapOf("code" to "UNAVAILABLE"))))
        assertEquals(MetricOutcome.SYSTEM_FAILED, LogicalResult.fromGraphql(error, 200, setOf("titles")).outcome)
    }

    @Test
    fun `ingress cannot masquerade as Core owner and arbitrary operation labels are rejected`() {
        SimpleMeterRegistry().useRegistry { registry ->
            val metrics = LogicalMetrics(registry, "core")
            val start = System.nanoTime()
            assertFalse(metrics.complete("createMovie", "owner", LogicalResult.success(), start))
            assertFalse(metrics.complete(UUID.randomUUID().toString(), "ingress", LogicalResult.success(), start))
            assertTrue(registry.meters.isEmpty())
            assertTrue(metrics.complete("createMovie", "ingress", LogicalResult.fromProblem("FIELD_INVALID"), start))
            assertEquals(
                1.0,
                registry
                    .get("spoolin.operation.results")
                    .tag("boundary", "ingress")
                    .counter()
                    .count(),
            )
        }
    }

    private fun failed(code: String): Spoolin.Outcome =
        Spoolin.Outcome
            .newBuilder()
            .setOperationId(operationId)
            .setState("FAILED")
            .setProblem(Spoolin.Problem.newBuilder().setCode(code))
            .build()

    private fun count(
        registry: SimpleMeterRegistry,
        operation: String,
        outcome: String,
        code: String,
    ): Double {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
        while (System.nanoTime() < deadline) {
            val value =
                registry
                    .find("spoolin.operation.results")
                    .tags("operation", operation, "outcome", outcome, "code", code)
                    .counter()
                    ?.count()
            if (value != null && value >= 1) return value
            Thread.sleep(5)
        }
        error("Expected actual completed RPC measurement was not observed")
    }

    private fun stub(channel: ManagedChannel): CatalogServiceGrpc.CatalogServiceBlockingStub =
        CatalogServiceGrpc.newBlockingStub(channel).withDeadlineAfter(3, TimeUnit.SECONDS)

    private fun close(channel: ManagedChannel) {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
    }

    private fun fixture(action: (SimpleMeterRegistry, ManagedChannel, CatalogProbe) -> Unit) {
        SimpleMeterRegistry().useRegistry { registry ->
            val probe = CatalogProbe()
            val server =
                NettyServerBuilder
                    .forAddress(InetSocketAddress("127.0.0.1", 0))
                    .addService(probe)
                    .intercept(LogicalRpcMetricsInterceptor(LogicalMetrics(registry, "catalog")))
                    .build()
                    .start()
            val channel = NettyChannelBuilder.forAddress("127.0.0.1", server.port).usePlaintext().build()
            try {
                action(registry, channel, probe)
            } finally {
                close(channel)
                server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
            }
        }
    }

    private class CatalogProbe : CatalogServiceGrpc.CatalogServiceImplBase() {
        var reply = Spoolin.Outcome.getDefaultInstance()
        var failure: Status? = null
        var omit = false

        override fun writeMovie(
            request: Spoolin.MovieWrite,
            response: StreamObserver<Spoolin.Outcome>,
        ) = respond(response)

        override fun getOperation(
            request: Spoolin.Id,
            response: StreamObserver<Spoolin.Outcome>,
        ) = respond(response)

        private fun respond(response: StreamObserver<Spoolin.Outcome>) {
            val error = failure
            if (error != null) {
                response.onError(error.asRuntimeException())
                return
            }
            if (!omit) response.onNext(reply)
            response.onCompleted()
        }
    }
}

private inline fun <R> SimpleMeterRegistry.useRegistry(action: (SimpleMeterRegistry) -> R): R =
    try {
        action(this)
    } finally {
        close()
    }
