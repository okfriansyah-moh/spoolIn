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
                val title = createTitle(pool)
                withTransport(proof, pool) { channel -> verifyBegin(proof, pool, channel, title) }
            }
        }
    }

    private fun createTitle(pool: javax.sql.DataSource): UUID {
        val title = UUID.randomUUID()
        pool.committed {
            it.executeBound(
                "INSERT INTO title(id,kind,status,display_title,synopsis,synopsis_language,release_year,version) " +
                    "VALUES(?,'MOVIE','ACTIVE','Artwork fixture','Original fixture synopsis','en',2024,0)",
                title,
            )
        }
        return title
    }

    private fun withTransport(
        proof: SignedReceiptFixture,
        pool: javax.sql.DataSource,
        verify: (io.grpc.ManagedChannel) -> Unit,
    ) {
        val receipts = OwnerReceipts(pool, IntentHashes(mapOf("v1" to ByteArray(32) { 31 }), "v1"))
        val handler = ArtworkRpc(pool, receipts) { "policy" }
        val live = CatalogActors(IdentityServiceGrpc.newBlockingStub(proof.identity))
        val actors =
            ActorInterceptor(
                "catalog",
                CatalogActors.protectedMethods,
                ActorVerifier(mapOf("key_A" to proof.pair.public)),
                live::check,
            )
        val transport = ScopedActorTest()
        val server =
            transport.server(
                proof.root,
                "catalog",
                rpc(handler),
                com.spoolin.catalog.ServiceIdentityInterceptor(actors),
            )
        val channel = transport.channel(proof.root, "core", "catalog", server.port)
        try {
            verify(channel)
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
            server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
        }
    }

    private fun rpc(handler: ArtworkRpc): CatalogServiceGrpc.CatalogServiceImplBase =
        object : CatalogServiceGrpc.CatalogServiceImplBase() {
            override fun beginArtwork(
                request: Spoolin.Hold,
                response: StreamObserver<Spoolin.Outcome>,
            ) = handler.beginArtwork(request, response)

            override fun cancelArtwork(
                request: Spoolin.CancelArtworkRequest,
                response: StreamObserver<Spoolin.ArtworkCancelOutcome>,
            ) = handler.cancelArtwork(request, response)
        }

    private fun verifyBegin(
        proof: SignedReceiptFixture,
        pool: javax.sql.DataSource,
        channel: io.grpc.ManagedChannel,
        title: UUID,
    ) {
        val operation = UUID.randomUUID()
        val correlation = UUID.randomUUID()
        val request =
            Spoolin.Hold
                .newBuilder()
                .setContext(context(operation, correlation).setPolicyRevision("policy"))
                .setHoldId(operation.toString())
                .setTitleId(title.toString())
                .setAssetId(UUID.randomUUID().toString())
                .setExpectedVersion(0)
                .build()
        val stub = client(proof, channel, "BeginArtwork", operation, correlation)
        assertEquals("PENDING", stub.beginArtwork(request).state)
        assertEquals("PENDING", stub.beginArtwork(request).state)
        verifyStored(pool, title, operation)
        assertThrows(StatusRuntimeException::class.java) {
            stub.beginArtwork(request.toBuilder().setExpectedVersion(1).build())
        }
        verifyCancellation(proof, channel, operation)
    }

    private fun verifyStored(
        pool: javax.sql.DataSource,
        title: UUID,
        operation: UUID,
    ) {
        pool.connection.use { connection ->
            assertEquals(
                0L,
                connection
                    .queryBound("SELECT version FROM title WHERE id=?", listOf(title)) {
                        it.getLong("version")
                    }.single(),
            )
            assertEquals(
                "PREPARING",
                connection
                    .queryBound("SELECT state FROM artwork_attachment_request WHERE id=?", listOf(operation)) {
                        it.getString("state")
                    }.single(),
            )
            assertEquals(
                1,
                connection
                    .queryBound(
                        "SELECT count(*) AS total FROM durable_work WHERE operation_id=? AND kind='ARTWORK_ATTACH'",
                        listOf(operation),
                    ) { it.getInt("total") }
                    .single(),
            )
        }
    }

    private fun verifyCancellation(
        proof: SignedReceiptFixture,
        channel: io.grpc.ManagedChannel,
        operation: UUID,
    ) {
        val cancellation = UUID.randomUUID()
        val correlation = UUID.randomUUID()
        val cancel =
            Spoolin.CancelArtworkRequest
                .newBuilder()
                .setContext(context(cancellation, correlation))
                .setUploadOperationId(operation.toString())
                .build()
        val result = client(proof, channel, "CancelArtwork", cancellation, correlation).cancelArtwork(cancel)
        assertEquals(true, result.cancelled)
        assertEquals("FAILED", result.uploadOperation.state)
        assertEquals("CANCELLED", result.uploadOperation.problem.code)
        assertEquals(
            true,
            client(proof, channel, "CancelArtwork", cancellation, correlation).cancelArtwork(cancel).cancelled,
        )
    }

    private fun context(
        operation: UUID,
        correlation: UUID,
    ): Spoolin.Context.Builder =
        Spoolin.Context
            .newBuilder()
            .setOperationId(operation.toString())
            .setCorrelationId(correlation.toString())

    private fun client(
        proof: SignedReceiptFixture,
        channel: io.grpc.ManagedChannel,
        method: String,
        operation: UUID,
        correlation: UUID,
    ): CatalogServiceGrpc.CatalogServiceBlockingStub {
        val claims =
            proof.claims.copy(
                method = "/spoolin.v1.CatalogService/$method",
                operationId = operation,
                correlationId = correlation,
            )
        val metadata = Metadata().apply { put(ActorRpc.metadata, proof.signer.sign(claims)) }
        return CatalogServiceGrpc
            .newBlockingStub(channel)
            .withDeadlineAfter(5, TimeUnit.SECONDS)
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
    }
}
