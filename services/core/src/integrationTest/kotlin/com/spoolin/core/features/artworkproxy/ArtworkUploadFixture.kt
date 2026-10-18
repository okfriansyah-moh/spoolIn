package com.spoolin.core.features.artworkproxy

import com.spoolin.core.boundary.bootstrap.OwnerFixture
import com.spoolin.core.features.session.SessionPrincipal
import com.spoolin.core.identity.AccountAuthority
import com.spoolin.core.identity.AccountRole
import com.spoolin.support.identity.ActorClaims
import com.spoolin.support.identity.ActorInterceptor
import com.spoolin.support.identity.ActorSigner
import com.spoolin.support.identity.ActorVerifier
import io.grpc.Context
import io.grpc.ServerInterceptors
import io.grpc.netty.GrpcSslContexts
import io.grpc.netty.NettyChannelBuilder
import io.grpc.netty.NettyServerBuilder
import io.grpc.stub.StreamObserver
import io.netty.handler.ssl.ClientAuth
import spoolin.v1.MediaServiceGrpc
import spoolin.v1.Spoolin
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.security.KeyPairGenerator
import java.time.Instant
import java.util.Base64
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

private const val NONCE_BYTES = 16
private const val ASSERTION_SECONDS = 30L
private const val STOP_SECONDS = 5L

/** Real mTLS and signed actor admission with explicit synthetic downstream protocol faults; no host proof. */
internal class ArtworkUploadFixture(private val mode: Mode) : AutoCloseable {
    enum class Mode { NORMAL, DUPLICATE, MISMATCH, HOLD }
    private val fixture = OwnerFixture("t007-artwork-upload.json").start()
    private val actor = UUID.randomUUID()
    private val session = UUID.randomUUID()
    private val keys = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
    private val signer = ActorSigner("test", keys.private)
    val operation: UUID = UUID.randomUUID()
    val asset: UUID = UUID.randomUUID()
    val intent: UUID = UUID.randomUUID()
    val correlation: UUID = UUID.randomUUID()
    val cancelled = CountDownLatch(1)
    val header = AtomicReference<Spoolin.UploadHeader>()
    val bytes = ByteArrayOutputStream()
    private val server: io.grpc.Server
    private val channel: io.grpc.ManagedChannel

    init {
        val actors = ActorInterceptor("media", setOf("spoolin.v1.MediaService/Upload"),
            ActorVerifier(mapOf("test" to keys.public)), { claims ->
                require(claims.accountId == actor && claims.credentialId == session && claims.role == "MAINTAINER")
            })
        val tls = GrpcSslContexts.forServer(fixture.root.resolve("media.crt").toFile(),
            fixture.root.resolve("media.key").toFile()).trustManager(fixture.root.resolve("ca.crt").toFile())
            .clientAuth(ClientAuth.REQUIRE).build()
        server = NettyServerBuilder.forAddress(InetSocketAddress("127.0.0.1", 0)).sslContext(tls)
            .addService(ServerInterceptors.intercept(service(), actors)).build().start()
        val clientTls = GrpcSslContexts.forClient().trustManager(fixture.root.resolve("ca.crt").toFile())
            .keyManager(fixture.root.resolve("core.crt").toFile(), fixture.root.resolve("core.key").toFile()).build()
        channel = NettyChannelBuilder.forAddress("127.0.0.1", server.port).overrideAuthority("media")
            .sslContext(clientTls).build()
    }

    fun request(spool: ArtworkSpool): ArtworkUploadRequest {
        val now = Instant.now()
        val principal = SessionPrincipal(session, AccountAuthority(actor, AccountRole.MAINTAINER, 1),
            now.plusSeconds(ASSERTION_SECONDS), UUID.randomUUID().toString())
        val claims = ActorClaims("media", "/spoolin.v1.MediaService/Upload", actor, session, null, "MAINTAINER", 1,
            now.epochSecond, now.epochSecond + ASSERTION_SECONDS,
            Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(NONCE_BYTES) { 1 }), correlation, operation)
        return ArtworkUploadRequest(MediaServiceGrpc.newStub(channel), signer.sign(claims), principal,
            operation, correlation, "p1", asset, intent, spool)
    }

    private fun service(): MediaServiceGrpc.MediaServiceImplBase = object : MediaServiceGrpc.MediaServiceImplBase() {
        override fun upload(response: StreamObserver<Spoolin.Outcome>): StreamObserver<Spoolin.UploadChunk> {
            Context.current().addListener({ cancelled.countDown() }, java.util.concurrent.Executor { it.run() })
            return observer(response)
        }
    }

    private fun observer(response: StreamObserver<Spoolin.Outcome>): StreamObserver<Spoolin.UploadChunk> =
        object : StreamObserver<Spoolin.UploadChunk> {
            override fun onNext(value: Spoolin.UploadChunk) {
                if (value.hasHeader()) header.set(value.header) else bytes.write(value.data.toByteArray())
            }
            override fun onError(t: Throwable) = Unit
            override fun onCompleted() { finish(response) }
        }

    private fun finish(response: StreamObserver<Spoolin.Outcome>) {
        if (mode == Mode.HOLD) return
        val id = if (mode == Mode.MISMATCH) UUID.randomUUID() else operation
        val outcome = Spoolin.Outcome.newBuilder().setOperationId(id.toString()).setState("PENDING").build()
        response.onNext(outcome)
        if (mode == Mode.DUPLICATE) response.onNext(outcome)
        response.onCompleted()
    }

    override fun close() {
        channel.shutdownNow().awaitTermination(STOP_SECONDS, TimeUnit.SECONDS)
        server.shutdownNow().awaitTermination(STOP_SECONDS, TimeUnit.SECONDS)
        fixture.close()
    }
}
