package com.spoolin.integration.artwork

import com.spoolin.catalog.boundary.grpc.CatalogActors
import com.spoolin.catalog.features.artwork.ArtworkRpc
import com.spoolin.catalog.operations.receipts.OwnerReceipts
import com.spoolin.integration.identity.ScopedActorTest
import com.spoolin.integration.identity.SignedReceiptFixture
import com.spoolin.support.identity.ActorInterceptor
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.identity.ActorVerifier
import com.spoolin.support.receipts.IntentHashes
import com.spoolin.support.receipts.committed
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import io.grpc.Metadata
import io.grpc.StatusRuntimeException
import io.grpc.stub.MetadataUtils
import io.grpc.stub.StreamObserver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.IdentityServiceGrpc
import spoolin.v1.Spoolin
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Real PostgreSQL and signed mTLS Catalog admission; complete Media/browser proof stays separate. */
class ArtworkFlowBoundaryTest {
    @Test
    fun `begin commits pending intent and recovery work while preserving prior title version`() {
        SignedReceiptFixture("artwork-flow-boundary.json").use { proof ->
            proof.fixture.owner.pool("catalog").use { pool ->
                val title = UUID.randomUUID()
                pool.committed { it.executeBound("INSERT INTO title(id,kind,status,display_title,synopsis,synopsis_language,release_year,version) VALUES(?,'MOVIE','ACTIVE','Artwork fixture','Original fixture synopsis','en',2024,0)", title) }
                val handler = ArtworkRpc(pool, OwnerReceipts(pool, IntentHashes(mapOf("v1" to ByteArray(32) { 31 }), "v1"))) { "policy" }
                val rpc = object : CatalogServiceGrpc.CatalogServiceImplBase() {
                    override fun beginArtwork(request: Spoolin.Hold, response: StreamObserver<Spoolin.Outcome>) = handler.beginArtwork(request, response)
                    override fun cancelArtwork(request: Spoolin.CancelArtworkRequest, response: StreamObserver<Spoolin.ArtworkCancelOutcome>) = handler.cancelArtwork(request, response)
                }
                val live = CatalogActors(IdentityServiceGrpc.newBlockingStub(proof.identity))
                val actors = ActorInterceptor("catalog", CatalogActors.protectedMethods, ActorVerifier(mapOf("key_A" to proof.pair.public)), live::check)
                val transport = ScopedActorTest()
                val server = transport.server(proof.root, "catalog", rpc, com.spoolin.catalog.ServiceIdentityInterceptor(actors))
                val channel = transport.channel(proof.root, "core", "catalog", server.port)
                try {
                    val operation = UUID.randomUUID()
                    val correlation = UUID.randomUUID()
                    val request = Spoolin.Hold.newBuilder().setContext(Spoolin.Context.newBuilder().setOperationId(operation.toString()).setCorrelationId(correlation.toString()).setPolicyRevision("policy"))
                        .setHoldId(operation.toString()).setTitleId(title.toString()).setAssetId(UUID.randomUUID().toString()).setExpectedVersion(0).build()
                    fun client(method: String, id: UUID, correlationId: UUID): CatalogServiceGrpc.CatalogServiceBlockingStub {
                        val claims = proof.claims.copy(method = "/spoolin.v1.CatalogService/$method", operationId = id, correlationId = correlationId)
                        val metadata = Metadata().apply { put(ActorRpc.metadata, proof.signer.sign(claims)) }
                        return CatalogServiceGrpc.newBlockingStub(channel).withDeadlineAfter(5, TimeUnit.SECONDS).withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                    }
                    val stub = client("BeginArtwork", operation, correlation)
                    assertEquals("PENDING", stub.beginArtwork(request).state)
                    assertEquals("PENDING", stub.beginArtwork(request).state)
                    pool.connection.use { connection ->
                        assertEquals(0L, connection.queryBound("SELECT version FROM title WHERE id=?", listOf(title)) { it.getLong("version") }.single())
                        assertEquals("PREPARING", connection.queryBound("SELECT state FROM artwork_attachment_request WHERE id=?", listOf(operation)) { it.getString("state") }.single())
                        assertEquals(1, connection.queryBound("SELECT count(*) AS total FROM durable_work WHERE operation_id=? AND kind='ARTWORK_ATTACH'", listOf(operation)) { it.getInt("total") }.single())
                    }
                    assertThrows(StatusRuntimeException::class.java) { stub.beginArtwork(request.toBuilder().setExpectedVersion(1).build()) }
                    val cancellation = UUID.randomUUID()
                    val cancelCorrelation = UUID.randomUUID()
                    val cancel = Spoolin.CancelArtworkRequest.newBuilder().setContext(Spoolin.Context.newBuilder().setOperationId(cancellation.toString()).setCorrelationId(cancelCorrelation.toString()))
                        .setUploadOperationId(operation.toString()).build()
                    val result = client("CancelArtwork", cancellation, cancelCorrelation).cancelArtwork(cancel)
                    assertEquals(true, result.cancelled)
                    assertEquals("FAILED", result.uploadOperation.state)
                    assertEquals("CANCELLED", result.uploadOperation.problem.code)
                    assertEquals(true, client("CancelArtwork", cancellation, cancelCorrelation).cancelArtwork(cancel).cancelled)
                } finally {
                    channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
                    server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
                }
            }
        }
    }
}
