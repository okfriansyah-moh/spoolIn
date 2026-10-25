package com.spoolin.core.operations.receipts

import com.spoolin.core.boundary.graphql.GraphqlEnvelope
import com.spoolin.core.boundary.grpc.ActorIssuer
import com.spoolin.core.boundary.http.BrowserContext
import com.spoolin.core.features.account.application.AuthenticationRequest
import com.spoolin.core.features.ratelimit.*
import com.spoolin.core.features.session.*
import com.spoolin.support.identity.ActorSigner
import com.spoolin.support.identity.RateProfiles
import com.spoolin.support.policy.ConfigurationSources
import com.spoolin.support.policy.PolicyEnvironment
import com.spoolin.support.receipts.*
import io.grpc.netty.NettyChannelBuilder
import io.grpc.netty.NettyServerBuilder
import io.grpc.stub.StreamObserver
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.MediaServiceGrpc
import spoolin.v1.Spoolin
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.security.KeyPairGenerator
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class OperationQueriesBehaviorTest {
    @TempDir lateinit var root: Path
    private val id = UUID.randomUUID()
    private val account = UUID.randomUUID()
    private val session = UUID.randomUUID()
    private val now = Instant.now()
    private val hashes = IntentHashes(mapOf("compare" to ByteArray(32) { 1 }), "compare")

    @Test
    fun `authenticated core operation is admitted and remembered within account scope`() = fixture { h ->
        val principal = SessionPrincipal(session, com.spoolin.core.identity.AccountAuthority(account, com.spoolin.core.identity.AccountRole.VIEWER, 3), now.plusSeconds(3600), "csrf")
        val context = BrowserContext(principal, null, null, null, AuthenticationRequest("127.0.0.1", session))
        h.rate(); h.db.enqueue(); h.db.enqueue(row("account:$account", "SaveDraft"))
        h.prepare(context, "CORE")
        assertEquals("SUCCEEDED", context.operation(id, OperationOwner.CORE)!!["state"])
        assertEquals(id.toString(), context.operation(id, OperationOwner.CORE)!!["id"])
    }

    @Test
    fun `anonymous own registration receipt is remembered and unidentified lookup stays absent`() = fixture { h ->
        val anonymous = AnonymousCsrf(ByteArray(32) { 2 }).issue()
        val context = BrowserContext(null, anonymous, null, null, AuthenticationRequest("127.0.0.1", null))
        h.rate(); h.db.enqueue(emptyMap()); h.db.enqueue(row(anonymous.scope, "REGISTER"))
        h.prepare(context, "CORE")
        assertEquals("SUCCEEDED", context.operation(id, OperationOwner.CORE)!!["state"])
        val hidden = BrowserContext(null, null, null, null, AuthenticationRequest("127.0.0.1", null))
        h.rate(); h.prepare(hidden, "CATALOG")
        assertNull(hidden.operation(id, OperationOwner.CATALOG))
    }

    @Test
    fun `remote catalog operation is routed with actor identity and remembered under explicit owner`() = fixture { h ->
        val principal = SessionPrincipal(session, com.spoolin.core.identity.AccountAuthority(account, com.spoolin.core.identity.AccountRole.MAINTAINER, 3), now.plusSeconds(3600), "csrf")
        val context = BrowserContext(principal, null, null, null, AuthenticationRequest("127.0.0.1", session))
        h.rate(); h.prepare(context, "CATALOG")
        assertEquals("PENDING", context.operation(id, OperationOwner.CATALOG)!!["state"])
        assertNull(context.operation(id, OperationOwner.CORE))
    }

    @Test
    fun `completed sign out can reconcile retained credential and delete cookie after identity no longer authenticates`() = fixture { h ->
        val credential = SessionCredential.issue(); val csrf = h.csrf.derive(credential)
        val intent = OperationIntent(id, "account:$account", "SignOut", "signout-v1", listOf("session" to session.toString()))
        val context = BrowserContext(null, null, credential, csrf, AuthenticationRequest("127.0.0.1", null))
        h.rate(); h.db.enqueue(mapOf("id" to session, "account_id" to account))
        h.db.enqueue(row(intent.actorScope, intent.kind) + mapOf("request_hash" to hashes.hash(intent, "compare"), "hash_key_id" to "compare"))
        h.prepare(context, "CORE")
        assertEquals(listOf(SessionCredential.deletion()), context.cookies())
        assertEquals("SUCCEEDED", context.operation(id, OperationOwner.CORE)!!["state"])
    }

    private fun row(scope: String, kind: String): Map<String, Any?> = mapOf("id" to id, "state" to "SUCCEEDED", "expires_at" to Timestamp.from(now.plusSeconds(3600)), "created_at" to Timestamp.from(now), "result" to SafeResult(id).json(), "error_code" to null, "correlation_id" to UUID.randomUUID(), "actor_scope" to scope, "kind" to kind, "request_hash" to ByteArray(32), "hash_key_id" to null, "receipt_type" to "REQUEST")
    private inner class Harness(val db: SessionJdbcFixture, val queries: OperationQueries, val admission: TrustedAdmission, val csrf: SessionCsrf) {
        fun rate() { db.enqueue(mapOf("1" to now.epochSecond)); db.enqueue(emptyMap()) }
        fun prepare(context: BrowserContext, owner: String) {
            val parsed = GraphqlEnvelope(PolicyEnvironment.resolve { null }).prepare(mapOf("query" to "{operation(id:\"$id\",owner:$owner){id state}}"))
            queries.prepare(parsed, context, admission)
        }
    }
    private fun fixture(test: (Harness) -> Unit) {
        val service = object : CatalogServiceGrpc.CatalogServiceImplBase() {
            override fun getOperation(request: Spoolin.Id, observer: StreamObserver<Spoolin.Outcome>) { observer.onNext(Spoolin.Outcome.newBuilder().setOperationId(request.id).setState("PENDING").build()); observer.onCompleted() }
        }
        val server = NettyServerBuilder.forAddress(InetSocketAddress("127.0.0.1", 0)).addService(service).build().start()
        val channel = NettyChannelBuilder.forAddress("127.0.0.1", server.port).usePlaintext().build()
        try {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
            while (true) {
                val state = channel.getState(true); if (state == io.grpc.ConnectivityState.READY) break
                val remaining = deadline - System.nanoTime(); assertTrue(remaining > 0)
                val ready = CountDownLatch(1); channel.notifyWhenStateChanged(state, ready::countDown); assertTrue(ready.await(remaining, TimeUnit.NANOSECONDS))
            }
            val db = SessionJdbcFixture(emptyList()); val csrf = SessionCsrf(ByteArray(32) { 3 })
            val profiles = RateProfiles(ConfigurationSources()); val key = ByteArray(32) { 101 }; val file = root.resolve("agreement.json")
            val canary = Mac.getInstance("HmacSHA256").also { it.init(SecretKeySpec(key, "HmacSHA256")) }.doFinal("spoolin-rate-agreement-v1".toByteArray())
            Files.writeString(file, "{\"profileRevision\":\"${profiles.revision}\",\"keyCanary\":\"${java.util.HexFormat.of().formatHex(canary)}\"}"); Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"))
            val admission = TrustedAdmission(RateAdmission(db, profiles, key, RateAgreement(file, profiles.revision, key)::check))
            val signer = ActorIssuer(ActorSigner("test", KeyPairGenerator.getInstance("Ed25519").generateKeyPair().private))
            val queries = OperationQueries(OwnerReceiptReads(db), OwnerReceipts(db, hashes), SessionStore(db, csrf), signer, CatalogServiceGrpc.newBlockingStub(channel), MediaServiceGrpc.newBlockingStub(channel))
            test(Harness(db, queries, admission, csrf))
        } finally { channel.shutdownNow(); server.shutdownNow() }
    }
}
