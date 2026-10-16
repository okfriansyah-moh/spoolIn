package com.spoolin.core.operations.receipts

import com.spoolin.core.boundary.bootstrap.OwnerFixture
import com.spoolin.support.receipts.OperationOwner
import com.spoolin.support.receipts.ReceiptAuthority
import com.spoolin.support.receipts.ReceiptOutcome
import com.spoolin.support.receipts.ReceiptState
import io.grpc.BindableService
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.netty.GrpcSslContexts
import io.grpc.netty.NettyChannelBuilder
import io.grpc.netty.NettyServerBuilder
import io.grpc.stub.StreamObserver
import io.netty.handler.ssl.ClientAuth
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.MediaServiceGrpc
import spoolin.v1.Spoolin.Id
import spoolin.v1.Spoolin.Outcome
import java.net.InetSocketAddress
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class OperationRouterBoundaryTest {
    @Test
    fun `explicit owners and omitted ambiguity use typed TLS clients without database fanout`() {
        OwnerFixture("t010-core-router.json").start().use { fixture ->
            val id = UUID.randomUUID()
            val catalogCalls = AtomicInteger()
            val mediaCalls = AtomicInteger()
            val catalog =
                object : CatalogServiceGrpc.CatalogServiceImplBase() {
                    override fun getOperation(
                        request: Id,
                        observer: StreamObserver<Outcome>,
                    ) = answer(request, observer, id, catalogCalls)
                }
            val media =
                object : MediaServiceGrpc.MediaServiceImplBase() {
                    override fun getOperation(
                        request: Id,
                        observer: StreamObserver<Outcome>,
                    ) = answer(request, observer, null, mediaCalls)
                }
            transport(fixture, "catalog", catalog) { catalogChannel ->
                transport(fixture, "media", media) { mediaChannel ->
                    val actor = AtomicReference<String?>("account:A")
                    val localCalls = AtomicInteger()
                    val router =
                        OperationRouter(ReceiptAuthority { actor.get() }, { requested, scope ->
                            localCalls.incrementAndGet()
                            if (requested == id && scope == "account:A") {
                                ReceiptOutcome(
                                    id,
                                    ReceiptState.SUCCEEDED,
                                    null,
                                    null,
                                    UUID.randomUUID(),
                                    Instant.now(),
                                    false,
                                )
                            } else {
                                null
                            }
                        }, CatalogServiceGrpc.newBlockingStub(catalogChannel), MediaServiceGrpc.newBlockingStub(mediaChannel))
                    assertEquals(id.toString(), router.find(id, OperationOwner.CATALOG)?.get("id"))
                    assertEquals(1, catalogCalls.get())
                    assertEquals(0, localCalls.get())
                    assertEquals(0, mediaCalls.get())
                    assertThrows(IllegalStateException::class.java) { router.find(id, null) }
                    assertNull(router.find(UUID.randomUUID(), null))
                    actor.set(null)
                    val calls = catalogCalls.get() + mediaCalls.get() + localCalls.get()
                    assertThrows(StatusRuntimeException::class.java) { router.find(id, OperationOwner.CORE) }
                    assertEquals(calls, catalogCalls.get() + mediaCalls.get() + localCalls.get())
                }
            }
        }
    }

    private fun answer(
        request: Id,
        observer: StreamObserver<Outcome>,
        known: UUID?,
        calls: AtomicInteger,
    ) {
        calls.incrementAndGet()
        if (request.id != known?.toString()) {
            observer.onError(Status.NOT_FOUND.asRuntimeException())
        } else {
            observer.onNext(
                Outcome
                    .newBuilder()
                    .setOperationId(request.id)
                    .setState("SUCCEEDED")
                    .build(),
            )
            observer.onCompleted()
        }
    }

    private fun transport(
        fixture: OwnerFixture,
        owner: String,
        service: BindableService,
        action: (io.grpc.ManagedChannel) -> Unit,
    ) {
        val serverTls =
            GrpcSslContexts
                .forServer(
                    fixture.root.resolve("$owner.crt").toFile(),
                    fixture.root.resolve("$owner.key").toFile(),
                ).trustManager(fixture.root.resolve("ca.crt").toFile())
                .clientAuth(ClientAuth.REQUIRE)
                .build()
        val server =
            NettyServerBuilder
                .forAddress(InetSocketAddress("127.0.0.1", 0))
                .sslContext(serverTls)
                .addService(service)
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
                .overrideAuthority(owner)
                .sslContext(clientTls)
                .build()
        try {
            action(channel)
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
            server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
        }
    }
}
