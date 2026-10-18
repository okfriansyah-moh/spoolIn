package com.spoolin.catalog.features.proposal

import com.spoolin.catalog.MigrationRunner
import com.spoolin.catalog.ServiceIdentityInterceptor
import com.spoolin.catalog.boundary.bootstrap.OwnerFixture
import com.spoolin.catalog.features.movie.MovieWrites
import com.spoolin.catalog.operations.receipts.OwnerReceipts
import com.spoolin.support.identity.ActorClaims
import com.spoolin.support.identity.ActorInterceptor
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.identity.ActorSigner
import com.spoolin.support.identity.ActorVerifier
import com.spoolin.support.receipts.IntentHashes
import com.spoolin.support.receipts.queryBound
import io.grpc.Metadata
import io.grpc.ServerInterceptors
import io.grpc.netty.GrpcSslContexts
import io.grpc.netty.NettyChannelBuilder
import io.grpc.netty.NettyServerBuilder
import io.grpc.stub.MetadataUtils
import io.grpc.stub.StreamObserver
import io.netty.handler.ssl.ClientAuth
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.Spoolin
import java.net.InetSocketAddress
import java.security.KeyPairGenerator
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Base64
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val KEY_BYTES = 32
private const val ASSERTION_SECONDS = 30L
private const val RPC_SECONDS = 10L
private const val STOP_SECONDS = 5L
private const val NONCE_BYTES = 16

/** Actual PostgreSQL and mutual TLS; synthetic signer admission isolates Catalog proposal rules from host proof. */
internal class ProposalFixture : AutoCloseable {
    private val fixture = OwnerFixture("t007-proposal-boundary.json").start()
    val actor: UUID = UUID.randomUUID()
    val connector: UUID = UUID.randomUUID()
    var now: Instant = Instant.now()
    var policy = "p1"
    private val clock = object : Clock() {
        override fun instant(): Instant = now
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
    }
    private val keys = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
    private val signer = ActorSigner("test", keys.private)
    val pool: com.zaxxer.hikari.HikariDataSource
    private val server: io.grpc.Server
    private val channel: io.grpc.ManagedChannel

    init {
        fixture.membership("catalog", "grant")
        fixture.pool("catalog", "migrator").use { MigrationRunner(it).migrate() }
        fixture.grants("catalog")
        fixture.membership("catalog", "revoke")
        pool = fixture.pool("catalog")
        val receipts = OwnerReceipts(pool, IntentHashes(mapOf("test" to ByteArray(KEY_BYTES) { 1 }), "test"), clock)
        val writes = ProposalWrites(pool, receipts, MovieWrites(receipts, clock), clock = clock) { policy }
        val methods = setOf("ProposeMovie", "ApproveAndCreate", "RejectProposal")
            .map { "spoolin.v1.CatalogService/$it" }.toSet()
        val actors = ActorInterceptor("catalog", methods, ActorVerifier(mapOf("test" to keys.public)), { claims ->
            require(claims.authEpoch == 1L)
        })
        server = NettyServerBuilder.forAddress(InetSocketAddress("127.0.0.1", 0)).sslContext(serverTls())
            .addService(ServerInterceptors.intercept(service(writes), ServiceIdentityInterceptor(actors))).build().start()
        channel = NettyChannelBuilder.forAddress("127.0.0.1", server.port).overrideAuthority("catalog")
            .sslContext(clientTls()).build()
    }

    private fun service(writes: ProposalWrites): CatalogServiceGrpc.CatalogServiceImplBase =
        object : CatalogServiceGrpc.CatalogServiceImplBase() {
            override fun proposeMovie(r: Spoolin.Proposal, o: StreamObserver<Spoolin.ProposalRevision>) = writes.propose(r, o)
            override fun approveAndCreate(r: Spoolin.ApprovalInput, o: StreamObserver<Spoolin.Outcome>) = writes.approve(r, o)
            override fun rejectProposal(r: Spoolin.ApprovalInput, o: StreamObserver<Spoolin.Outcome>) = writes.reject(r, o)
        }

    private fun serverTls(): io.netty.handler.ssl.SslContext = GrpcSslContexts.forServer(
        fixture.root.resolve("catalog.crt").toFile(), fixture.root.resolve("catalog.key").toFile(),
    ).trustManager(fixture.root.resolve("ca.crt").toFile()).clientAuth(ClientAuth.REQUIRE).build()

    private fun clientTls(): io.netty.handler.ssl.SslContext = GrpcSslContexts.forClient()
        .trustManager(fixture.root.resolve("ca.crt").toFile())
        .keyManager(fixture.root.resolve("core.crt").toFile(), fixture.root.resolve("core.key").toFile()).build()

    fun stub(
        method: String,
        operation: UUID? = null,
        account: UUID = actor,
        credential: UUID = connector,
        client: String = "CLAUDE",
        correlation: UUID = UUID.randomUUID(),
    ): CatalogServiceGrpc.CatalogServiceBlockingStub {
        val issued = Instant.now().epochSecond
        val claims = ActorClaims(
            "catalog", "/spoolin.v1.CatalogService/$method", account, credential, client, "MAINTAINER", 1,
            issued, issued + ASSERTION_SECONDS,
            Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(NONCE_BYTES) { 1 }),
            correlation, operation,
        )
        val headers = Metadata().apply { put(ActorRpc.metadata, signer.sign(claims)) }
        return CatalogServiceGrpc.newBlockingStub(channel).withDeadlineAfter(RPC_SECONDS, TimeUnit.SECONDS)
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))
    }

    fun row(id: String, revision: Int): ProposalRow = pool.connection.use {
        ProposalStore.requireForUpdate(it, UUID.fromString(id), revision)
    }

    fun count(sql: String, value: Any): Int = pool.connection.use {
        it.queryBound(sql, listOf(value)) { rows -> rows.getInt(1) }.single()
    }

    fun receipt(id: UUID): Pair<String, String?> = pool.connection.use {
        it.queryBound("SELECT state,result::text FROM operation_receipt WHERE id=?", listOf(id)) { row ->
            row.getString("state") to row.getString("result")
        }.single()
    }

    override fun close() {
        channel.shutdownNow().awaitTermination(STOP_SECONDS, TimeUnit.SECONDS)
        server.shutdownNow().awaitTermination(STOP_SECONDS, TimeUnit.SECONDS)
        pool.close()
        fixture.close()
    }
}
