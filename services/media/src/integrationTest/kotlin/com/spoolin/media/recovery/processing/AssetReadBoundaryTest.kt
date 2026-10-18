package com.spoolin.media.recovery.processing

import com.spoolin.media.MigrationRunner
import com.spoolin.media.ServiceIdentityInterceptor
import com.spoolin.media.boundary.bootstrap.OwnerFixture
import com.spoolin.media.features.asset.AssetRpc
import com.spoolin.media.features.asset.CatalogAttachments
import com.spoolin.media.features.hold.HoldRpc
import com.spoolin.media.features.upload.MediaServiceImpl
import com.spoolin.media.features.upload.QuarantineStore
import com.spoolin.media.features.upload.UploadRpc
import com.spoolin.media.operations.receipts.OwnerReceiptReads
import com.spoolin.support.receipts.committed
import com.spoolin.support.receipts.executeBound
import io.grpc.ManagedChannel
import io.grpc.Server
import io.grpc.ServerInterceptors
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.netty.GrpcSslContexts
import io.grpc.netty.NettyChannelBuilder
import io.grpc.netty.NettyServerBuilder
import io.grpc.stub.StreamObserver
import io.netty.handler.ssl.ClientAuth
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.MediaServiceGrpc
import spoolin.v1.Spoolin
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.sql.DataSource

/** Actual mTLS transports and owner PostgreSQL. Catalog projection is a controlled authority fixture. */
class AssetReadBoundaryTest {
    @TempDir lateinit var directory: Path

    @Test
    fun `real stream rechecks current Catalog tuple before header and hides stale grants and foreign services`() {
        OwnerFixture("media-asset-stream.json").start().use { fixture ->
            fixture.membership("media", "grant")
            fixture.pool("media", "migrator").use { MigrationRunner(it).migrate() }
            fixture.grants("media")
            fixture.membership("media", "revoke")
            fixture.pool("media").use { pool -> verifyReads(fixture, pool) }
        }
    }

    private fun verifyReads(fixture: OwnerFixture, pool: DataSource) {
        val asset = UUID.randomUUID()
        val hold = UUID.randomUUID()
        val title = UUID.randomUUID()
        val store = QuarantineStore(directory)
        val bytes = ByteArray(150000) { (it % 251).toByte() }
        Files.write(store.outputDirectory().resolve("$asset.png"), bytes)
        seed(pool, asset, hold, title, bytes)
        val projection = AtomicReference(Spoolin.Attachment.newBuilder().setHoldId(hold.toString())
            .setAssetId(asset.toString()).setTitleId(title.toString()).setState("ACTIVE").setCatalogVersion(9).build())
        val calls = AtomicInteger()
        val authority = object : CatalogServiceGrpc.CatalogServiceImplBase() {
            override fun getAttachment(request: Spoolin.Id, response: StreamObserver<Spoolin.Attachment>) {
                assertEquals(hold.toString(), request.id)
                calls.incrementAndGet()
                response.onNext(projection.get()); response.onCompleted()
            }
        }
        val catalogServer = server(fixture, "catalog", authority)
        val catalogChannel = channel(fixture, "media", "catalog", catalogServer.port)
        val assets = AssetRpc(pool, store, CatalogAttachments(CatalogServiceGrpc.newBlockingStub(catalogChannel)))
        val media = MediaServiceImpl(UploadRpc(pool, store, "p1"), HoldRpc(pool), OwnerReceiptReads(pool), assets)
        val mediaServer = server(fixture, "media", media, true)
        val core = channel(fixture, "core", "media", mediaServer.port)
        val foreign = channel(fixture, "mcp", "media", mediaServer.port)
        try {
            val request = Spoolin.AssetReadRequest.newBuilder().setAssetId(asset.toString()).setHoldId(hold.toString())
                .setCatalogVersion(9).build()
            val client = MediaServiceGrpc.newBlockingStub(core).withDeadlineAfter(5, TimeUnit.SECONDS)
            assertBytes(client, request, bytes)
            val catalogCaller = channel(fixture, "catalog", "media", mediaServer.port)
            try {
                val metadata = MediaServiceGrpc.newBlockingStub(catalogCaller).getAsset(
                    Spoolin.Id.newBuilder().setId(asset.toString()).build(),
                )
                assertEquals("READY", metadata.state)
            } finally { catalogCaller.shutdownNow().awaitTermination(5, TimeUnit.SECONDS) }
            assertRejected(client, request, projection, calls, foreign)
        } finally {
            listOf(core, foreign, catalogChannel).forEach { it.shutdownNow().awaitTermination(5, TimeUnit.SECONDS) }
            listOf(mediaServer, catalogServer).forEach { it.shutdownNow().awaitTermination(5, TimeUnit.SECONDS) }
        }
    }

    private fun assertBytes(client: MediaServiceGrpc.MediaServiceBlockingStub, request: Spoolin.AssetReadRequest, bytes: ByteArray) {
        val chunks = client.readAsset(request).asSequence().toList()
        assertTrue(chunks.first().hasHeader())
        assertEquals(1, chunks.count { it.hasHeader() })
        assertEquals(bytes.size.toLong(), chunks.first().header.outputBytes)
        assertArrayEquals(MessageDigest.getInstance("SHA-256").digest(bytes), chunks.first().header.contentHash.toByteArray())
        assertTrue(chunks.drop(1).all { it.data.size() in 1..65536 })
        assertArrayEquals(bytes, chunks.drop(1).flatMap { it.data.toByteArray().asIterable() }.toByteArray())
    }

    private fun assertRejected(
        client: MediaServiceGrpc.MediaServiceBlockingStub,
        request: Spoolin.AssetReadRequest,
        projection: AtomicReference<Spoolin.Attachment>,
        calls: AtomicInteger,
        foreign: ManagedChannel,
    ) {
        val before = calls.get()
        projection.set(projection.get().toBuilder().setCatalogVersion(10).build())
        assertEquals(Status.Code.NOT_FOUND, assertThrows(StatusRuntimeException::class.java) {
            client.readAsset(request).hasNext()
        }.status.code)
        assertTrue(calls.get() > before)
        assertEquals(Status.Code.PERMISSION_DENIED, assertThrows(StatusRuntimeException::class.java) {
            MediaServiceGrpc.newBlockingStub(foreign).readAsset(request).hasNext()
        }.status.code)
        projection.set(projection.get().toBuilder().setCatalogVersion(9).setState("RELEASED").build())
        assertEquals(Status.Code.NOT_FOUND, assertThrows(StatusRuntimeException::class.java) {
            client.readAsset(request).hasNext()
        }.status.code)
    }

    private fun seed(pool: DataSource, asset: UUID, hold: UUID, title: UUID, bytes: ByteArray) = pool.committed { c ->
        val operation = UUID.randomUUID()
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes)
        c.executeBound("INSERT INTO operation_receipt " +
            "(id,actor_scope,kind,request_hash,state,correlation_id,expires_at) " +
            "VALUES (?,'account:test','UPLOAD',?,'SUCCEEDED',?,CURRENT_TIMESTAMP+interval '7 days')",
            operation, hash, operation)
        c.executeBound("INSERT INTO asset " +
            "(id,upload_operation_id,actor_id,state,storage_key,content_hash,format,input_bytes,output_bytes," +
            "width,height,policy_revision) VALUES (?,?,?,'READY',?,?,'PNG',?,?,1,1,'p1')",
            asset, operation, UUID.randomUUID(), "output/$asset.png", hash, bytes.size.toLong(), bytes.size.toLong())
        c.executeBound("INSERT INTO asset_reference (id,asset_id,title_id,state,catalog_version) VALUES (?,?,?,'ACTIVE',4)",
            hold, asset, title)
    }

    private fun server(fixture: OwnerFixture, name: String, service: io.grpc.BindableService, media: Boolean = false): Server {
        val tls = GrpcSslContexts.forServer(fixture.root.resolve("$name.crt").toFile(), fixture.root.resolve("$name.key").toFile())
            .trustManager(fixture.root.resolve("ca.crt").toFile()).clientAuth(ClientAuth.REQUIRE).build()
        val builder = NettyServerBuilder.forAddress(InetSocketAddress("127.0.0.1", 0)).sslContext(tls)
        if (media) builder.addService(ServerInterceptors.intercept(service, ServiceIdentityInterceptor(
            com.spoolin.media.boundary.grpc.MediaActors.interceptor(org.springframework.core.env.StandardEnvironment()),
        )))
        else builder.addService(service)
        return builder.build().start()
    }

    private fun channel(fixture: OwnerFixture, caller: String, receiver: String, port: Int): ManagedChannel {
        val tls = GrpcSslContexts.forClient().trustManager(fixture.root.resolve("ca.crt").toFile())
            .keyManager(fixture.root.resolve("$caller.crt").toFile(), fixture.root.resolve("$caller.key").toFile()).build()
        return NettyChannelBuilder.forAddress("127.0.0.1", port).overrideAuthority(receiver).sslContext(tls).build()
    }
}
