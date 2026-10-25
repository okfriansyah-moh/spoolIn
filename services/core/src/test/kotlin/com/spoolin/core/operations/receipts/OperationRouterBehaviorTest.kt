package com.spoolin.core.operations.receipts

import com.spoolin.support.receipts.*
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.netty.NettyChannelBuilder
import io.grpc.netty.NettyServerBuilder
import io.grpc.stub.StreamObserver
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.MediaServiceGrpc
import spoolin.v1.Spoolin
import java.net.InetSocketAddress
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class OperationRouterBehaviorTest {
    private val id = UUID.randomUUID()
    private val resultId = UUID.randomUUID()

    @Test
    fun `explicit owner returns its receipt and unqualified lookup rejects ambiguous ownership`() = fixture { catalog, media ->
        val local = ReceiptOutcome(id, ReceiptState.SUCCEEDED, SafeResult(resultId, 4), null, UUID.randomUUID(), Instant.now(), false)
        val router = OperationRouter(ReceiptAuthority { "account:viewer" }, { _, _ -> local }, catalog, media)
        assertEquals("4", router.find(id, OperationOwner.CORE)!!["version"])
        assertEquals(resultId.toString(), router.find(id, OperationOwner.CORE)!!["resultId"])
        assertEquals("PENDING", router.find(id, OperationOwner.CATALOG)!!["state"])
        assertNull(router.find(id, OperationOwner.MEDIA))
        assertThrows(AmbiguousOperation::class.java) { router.find(id, null) }
    }

    @Test
    fun `unknown receipts remain absent while unauthenticated and unavailable reads fail explicitly`() = fixture { catalog, media ->
        val router = OperationRouter(ReceiptAuthority { "account:viewer" }, { _, _ -> null }, catalog, media)
        assertNull(router.find(UUID.randomUUID(), null))
        val denied = OperationRouter(ReceiptAuthority { null }, { _, _ -> null }, catalog, media)
        assertEquals(Status.Code.PERMISSION_DENIED, assertThrows(StatusRuntimeException::class.java) { denied.find(id, OperationOwner.CORE) }.status.code)
        val unavailable = OperationRouter(ReceiptAuthority { "account:viewer" }, { _, _ -> error("database unavailable") }, catalog, media)
        assertEquals(Status.Code.UNAVAILABLE, assertThrows(StatusRuntimeException::class.java) { unavailable.find(id, OperationOwner.CORE) }.status.code)
    }

    @Test
    fun `expired local result preserves state without leaking removed identifiers or version`() = fixture { catalog, media ->
        val local = ReceiptOutcome(id, ReceiptState.SUCCEEDED, null, "RESULT_EXPIRED", UUID.randomUUID(), Instant.now(), true)
        val router = OperationRouter(ReceiptAuthority { "account:viewer" }, { _, _ -> local }, catalog, media)
        val result = router.find(id, OperationOwner.CORE)!!
        assertTrue(result["resultExpired"] as Boolean)
        assertNull(result["resultId"])
        assertNull(result["version"])
        assertEquals("RESULT_EXPIRED", (result["problem"] as Map<*, *>)["code"])
    }

    private fun fixture(test: (CatalogServiceGrpc.CatalogServiceBlockingStub, MediaServiceGrpc.MediaServiceBlockingStub) -> Unit) {
        val catalog = object : CatalogServiceGrpc.CatalogServiceImplBase() {
            override fun getOperation(request: Spoolin.Id, observer: StreamObserver<Spoolin.Outcome>) {
                if (request.id == id.toString()) {
                    observer.onNext(Spoolin.Outcome.newBuilder().setOperationId(id.toString()).setState("PENDING").build()); observer.onCompleted()
                } else observer.onError(Status.NOT_FOUND.asRuntimeException())
            }
        }
        val media = object : MediaServiceGrpc.MediaServiceImplBase() {
            override fun getOperation(request: Spoolin.Id, observer: StreamObserver<Spoolin.Outcome>) { observer.onError(Status.NOT_FOUND.asRuntimeException()) }
        }
        val server = NettyServerBuilder.forAddress(InetSocketAddress("127.0.0.1", 0)).addService(catalog).addService(media).build().start()
        val channel = NettyChannelBuilder.forAddress("127.0.0.1", server.port).usePlaintext().build()
        try {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
            while (true) {
                val state = channel.getState(true)
                if (state == io.grpc.ConnectivityState.READY) break
                val remaining = deadline - System.nanoTime(); assertTrue(remaining > 0)
                val changed = CountDownLatch(1); channel.notifyWhenStateChanged(state, changed::countDown)
                assertTrue(changed.await(remaining, TimeUnit.NANOSECONDS))
            }
            test(CatalogServiceGrpc.newBlockingStub(channel), MediaServiceGrpc.newBlockingStub(channel))
        } finally { channel.shutdownNow(); server.shutdownNow() }
    }
}
