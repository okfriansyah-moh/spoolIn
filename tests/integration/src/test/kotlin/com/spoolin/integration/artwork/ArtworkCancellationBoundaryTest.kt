package com.spoolin.integration.artwork

import com.spoolin.catalog.boundary.grpc.CatalogActors
import com.spoolin.catalog.features.artwork.ArtworkRpc
import com.spoolin.catalog.features.artwork.ArtworkWorkStore
import com.spoolin.catalog.operations.receipts.OwnerReceipts
import com.spoolin.core.features.account.persistence.AccountRole
import com.spoolin.core.features.account.persistence.IdentityRules
import com.spoolin.core.features.crypto.StoredPasswordHash
import com.spoolin.integration.identity.ScopedActorTest
import com.spoolin.integration.identity.SignedReceiptFixture
import com.spoolin.support.identity.ActorClaims
import com.spoolin.support.identity.ActorInterceptor
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.identity.ActorVerifier
import com.spoolin.support.receipts.IntentHashes
import com.spoolin.support.receipts.WorkRequest
import com.spoolin.support.receipts.committed
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import com.spoolin.support.work.WorkClaim
import com.spoolin.support.work.WorkSql
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.MetadataUtils
import io.grpc.stub.StreamObserver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.IdentityServiceGrpc
import spoolin.v1.Spoolin
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.sql.DataSource

/** Catalog transaction/receipt proof over actual PostgreSQL and signed mTLS, not a Media/browser acceptance claim. */
class ArtworkCancellationBoundaryTest {
    @Test
    fun `winning cancel atomically persists audit receipt original failure and release and replays once`() {
        CancellationFixture().use { fixture ->
            val upload = fixture.begin()
            val cancel = Cancellation(upload.id)
            val result = fixture.cancel(cancel)
            assertTrue(result.cancelled)
            assertEquals("SUCCEEDED", result.operation.state)
            assertEquals("FAILED", result.uploadOperation.state)
            assertEquals("CANCELLED", result.uploadOperation.problem.code)
            assertEquals(result, fixture.cancel(cancel))
            val state = fixture.state(upload)
            assertEquals("ABORTED", state.intent); assertEquals("FAILED", state.receipt)
            assertEquals(0L, state.version); assertEquals(null, state.asset); assertEquals(null, state.hold)
            assertEquals(1, fixture.count("SELECT count(*) FROM catalog_audit_event WHERE operation_id=? AND action='CANCEL_ARTWORK'", cancel.operation))
            assertEquals(1, fixture.count("SELECT count(*) FROM durable_work WHERE operation_id=? AND kind='ARTWORK_RELEASE'", upload.id))
        }
    }

    @Test
    fun `lost cancellation response reconciles through the same receipt without extra audit or release`() {
        CancellationFixture().use { fixture ->
            val upload = fixture.begin(); val cancel = Cancellation(upload.id)
            fixture.dropNextCancel.set(true)
            val failure = assertThrows(StatusRuntimeException::class.java) { fixture.cancel(cancel) }
            assertEquals(Status.Code.UNAVAILABLE, failure.status.code)
            val repeated = fixture.cancel(cancel)
            assertTrue(repeated.cancelled); assertEquals("CANCELLED", repeated.uploadOperation.problem.code)
            assertEquals(1, fixture.count("SELECT count(*) FROM catalog_audit_event WHERE operation_id=? AND action='CANCEL_ARTWORK'", cancel.operation))
            assertEquals(1, fixture.count("SELECT count(*) FROM durable_work WHERE operation_id=? AND kind='ARTWORK_RELEASE'", upload.id))
        }
    }

    @Test
    fun `unknown and hidden original each persist a stable failed cancellation without disclosing upload`() {
        CancellationFixture().use { fixture ->
            val hidden = fixture.begin(fixture.foreignActor())
            listOf(UUID.randomUUID(), hidden.id).forEach { upload ->
                val cancel = Cancellation(upload)
                val result = fixture.cancel(cancel)
                assertFalse(result.cancelled); assertFalse(result.hasUploadOperation())
                assertEquals("FAILED", result.operation.state); assertEquals("NOT_FOUND", result.operation.problem.code)
                assertEquals(result, fixture.cancel(cancel))
                assertEquals(1, fixture.count("SELECT count(*) FROM operation_receipt WHERE id=? AND state='FAILED' AND error_code='NOT_FOUND'", cancel.operation))
                assertEquals(0, fixture.count("SELECT count(*) FROM catalog_audit_event WHERE operation_id=?", cancel.operation))
                assertEquals(0, fixture.count("SELECT count(*) FROM durable_work WHERE operation_id=? AND kind='ARTWORK_RELEASE'", upload))
            }
            assertEquals("PREPARING", fixture.state(hidden).intent)
            assertEquals("PENDING", fixture.state(hidden).receipt)
        }
    }

    @Test
    fun `audit write failure rolls back every cancellation effect then same UUID can retry`() {
        CancellationFixture().use { fixture ->
            val upload = fixture.begin(); val cancel = Cancellation(upload.id)
            fixture.auditFailure(true)
            try {
                assertThrows(StatusRuntimeException::class.java) { fixture.cancel(cancel) }
                assertEquals("PREPARING", fixture.state(upload).intent)
                assertEquals("PENDING", fixture.state(upload).receipt)
                assertEquals(0, fixture.count("SELECT count(*) FROM operation_receipt WHERE id=?", cancel.operation))
                assertEquals(0, fixture.count("SELECT count(*) FROM durable_work WHERE operation_id=? AND kind='ARTWORK_RELEASE'", upload.id))
            } finally { fixture.auditFailure(false) }
            assertTrue(fixture.cancel(cancel).cancelled)
            assertEquals(1, fixture.count("SELECT count(*) FROM catalog_audit_event WHERE operation_id=? AND action='CANCEL_ARTWORK'", cancel.operation))
        }
    }

    @Test
    fun `database barriers preserve both cancellation and attachment winning orders`() {
        listOf(true, false).forEach { cancelFirst -> proveOrder(cancelFirst) }
    }

    private fun proveOrder(cancelFirst: Boolean) {
        CancellationFixture().use { fixture ->
            val upload = fixture.begin(); val cancel = Cancellation(upload.id); val claim = fixture.claim(upload)
            val executor = Executors.newFixedThreadPool(2)
            try {
                val result = fixture.pool.connection.use { gate ->
                    gate.autoCommit = false; ArtworkRpc.lock(gate)
                    val cancelResult = if (cancelFirst) executor.submit<Spoolin.ArtworkCancelOutcome> { fixture.cancel(cancel) } else null
                    val attachResult = if (!cancelFirst) executor.submit { fixture.attach(claim, upload) } else null
                    awaitWaiters(fixture.pool, 1)
                    val secondCancel = if (!cancelFirst) executor.submit<Spoolin.ArtworkCancelOutcome> { fixture.cancel(cancel) } else null
                    val secondAttach = if (cancelFirst) executor.submit { fixture.attach(claim, upload) } else null
                    awaitWaiters(fixture.pool, 2); gate.commit()
                    (attachResult ?: secondAttach)!!.get(10, TimeUnit.SECONDS)
                    (cancelResult ?: secondCancel)!!.get(10, TimeUnit.SECONDS)
                }
                assertEquals(cancelFirst, result.cancelled)
                assertEquals(if (cancelFirst) "FAILED" else "SUCCEEDED", result.uploadOperation.state)
                assertEquals(result, fixture.cancel(cancel))
                val state = fixture.state(upload)
                assertEquals(if (cancelFirst) "ABORTED" else "ATTACHED", state.intent)
                assertEquals(if (cancelFirst) 0L else 1L, state.version)
                if (!cancelFirst) { assertEquals(upload.asset, state.asset); assertEquals(upload.id, state.hold) }
                else { assertEquals(null, state.asset); assertEquals(null, state.hold) }
                assertEquals(if (cancelFirst) 1 else 0, fixture.count(
                    "SELECT count(*) FROM durable_work WHERE operation_id=? AND kind='ARTWORK_RELEASE'", upload.id))
                assertEquals(if (cancelFirst) 1 else 0, fixture.count(
                    "SELECT count(*) FROM catalog_audit_event WHERE operation_id=? AND action='CANCEL_ARTWORK'", cancel.operation))
            } finally { executor.shutdownNow() }
        }
    }
}

private data class Upload(val id: UUID, val title: UUID, val asset: UUID)
private data class Cancellation(val upload: UUID, val operation: UUID = UUID.randomUUID(), val correlation: UUID = UUID.randomUUID())
private data class ArtworkState(val intent: String, val receipt: String, val version: Long, val asset: UUID?, val hold: UUID?)

private class CancellationFixture : AutoCloseable {
    private val proof = SignedReceiptFixture("artwork-cancellation-boundary.json")
    val pool = proof.fixture.owner.pool("catalog")
    private val receipts = OwnerReceipts(pool, IntentHashes(mapOf("v1" to ByteArray(32) { 31 }), "v1"))
    private val handler = ArtworkRpc(pool, receipts) { "policy" }
    val dropNextCancel = AtomicBoolean(false)
    private val service = object : CatalogServiceGrpc.CatalogServiceImplBase() {
        override fun beginArtwork(request: Spoolin.Hold, response: StreamObserver<Spoolin.Outcome>) = handler.beginArtwork(request, response)
        override fun cancelArtwork(request: Spoolin.CancelArtworkRequest, response: StreamObserver<Spoolin.ArtworkCancelOutcome>) =
            handler.cancelArtwork(request, CancelReply(response, dropNextCancel))
    }
    private val transport = ScopedActorTest()
    private val live = CatalogActors(IdentityServiceGrpc.newBlockingStub(proof.identity))
    private val actors = ActorInterceptor("catalog", CatalogActors.protectedMethods,
        ActorVerifier(mapOf("key_A" to proof.pair.public)), live::check)
    private val server = transport.server(proof.root, "catalog", service, com.spoolin.catalog.ServiceIdentityInterceptor(actors))
    private val channel = transport.channel(proof.root, "core", "catalog", server.port)

    fun begin(actor: ActorClaims = proof.claims): Upload {
        val operation = UUID.randomUUID(); val title = UUID.randomUUID(); val correlation = UUID.randomUUID()
        val asset = WorkRequest("asset", "ARTWORK", operation, Instant.MAX).id(operation)
        pool.committed { it.executeBound("INSERT INTO title(id,kind,status,display_title,synopsis,synopsis_language,release_year,version) " +
            "VALUES (?,'MOVIE','ACTIVE','Cancellation fixture','Fixture synopsis','en',2024,0)", title) }
        val request = Spoolin.Hold.newBuilder().setContext(Spoolin.Context.newBuilder().setOperationId(operation.toString())
            .setCorrelationId(correlation.toString()).setPolicyRevision("policy")).setHoldId(operation.toString())
            .setTitleId(title.toString()).setAssetId(asset.toString()).setExpectedVersion(0).build()
        assertEquals("PENDING", client("BeginArtwork", operation, correlation, actor).beginArtwork(request).state)
        return Upload(operation, title, asset)
    }

    fun cancel(command: Cancellation): Spoolin.ArtworkCancelOutcome = client("CancelArtwork", command.operation, command.correlation)
        .cancelArtwork(Spoolin.CancelArtworkRequest.newBuilder().setContext(Spoolin.Context.newBuilder()
            .setOperationId(command.operation.toString()).setCorrelationId(command.correlation.toString()))
            .setUploadOperationId(command.upload.toString()).build())

    private fun client(method: String, operation: UUID, correlation: UUID, actor: ActorClaims = proof.claims): CatalogServiceGrpc.CatalogServiceBlockingStub {
        val now = Instant.now().epochSecond
        val claims = actor.copy(method = "/spoolin.v1.CatalogService/$method", operationId = operation,
            correlationId = correlation, issuedAt = now, expiresAt = now + 30)
        val metadata = Metadata().apply { put(ActorRpc.metadata, proof.signer.sign(claims)) }
        return CatalogServiceGrpc.newBlockingStub(channel).withDeadlineAfter(10, TimeUnit.SECONDS)
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
    }

    fun foreignActor(): ActorClaims {
        val id = UUID.randomUUID()
        proof.fixture.accounts.insert(id, IdentityRules().identity("Other Maintainer", "other@example.test"),
            StoredPasswordHash("unusable"), AccountRole.MAINTAINER)
        val session = proof.fixture.pool.committed { proof.sessions.issue(it, id, 0, null) }
        return proof.claims.copy(accountId = id, credentialId = session.principal.id)
    }

    fun claim(upload: Upload): WorkClaim = pool.committed { connection ->
        val now = Instant.now()
        val candidate = WorkSql.selectForClaim(connection, now, 10, setOf("ARTWORK_ATTACH")).single { it.operationId == upload.id }
        requireNotNull(WorkSql.commitClaim(connection, candidate, UUID.randomUUID(), now))
    }

    // Invokes the post-reserve Catalog transaction directly; this fixture neither executes nor proves Media reserve.
    fun attach(claim: WorkClaim, upload: Upload) {
        val store = ArtworkWorkStore(pool, receipts) { "policy" }
        store.attach(claim, store.read(upload.id))
    }

    fun state(upload: Upload): ArtworkState = pool.connection.use { connection -> connection.queryBound(
        "SELECT a.state AS intent,r.state AS receipt,t.version,t.asset_id,t.attachment_id " +
            "FROM artwork_attachment_request a JOIN operation_receipt r ON r.id=a.id JOIN title t ON t.id=a.title_id WHERE a.id=?",
        listOf(upload.id),
    ) { ArtworkState(it.getString("intent"), it.getString("receipt"), it.getLong("version"),
        it.getObject("asset_id", UUID::class.java), it.getObject("attachment_id", UUID::class.java)) }.single() }

    fun count(sql: String, id: UUID): Int = pool.connection.use { it.queryBound(sql, listOf(id)) { row -> row.getInt(1) }.single() }

    fun auditFailure(enabled: Boolean) {
        proof.fixture.owner.membership("catalog", "grant")
        proof.fixture.owner.pool("catalog", "migrator").use { migration -> migration.connection.use { connection ->
            connection.createStatement().use { statement ->
                if (enabled) {
                    statement.execute("CREATE FUNCTION appdata.reject_cancel_fixture() RETURNS trigger LANGUAGE plpgsql AS " +
                        "\u0024\u0024 BEGIN IF NEW.action='CANCEL_ARTWORK' THEN RAISE EXCEPTION 'Fixture audit unavailable'; END IF; RETURN NEW; END; \u0024\u0024")
                    statement.execute("CREATE TRIGGER reject_cancel_fixture BEFORE INSERT ON catalog_audit_event FOR EACH ROW EXECUTE FUNCTION appdata.reject_cancel_fixture()")
                } else {
                    statement.execute("DROP TRIGGER reject_cancel_fixture ON catalog_audit_event")
                    statement.execute("DROP FUNCTION appdata.reject_cancel_fixture()")
                }
            }
        } }
    }

    override fun close() {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
        server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
        pool.close(); proof.close()
    }
}

private fun awaitWaiters(pool: DataSource, expected: Int) {
    val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
    var waiting: Int
    do {
        waiting = pool.connection.use { connection -> connection.queryBound(
            "SELECT count(*) FROM pg_locks WHERE locktype='advisory' AND NOT granted AND classid=41001 AND objid=1 " +
                "AND database=(SELECT oid FROM pg_database WHERE datname=current_database())",
        ) { it.getInt(1) }.single() }
        if (waiting < expected) Thread.sleep(10)
    } while (waiting < expected && System.nanoTime() < deadline)
    assertEquals(expected, waiting, "Both actual Catalog transactions must reach the advisory-lock barrier")
}

/** Controlled loss occurs after the real owner transaction commits and before its response reaches the client. */
private class CancelReply(private val response: StreamObserver<Spoolin.ArtworkCancelOutcome>,
    private val drop: AtomicBoolean) : StreamObserver<Spoolin.ArtworkCancelOutcome> {
    private var lost = false
    override fun onNext(value: Spoolin.ArtworkCancelOutcome) {
        lost = drop.getAndSet(false)
        if (lost) response.onError(Status.UNAVAILABLE.withDescription("CONTROLLED_RESPONSE_LOSS").asRuntimeException())
        else response.onNext(value)
    }
    override fun onCompleted() { if (!lost) response.onCompleted() }
    override fun onError(failure: Throwable) { if (!lost) response.onError(failure) }
}
