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
import com.spoolin.support.receipts.WorkRequest
import com.spoolin.support.receipts.committed
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.MediaServiceGrpc
import spoolin.v1.Spoolin
import java.net.InetSocketAddress
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.sql.DataSource

/** Actual owner PostgreSQL and mTLS calls; Catalog authority is a controlled projection fixture. */
class HoldBoundaryTest {
    @TempDir lateinit var directory: Path
    private val hold = UUID.randomUUID()
    private val actor = UUID.randomUUID()
    private val title = UUID.randomUUID()
    private val asset = child("asset")
    private val correlation = UUID.randomUUID()
    private val projection = AtomicReference(attachment("PREPARING", 6))

    @Test
    fun `Catalog service holds are tuple bound atomic idempotent and never resurrect a released reference`() {
        database { fixture, pool ->
            val authority = object : CatalogServiceGrpc.CatalogServiceImplBase() {
                override fun getAttachment(request: Spoolin.Id, response: StreamObserver<Spoolin.Attachment>) {
                    assertEquals(hold.toString(), request.id)
                    response.onNext(projection.get()); response.onCompleted()
                }
            }
            val catalogServer = server(fixture, "catalog", authority)
            val downstream = channel(fixture, "media", "catalog", catalogServer.port)
            val catalog = CatalogAttachments(CatalogServiceGrpc.newBlockingStub(downstream))
            val store = QuarantineStore(directory)
            val rpc = MediaServiceImpl(UploadRpc(pool, store, "p1", catalog = catalog), HoldRpc(pool, catalog),
                OwnerReceiptReads(pool), AssetRpc(pool, store, catalog))
            val media = server(fixture, "media", rpc, true)
            val caller = channel(fixture, "catalog", "media", media.port)
            val core = channel(fixture, "core", "media", media.port)
            try {
                val client = MediaServiceGrpc.newBlockingStub(caller).withDeadlineAfter(10, TimeUnit.SECONDS)
                assertInvalidCommands(client)
                assertReserve(pool, client)
                assertLifecycle(pool, client)
                assertExpiredRecovery(pool, client)
                assertBindingProtection(pool)
                val wrong = MediaServiceGrpc.newBlockingStub(core).withDeadlineAfter(5, TimeUnit.SECONDS)
                assertEquals(Status.Code.PERMISSION_DENIED, assertThrows(StatusRuntimeException::class.java) {
                    wrong.reserve(command("media-reserve"))
                }.status.code)
                catalogServer.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
                assertEquals(Status.Code.UNAVAILABLE, assertThrows(StatusRuntimeException::class.java) {
                    client.release(command("media-release", 8))
                }.status.code)
                assertEquals("RELEASED", state(pool))
            } finally {
                listOf(caller, core, downstream).forEach { it.shutdownNow().awaitTermination(5, TimeUnit.SECONDS) }
                listOf(media, catalogServer).forEach { it.shutdownNow().awaitTermination(5, TimeUnit.SECONDS) }
            }
        }
    }

    private fun assertInvalidCommands(client: MediaServiceGrpc.MediaServiceBlockingStub) {
        val reserve = command("media-reserve")
        val badOperation = reserve.toBuilder().setContext(reserve.context.toBuilder()
            .setOperationId(UUID.randomUUID().toString())).build()
        assertEquals(Status.Code.INVALID_ARGUMENT, assertThrows(StatusRuntimeException::class.java) {
            client.reserve(badOperation)
        }.status.code)
        assertEquals(Status.Code.INVALID_ARGUMENT, assertThrows(StatusRuntimeException::class.java) {
            client.reserve(reserve.toBuilder().clearActorId().build())
        }.status.code)
        assertEquals(Status.Code.PERMISSION_DENIED, assertThrows(StatusRuntimeException::class.java) {
            client.reserve(reserve.toBuilder().setAssetId(UUID.randomUUID().toString()).build())
        }.status.code)
        assertEquals(Status.Code.FAILED_PRECONDITION, assertThrows(StatusRuntimeException::class.java) {
            client.confirm(command("media-confirm", 7))
        }.status.code)
        projection.set(attachment("UNKNOWN", 6))
        assertEquals(Status.Code.UNAVAILABLE, assertThrows(StatusRuntimeException::class.java) {
            client.reserve(reserve)
        }.status.code)
        projection.set(attachment("PREPARING", 6))
    }

    private fun assertReserve(pool: DataSource, client: MediaServiceGrpc.MediaServiceBlockingStub) {
        val reserve = command("media-reserve")
        val executor = Executors.newFixedThreadPool(2)
        try {
            val results = List(2) { executor.submit<Spoolin.Attachment> { client.reserve(reserve) } }
            results.forEach { assertEquals("RESERVED", it.get(10, TimeUnit.SECONDS).state) }
        } finally { executor.shutdownNow() }
        assertEquals("RESERVED", state(pool))
        assertEquals(1, count(pool, "SELECT count(*) FROM asset_reference WHERE id=?", hold))
        assertEquals(1, count(pool, "SELECT count(*) FROM operation_receipt WHERE id=?", child("media-reserve")))
        assertEquals(1, count(pool, "SELECT count(*) FROM operational_event WHERE operation_id=?", child("media-reserve")))
        val changed = reserve.toBuilder().setContext(reserve.context.toBuilder().setPolicyRevision("different")).build()
        assertEquals(Status.Code.ALREADY_EXISTS, assertThrows(StatusRuntimeException::class.java) {
            client.reserve(changed)
        }.status.code)
        val foreign = reserve.toBuilder().setActorId(UUID.randomUUID().toString()).build()
        assertEquals(Status.Code.PERMISSION_DENIED, assertThrows(StatusRuntimeException::class.java) {
            client.reserve(foreign)
        }.status.code)
        val receipt = client.getOperation(Spoolin.Id.newBuilder().setId(child("media-reserve").toString()).build())
        assertEquals("SUCCEEDED", receipt.state)
        assertEquals(hold.toString(), receipt.resultId)
    }

    private fun assertLifecycle(pool: DataSource, client: MediaServiceGrpc.MediaServiceBlockingStub) {
        projection.set(attachment("ACTIVE", 7))
        assertEquals("ACTIVE", client.confirm(command("media-confirm", 7)).state)
        assertEquals("ACTIVE", client.confirm(command("media-confirm", 7)).state)
        assertEquals(Status.Code.FAILED_PRECONDITION, assertThrows(StatusRuntimeException::class.java) {
            client.release(command("media-release", 8))
        }.status.code)
        assertEquals("ACTIVE", state(pool))
        projection.set(attachment("RELEASED", 8))
        assertEquals(Status.Code.FAILED_PRECONDITION, assertThrows(StatusRuntimeException::class.java) {
            client.release(command("media-release", 6))
        }.status.code)
        assertEquals(0, count(pool, "SELECT count(*) FROM operation_receipt WHERE id=?", child("media-release")))
        assertEquals("RELEASED", client.release(command("media-release", 8)).state)
        assertEquals("RELEASED", client.release(command("media-release", 8)).state)
        assertEquals("RELEASED", client.confirm(command("media-confirm", 7)).state)
        assertEquals("RELEASED", client.reserve(command("media-reserve")).state)
        assertEquals("RELEASED", state(pool))
        assertEquals(1, count(pool, "SELECT count(*) FROM operational_event WHERE operation_id=?", child("media-release")))
    }

    private fun assertExpiredRecovery(pool: DataSource, client: MediaServiceGrpc.MediaServiceBlockingStub) {
        // Controlled retention boundary: the same permanent receipt survives with its result purged.
        pool.committed { connection -> connection.executeBound(
            "UPDATE operation_receipt SET result=NULL,expires_at=CURRENT_TIMESTAMP-interval '1 day' WHERE id=?",
            child("media-release"),
        ) }
        val request = Spoolin.Id.newBuilder().setId(child("media-release").toString()).build()
        val receipt = client.getOperation(request)
        assertEquals("SUCCEEDED", receipt.state)
        assertEquals(true, receipt.resultExpired)
        assertEquals(false, receipt.hasResultId())
        assertEquals("RESULT_EXPIRED", receipt.problem.code)
        assertEquals("RELEASED", client.release(command("media-release", 8)).state)
        assertEquals(1, count(pool, "SELECT count(*) FROM operational_event WHERE operation_id=?", child("media-release")))
        assertEquals(1, count(pool, "SELECT count(*) FROM hold_operation_binding WHERE operation_id=?", child("media-release")))
        projection.set(attachment("RELEASED", 8).toBuilder().setActorId(UUID.randomUUID().toString()).build())
        assertEquals(Status.Code.NOT_FOUND, assertThrows(StatusRuntimeException::class.java) {
            client.getOperation(request)
        }.status.code)
        projection.set(attachment("RELEASED", 8))
    }

    private fun assertBindingProtection(pool: DataSource) {
        val failure = assertThrows(java.sql.SQLException::class.java) {
            pool.connection.use { it.executeBound(
                "UPDATE hold_operation_binding SET actor_id=? WHERE operation_id=?", UUID.randomUUID(), child("media-release"),
            ) }
        }
        assertEquals("42501", failure.sqlState)
        val mismatch = assertThrows(java.sql.SQLException::class.java) {
            pool.committed { connection ->
                val operation = UUID.randomUUID()
                connection.executeBound("INSERT INTO operation_receipt " +
                    "(id,actor_scope,kind,request_hash,state,correlation_id,expires_at) " +
                    "VALUES (?,'account:wrong','HOLD_RELEASE',?,'SUCCEEDED',?,CURRENT_TIMESTAMP+interval '7 days')",
                    operation, ByteArray(32), correlation)
                connection.executeBound(
                    "INSERT INTO hold_operation_binding(operation_id,hold_id,actor_id,verb) VALUES (?,?,?,'RELEASE')",
                    operation, hold, actor,
                )
            }
        }
        assertEquals("23514", mismatch.sqlState)
    }

    private fun command(step: String, version: Long? = null): Spoolin.Hold = Spoolin.Hold.newBuilder()
        .setHoldId(hold.toString()).setAssetId(asset.toString()).setTitleId(title.toString()).setActorId(actor.toString())
        .setContext(Spoolin.Context.newBuilder().setOperationId(child(step).toString())
            .setCorrelationId(correlation.toString()).setPolicyRevision("p1"))
        .also { if (version == null) it.expectedVersion = 6 else it.catalogVersion = version }.build()

    private fun attachment(state: String, version: Long): Spoolin.Attachment = Spoolin.Attachment.newBuilder()
        .setHoldId(hold.toString()).setAssetId(asset.toString()).setTitleId(title.toString()).setActorId(actor.toString())
        .setState(state).setCatalogVersion(version).build()

    private fun child(step: String): UUID = WorkRequest(step, "ARTWORK", hold, Instant.MAX).id(hold)
    private fun state(pool: DataSource): String = pool.connection.use { connection -> connection.queryBound(
        "SELECT state FROM asset_reference WHERE id=?", listOf(hold),
    ) { it.getString("state") }.single() }
    private fun count(pool: DataSource, sql: String, id: UUID): Int = pool.connection.use { connection ->
        connection.queryBound(sql, listOf(id)) { it.getInt(1) }.single()
    }

    private fun database(action: (OwnerFixture, DataSource) -> Unit) {
        OwnerFixture("media-hold-boundary.json").start().use { fixture ->
            fixture.membership("media", "grant")
            fixture.pool("media", "migrator").use { MigrationRunner(it).migrate() }
            fixture.grants("media"); fixture.membership("media", "revoke")
            fixture.pool("media").use { pool ->
                pool.committed { connection ->
                    connection.executeBound("INSERT INTO operation_receipt " +
                        "(id,actor_scope,kind,request_hash,state,correlation_id,expires_at) " +
                        "VALUES (?,?,'UPLOAD',?,'SUCCEEDED',?,CURRENT_TIMESTAMP+interval '7 days')",
                        child("media-upload"), "account:$actor", ByteArray(32), correlation)
                    connection.executeBound("INSERT INTO asset " +
                        "(id,upload_operation_id,actor_id,state,storage_key,content_hash,format,input_bytes,output_bytes," +
                        "width,height,policy_revision) VALUES (?,?,?,'READY',?,?,'PNG',1,1,1,1,'p1')",
                        asset, child("media-upload"), actor, "output/$asset.png", ByteArray(32))
                }
                action(fixture, pool)
            }
        }
    }

    private fun server(fixture: OwnerFixture, name: String, rpc: io.grpc.BindableService, media: Boolean = false): Server {
        val tls = GrpcSslContexts.forServer(fixture.root.resolve("$name.crt").toFile(), fixture.root.resolve("$name.key").toFile())
            .trustManager(fixture.root.resolve("ca.crt").toFile()).clientAuth(ClientAuth.REQUIRE).build()
        val builder = NettyServerBuilder.forAddress(InetSocketAddress("127.0.0.1", 0)).sslContext(tls)
        if (media) builder.addService(ServerInterceptors.intercept(rpc, ServiceIdentityInterceptor())) else builder.addService(rpc)
        return builder.build().start()
    }

    private fun channel(fixture: OwnerFixture, caller: String, receiver: String, port: Int): ManagedChannel {
        val tls = GrpcSslContexts.forClient().trustManager(fixture.root.resolve("ca.crt").toFile())
            .keyManager(fixture.root.resolve("$caller.crt").toFile(), fixture.root.resolve("$caller.key").toFile()).build()
        return NettyChannelBuilder.forAddress("127.0.0.1", port).overrideAuthority(receiver).sslContext(tls).build()
    }
}
