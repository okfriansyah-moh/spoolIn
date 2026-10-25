package com.spoolin.core.features.mcpgateway

import com.google.protobuf.ByteString
import com.google.protobuf.Empty
import com.spoolin.core.features.connector.ConnectorStore
import com.spoolin.core.features.policy.PolicyProvider
import com.spoolin.core.features.session.SessionJdbcFixture
import com.spoolin.support.identity.ActorSigner
import com.spoolin.support.policy.PolicyEnvironment
import io.grpc.Status
import io.grpc.netty.NettyChannelBuilder
import io.grpc.netty.NettyServerBuilder
import io.grpc.stub.StreamObserver
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.Spoolin
import java.net.InetSocketAddress
import java.security.KeyPairGenerator
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class McpGatewayBehaviorTest {
    @org.junit.jupiter.api.io.TempDir lateinit var root: java.nio.file.Path
    private val account = UUID.randomUUID()
    private val credential = UUID.randomUUID()
    private val proposal = UUID.randomUUID()

    @Test
    fun `management identity and policy require live maintainer connector`() = fixture { h ->
        h.authorize(2)
        h.authenticated {
            val identity = observe<Spoolin.Actor> { h.gateway.getReviewIdentity(Empty.getDefaultInstance(), it) }
            assertEquals(account.toString(), identity.value!!.accountId)
            assertEquals("CHATGPT", identity.value!!.clientKind)
            val policy = observe<Spoolin.PolicySnapshot> { h.gateway.getPolicy(Empty.getDefaultInstance(), it) }
            assertTrue(policy.value!!.revision.isNotBlank())
        }
        assertEquals(Status.Code.PERMISSION_DENIED, observe<Spoolin.Actor> { h.gateway.getReviewIdentity(Empty.getDefaultInstance(), it) }.failure)
        assertEquals(Status.Code.PERMISSION_DENIED, observe<Spoolin.PolicySnapshot> { h.gateway.getPolicy(Empty.getDefaultInstance(), it) }.failure)
    }

    @Test
    fun `context search clamps page size and combines genres roles and effective policy`() = fixture { h ->
        repeat(3) { h.forwardAuthorization() }
        h.authenticated {
            val context = observe<Spoolin.MovieContext> { h.gateway.getMovieContext(Spoolin.MovieContextRequest.newBuilder().setQuery("Film").setLimit(999).build(), it) }
            assertNull(context.failure)
            assertEquals(20, h.search!!.first)
            assertEquals("Film", h.search!!.text)
            assertEquals("drama", context.value!!.genres.nodesList.single().id)
            assertEquals("Actor", context.value!!.roles.nodesList.single().label)
            assertTrue(context.completed)
        }
    }

    @Test
    fun `proposal and queue retain exact revision challenge bindings for trusted UI`() = fixture { h ->
        h.forwardAuthorization(); h.forwardAuthorization()
        h.authenticated {
            val context = Spoolin.Context.newBuilder().setOperationId(UUID.randomUUID().toString()).setCorrelationId(UUID.randomUUID().toString())
            val revision = observe<Spoolin.ProposalRevision> { h.gateway.proposeMovie(Spoolin.Proposal.newBuilder().setContext(context).build(), it) }
            assertEquals(proposal.toString(), revision.value!!.proposalId)
            assertTrue(h.challenges.matchesBoundChallenge(proposal.toString(), 2, "hash", "private-challenge"))
            val queue = observe<Spoolin.MovieProposalQueue> { h.gateway.listMovieProposals(Spoolin.MovieProposalQueueRequest.newBuilder().setLimit(2).build(), it) }
            assertEquals(1, queue.value!!.proposalsCount)
        }
    }

    @Test
    fun `invalid poster queue bounds and untrusted approval reject before catalog access`() = fixture { h ->
        assertEquals(Status.Code.INVALID_ARGUMENT, observe<Spoolin.PreparedPoster> { h.gateway.prepareMoviePoster(Spoolin.PrepareMoviePosterRequest.getDefaultInstance(), it) }.failure)
        assertEquals(Status.Code.INVALID_ARGUMENT, observe<Spoolin.MovieProposalQueue> { h.gateway.listMovieProposals(Spoolin.MovieProposalQueueRequest.newBuilder().setLimit(21).build(), it) }.failure)
        val request = Spoolin.ApprovalInput.newBuilder().setProposalId(proposal.toString()).build()
        assertEquals(Status.Code.PERMISSION_DENIED, observe<Spoolin.Outcome> { h.gateway.approveMovie(request, it) }.failure)
        assertEquals(Status.Code.PERMISSION_DENIED, observe<Spoolin.Outcome> { h.gateway.rejectMovie(request, it) }.failure)
        assertThrows(io.grpc.StatusRuntimeException::class.java) { h.gateway.approveMovieBlocking(request) }
        assertThrows(io.grpc.StatusRuntimeException::class.java) { h.gateway.rejectMovieBlocking(request) }
        assertEquals(0, h.db.queries)
    }

    @Test
    fun `poster and operation reads preserve downstream outcomes`() = fixture { h ->
        h.forwardAuthorization(); h.forwardAuthorization()
        h.authenticated {
            val poster = observe<Spoolin.MoviePosterImage> { h.gateway.readMoviePoster(Spoolin.MoviePosterReadRequest.newBuilder().setRefId(proposal.toString()).build(), it) }
            assertArrayEquals(byteArrayOf(1, 2, 3), poster.value!!.imageBytes.toByteArray())
            val receipt = observe<Spoolin.Outcome> { h.gateway.getMovieOperation(Spoolin.Id.newBuilder().setId(proposal.toString()).build(), it) }
            assertEquals("PENDING", receipt.value!!.state)
        }
    }

    private inner class Harness(val db: SessionJdbcFixture, val challenges: ProposalUiChallengeStore) : CatalogServiceGrpc.CatalogServiceImplBase() {
        lateinit var gateway: McpGatewayRpc
        var search: Spoolin.Search? = null
        fun authorize(count: Int) { repeat(count) { db.enqueue(connector()); db.enqueue(mapOf("role" to "MAINTAINER")) } }
        fun forwardAuthorization() { authorize(1); db.enqueue(mapOf("role" to "MAINTAINER")) }
        fun authenticated(action: () -> Unit) { ConnectorRpcContext.context(ByteArray(32)).run(action) }
        private fun connector(): Map<String, Any?> = mapOf("id" to credential, "account_id" to account, "client_kind" to "CHATGPT", "auth_epoch" to 3L, "scope" to "movie:propose movie:approve", "expires_at" to Timestamp.from(Instant.now().plusSeconds(3600)), "revoked_at" to null)
        private fun revision() = Spoolin.ProposalRevision.newBuilder().setProposalId(proposal.toString()).setRevision(2).setContentHash("hash").setUiChallenge(ByteString.copyFromUtf8("private-challenge")).build()
        private fun <T> reply(observer: StreamObserver<T>, value: T) { observer.onNext(value); observer.onCompleted() }
        override fun searchTitles(request: Spoolin.Search, observer: StreamObserver<Spoolin.TitlePage>) { search = request; reply(observer, Spoolin.TitlePage.getDefaultInstance()) }
        override fun listGenres(request: Empty, observer: StreamObserver<Spoolin.Genres>) { reply(observer, Spoolin.Genres.newBuilder().addNodes(Spoolin.Genre.newBuilder().setId("drama").setLabel("Drama")).build()) }
        override fun listRoles(request: Empty, observer: StreamObserver<Spoolin.Roles>) { reply(observer, Spoolin.Roles.newBuilder().addNodes(Spoolin.CreditRole.newBuilder().setId("actor").setLabel("Actor")).build()) }
        override fun proposeMovie(request: Spoolin.Proposal, observer: StreamObserver<Spoolin.ProposalRevision>) = reply(observer, revision())
        override fun listMovieProposals(request: Spoolin.MovieProposalQueueRequest, observer: StreamObserver<Spoolin.MovieProposalQueue>) = reply(observer, Spoolin.MovieProposalQueue.newBuilder().addProposals(revision()).build())
        override fun readMoviePoster(request: Spoolin.MoviePosterReadRequest, observer: StreamObserver<Spoolin.MoviePosterImage>) = reply(observer, Spoolin.MoviePosterImage.newBuilder().setImageBytes(ByteString.copyFrom(byteArrayOf(1, 2, 3))).build())
        override fun getOperation(request: Spoolin.Id, observer: StreamObserver<Spoolin.Outcome>) = reply(observer, Spoolin.Outcome.newBuilder().setOperationId(request.id).setState("PENDING").build())
    }
    private fun fixture(test: (Harness) -> Unit) {
        val harness = Harness(SessionJdbcFixture(emptyList()), ProposalUiChallengeStore())
        val server = NettyServerBuilder.forAddress(InetSocketAddress("127.0.0.1", 0)).addService(harness).build().start()
        val channel = NettyChannelBuilder.forAddress("127.0.0.1", server.port).usePlaintext().build()
        try {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
            while (true) {
                val state = channel.getState(true); if (state == io.grpc.ConnectivityState.READY) break
                val remaining = deadline - System.nanoTime(); assertTrue(remaining > 0)
                val changed = CountDownLatch(1); channel.notifyWhenStateChanged(state, changed::countDown); assertTrue(changed.await(remaining, TimeUnit.NANOSECONDS))
            }
            val keys = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
            val keyFile = root.resolve("actor-key")
            java.nio.file.Files.write(keyFile, keys.private.encoded)
            java.nio.file.Files.setPosixFilePermissions(keyFile, java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"))
            val config = McpGatewayConfiguration()
            val environment = org.springframework.core.env.StandardEnvironment()
            assertNull(config.mcpActorIssuer(environment))
            environment.propertySources.addFirst(org.springframework.core.env.MapPropertySource("fixture", mapOf("spoolin.actor.enabled" to true, "ACTOR_SIGNING_KEY_ID" to "test", "ACTOR_SIGNING_PRIVATE_KEY_FILE" to keyFile.toString())))
            val issuer = config.mcpActorIssuer(environment)
            assertNotNull(issuer)
            val channels = object : org.springframework.grpc.client.GrpcChannelFactory {
                override fun supports(target: String) = true
                override fun createChannel(target: String, options: org.springframework.grpc.client.ChannelBuilderOptions): io.grpc.ManagedChannel = channel
            }
            val provider = PolicyProvider(PolicyEnvironment.resolve { null })
            config.uiAssertionVerifier(environment, tools.jackson.databind.ObjectMapper()).use { assertions ->
                val security = config.mcpGatewaySecurity(issuer, harness.challenges, assertions)
                val upload = config.preparedPosterUpload(channels, provider)
                harness.gateway = config.mcpGatewayRpc(ConnectorStore(harness.db), provider, channels, security, upload)
                val challengeStore = config.proposalUiChallengeStore()
                assertFalse(challengeStore.matchesBoundChallenge("unknown", 1, "hash", "challenge"))
                test(harness)
            }
        } finally { channel.shutdownNow(); server.shutdownNow() }
    }
    private fun <T> observe(call: (StreamObserver<T>) -> Unit): Result<T> {
        val result = Result<T>()
        call(object : StreamObserver<T> {
            override fun onNext(value: T) { result.value = value }
            override fun onError(failure: Throwable) { result.failure = Status.fromThrowable(failure).code }
            override fun onCompleted() { result.completed = true }
        })
        return result
    }
    private class Result<T>(var value: T? = null, var failure: Status.Code? = null, var completed: Boolean = false)
}
