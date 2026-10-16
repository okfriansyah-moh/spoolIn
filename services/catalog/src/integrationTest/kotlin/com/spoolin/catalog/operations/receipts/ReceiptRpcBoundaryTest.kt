package com.spoolin.catalog.operations.receipts

import com.spoolin.catalog.MigrationRunner
import com.spoolin.catalog.ServiceIdentityInterceptor
import com.spoolin.catalog.boundary.bootstrap.OwnerFixture
import com.spoolin.support.receipts.IntentHashes
import com.spoolin.support.receipts.OperationIntent
import com.spoolin.support.receipts.ReceiptAuthority
import com.spoolin.support.receipts.SafeResult
import io.grpc.ServerInterceptors
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.netty.GrpcSslContexts
import io.grpc.netty.NettyChannelBuilder
import io.grpc.netty.NettyServerBuilder
import io.netty.handler.ssl.ClientAuth
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.Spoolin.Id
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class ReceiptRpcBoundaryTest {
    @Test
    fun `actual TCP mutual TLS routes owner receipt and denies unverified actor before lookup`() {
        OwnerFixture("t010-catalog-rpc.json").start().use { fixture ->
            fixture.membership("catalog", "grant")
            fixture.pool("catalog", "migrator").use { MigrationRunner(it).migrate() }
            fixture.grants("catalog")
            fixture.membership("catalog", "revoke")
            fixture.pool("catalog").use { pool ->
                val receipts = OwnerReceipts(pool, IntentHashes(mapOf("v1" to ByteArray(32) { 31 }), "v1"))
                val intent = OperationIntent(UUID.randomUUID(), "account:A", "WriteMovie", "p1", emptyList())
                receipts.execute(intent, UUID.randomUUID(), true) { SafeResult(intent.id, version = 7) }
                val actor = AtomicReference<String?>("account:A")
                val calls = AtomicInteger()
                val rpc =
                    ReceiptRpc(ReceiptAuthority { actor.get() }) { id, scope ->
                        calls.incrementAndGet()
                        OwnerReceiptReads(pool).find(id, scope)
                    }
                transport(fixture, rpc) { client ->
                    val request = Id.newBuilder().setId(intent.id.toString()).build()
                    val result = client.getOperation(request)
                    assertEquals("SUCCEEDED", result.state)
                    assertEquals(intent.id.toString(), result.resultId)
                    assertEquals(7L, result.version)
                    actor.set("account:B")
                    assertEquals(
                        Status.Code.NOT_FOUND,
                        assertThrows(StatusRuntimeException::class.java) {
                            client.getOperation(request)
                        }.status.code,
                    )
                    actor.set(null)
                    val before = calls.get()
                    assertEquals(
                        Status.Code.PERMISSION_DENIED,
                        assertThrows(StatusRuntimeException::class.java) {
                            client.getOperation(request)
                        }.status.code,
                    )
                    assertEquals(before, calls.get())
                }
            }
        }
    }

    private fun transport(
        fixture: OwnerFixture,
        rpc: ReceiptRpc,
        action: (CatalogServiceGrpc.CatalogServiceBlockingStub) -> Unit,
    ) {
        val serverTls =
            GrpcSslContexts
                .forServer(
                    fixture.root.resolve("catalog.crt").toFile(),
                    fixture.root.resolve("catalog.key").toFile(),
                ).trustManager(fixture.root.resolve("ca.crt").toFile())
                .clientAuth(ClientAuth.REQUIRE)
                .build()
        val server =
            NettyServerBuilder
                .forAddress(InetSocketAddress("127.0.0.1", 0))
                .sslContext(serverTls)
                .addService(ServerInterceptors.intercept(rpc, ServiceIdentityInterceptor()))
                .build()
                .start()
        val clientTls =
            GrpcSslContexts
                .forClient()
                .trustManager(fixture.root.resolve("ca.crt").toFile())
                .keyManager(fixture.root.resolve("core.crt").toFile(), fixture.root.resolve("core.key").toFile())
                .build()
        val channel =
            NettyChannelBuilder
                .forAddress("127.0.0.1", server.port)
                .overrideAuthority("catalog")
                .sslContext(clientTls)
                .build()
        try {
            action(CatalogServiceGrpc.newBlockingStub(channel).withDeadlineAfter(3, TimeUnit.SECONDS))
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
            server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
        }
    }
}
