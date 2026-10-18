package com.spoolin.integration.artwork

import com.google.protobuf.ByteString
import com.spoolin.core.boundary.http.ArtworkServing
import com.spoolin.core.boundary.http.BrowserContext
import com.spoolin.core.boundary.http.BrowserIdentity
import com.spoolin.core.features.artworkproxy.ArtworkGraphql
import com.spoolin.core.features.artworkproxy.ArtworkReadProxy
import com.spoolin.core.features.ratelimit.TrustedAdmission
import graphql.schema.idl.RuntimeWiring
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.netty.NettyChannelBuilder
import io.grpc.netty.NettyServerBuilder
import io.grpc.stub.ServerCallStreamObserver
import io.grpc.stub.StreamObserver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.MediaServiceGrpc
import spoolin.v1.Spoolin
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Actual loopback gRPC with controlled owners; separate from deployed mTLS/browser acceptance. */
class ArtworkMetadataBoundaryTest {
    @Test
    fun `metadata uses the live grant and processed dimensions then cancels the body`() {
        fixture { proxy, _, cancelled, requests ->
            val metadata = proxy.metadata(asset, false, browser)
            assertEquals(640, metadata.width)
            assertEquals(480, metadata.height)
            assertEquals("/artwork/$asset", metadata.fields()["url"])
            assertTrue(cancelled.await(3, TimeUnit.SECONDS))
            assertEquals(1, requests.get())
        }
    }

    @Test
    fun `private metadata rejects absent actor before either owner is called`() {
        fixture { proxy, _, _, requests ->
            val failure = assertThrows(StatusRuntimeException::class.java) { proxy.metadata(asset, true, browser) }
            assertEquals(Status.Code.PERMISSION_DENIED, failure.status.code)
            assertEquals(0, requests.get())
        }
    }

    @Test
    fun `truncated stream does not become a verified HTTP asset`() {
        fixture(truncate = true) { proxy, _, _, _ ->
            val failure = assertThrows(StatusRuntimeException::class.java) { proxy.read(asset, false, browser) }
            assertEquals(Status.Code.DATA_LOSS, failure.status.code)
        }
    }

    @Test
    fun `Title artwork resolver merges with existing credit resolver using pinned runtime wiring`() {
        fixture { proxy, channel, _, _ ->
            val wiring = RuntimeWiring.newRuntimeWiring()
            wiring.type("Title") { it.dataFetcher("credits") { "credits" } }
            ArtworkGraphql(
                CatalogServiceGrpc.newBlockingStub(channel), MediaServiceGrpc.newStub(channel), null, reads = proxy,
            ).bind(wiring)
            assertTrue(
                wiring
                    .build()
                    .getDataFetchersForType("Title")
                    .keys
                    .containsAll(listOf("credits", "artwork")),
            )
        }
    }

    @Test
    fun `HTTP rejects range and unknown view and fails closed without admission`() {
        fixture { proxy, _, _, requests ->
            val beans = DefaultListableBeanFactory()
            val endpoint =
                ArtworkServing(
                    proxy,
                    beans.getBeanProvider(BrowserIdentity::class.java),
                    beans.getBeanProvider(TrustedAdmission::class.java),
                )
            try {
                fun response(request: MockHttpServletRequest): Int {
                    val output = MockHttpServletResponse()
                    endpoint.get(asset.toString(), request, output)
                    assertEquals("no-store", output.getHeader("Cache-Control"))
                    assertEquals("nosniff", output.getHeader("X-Content-Type-Options"))
                    return output.status
                }
                assertEquals(416, response(MockHttpServletRequest().apply { addHeader("Range", "bytes=0-1") }))
                assertEquals(404, response(MockHttpServletRequest().apply { addParameter("management", "true") }))
                assertEquals(404, response(MockHttpServletRequest().apply { addParameter("view", "unknown") }))
                assertEquals(503, response(MockHttpServletRequest()))
                assertEquals(0, requests.get())
            } finally {
                endpoint.close()
            }
        }
    }

    private fun fixture(
        truncate: Boolean = false,
        assertions: (ArtworkReadProxy, io.grpc.ManagedChannel, CountDownLatch, AtomicInteger) -> Unit,
    ) {
        val cancelled = CountDownLatch(1)
        val requests = AtomicInteger()
        val catalog =
            object : CatalogServiceGrpc.CatalogServiceImplBase() {
                override fun resolveArtworkAccess(
                    request: Spoolin.ArtworkAccessRequest,
                    output: StreamObserver<Spoolin.ArtworkGrant>,
                ) {
                    requests.incrementAndGet()
                    assertEquals(asset.toString(), request.assetId)
                    output.onNext(
                        Spoolin.ArtworkGrant
                            .newBuilder()
                            .setAssetId(asset.toString())
                            .setHoldId(hold.toString())
                            .setTitleId(title.toString())
                            .setCatalogVersion(7)
                            .build(),
                    )
                    output.onCompleted()
                }
            }
        val media =
            object : MediaServiceGrpc.MediaServiceImplBase() {
                override fun readAsset(
                    request: Spoolin.AssetReadRequest,
                    output: StreamObserver<Spoolin.AssetReadChunk>,
                ) {
                    assertEquals(hold.toString(), request.holdId)
                    assertEquals(asset.toString(), request.assetId)
                    assertEquals(7L, request.catalogVersion)
                    (output as ServerCallStreamObserver<Spoolin.AssetReadChunk>).setOnCancelHandler {
                        cancelled.countDown()
                    }
                    output.onNext(
                        Spoolin.AssetReadChunk
                            .newBuilder()
                            .setHeader(
                                Spoolin.AssetReadHeader
                                    .newBuilder()
                                    .setFormat("png")
                                    .setWidth(640)
                                    .setHeight(480)
                                    .setOutputBytes(3)
                                    .setContentHash(
                                        ByteString.copyFrom(
                                            MessageDigest.getInstance("SHA-256").digest(byteArrayOf(1, 2, 3)),
                                        ),
                                    ),
                            ).build(),
                    )
                    // Metadata must not consume a body; full reads must reject the deliberate missing bytes.
                    if (truncate) output.onCompleted()
                }
            }
        val server =
            NettyServerBuilder
                .forAddress(InetSocketAddress("127.0.0.1", 0))
                .addService(catalog)
                .addService(media)
                .build()
                .start()
        val channel = NettyChannelBuilder.forAddress("127.0.0.1", server.port).usePlaintext().build()
        try {
            assertions(
                ArtworkReadProxy(
                    CatalogServiceGrpc.newBlockingStub(channel), MediaServiceGrpc.newBlockingStub(channel), null,
                ),
                channel,
                cancelled,
                requests,
            )
        } finally {
            channel.shutdownNow().awaitTermination(3, TimeUnit.SECONDS)
            server.shutdownNow().awaitTermination(3, TimeUnit.SECONDS)
        }
    }

    companion object {
        private val asset = UUID.randomUUID()
        private val hold = UUID.randomUUID()
        private val title = UUID.randomUUID()
        private val browser = BrowserContext(null, null, null, null)
    }
}
