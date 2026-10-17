package com.spoolin.integration.artwork

import com.spoolin.catalog.features.artwork.ArtworkAttachmentSql
import com.spoolin.catalog.features.artwork.ArtworkAttachmentState
import com.spoolin.integration.identity.SignedReceiptFixture
import com.spoolin.media.features.cleanup.CleanupRules
import com.spoolin.media.features.hold.ReferenceState
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.receipts.committed
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.Spoolin.Id
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Cross-owner artwork attachment rule checks across catalog and media owners (T023). */
class ArtworkAttachmentRulesTest {
    @Test
    fun `preparing artwork can attach or abort but not regress from attached`() {
        assertTrue(ArtworkAttachmentState.PREPARING.canTransitionTo(ArtworkAttachmentState.ATTACHED))
        assertTrue(ArtworkAttachmentState.PREPARING.canTransitionTo(ArtworkAttachmentState.ABORTED))
        assertFalse(ArtworkAttachmentState.ATTACHED.canTransitionTo(ArtworkAttachmentState.ABORTED))
    }

    @Test
    fun `media hold release and cleanup respect active references`() {
        assertTrue(ReferenceState.ACTIVE.canTransitionTo(ReferenceState.RELEASED))
        assertTrue(CleanupRules.blocksDeletion(1))
        assertFalse(CleanupRules.blocksDeletion(0))
    }

    @Test
    fun `catalog attachment sql respects receipt id and terminal states across media hold rules`() {
        SignedReceiptFixture("t009-artwork-attachment-boundary.json").use { proof ->
            val token = proof.signer.sign(proof.claims)
            val request = Id.newBuilder().setId(proof.operation.toString()).build()
            val metadata = Metadata().apply { put(ActorRpc.metadata, token) }
            val stub =
                CatalogServiceGrpc
                    .newBlockingStub(proof.caller)
                    .withDeadlineAfter(3, TimeUnit.SECONDS)
                    .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
            assertEquals("SUCCEEDED", stub.getOperation(request).state)

            val titleId = UUID.randomUUID()
            val assetId = UUID.randomUUID()
            proof.fixture.owner.pool("catalog").committed { connection ->
                assertEquals(
                    1,
                    ArtworkAttachmentSql.insertPreparing(
                        connection,
                        proof.operation,
                        titleId,
                        proof.account,
                        3L,
                        assetId,
                        "policy-revision-fixture",
                        null,
                    ),
                )
                assertEquals(1, ArtworkAttachmentSql.markAttached(connection, proof.operation))
                assertEquals(0, ArtworkAttachmentSql.markAborted(connection, proof.operation))
            }
            assertFalse(ArtworkAttachmentState.ATTACHED.canTransitionTo(ArtworkAttachmentState.ABORTED))
            assertTrue(ReferenceState.RESERVED.canTransitionTo(ReferenceState.ACTIVE))
        }
    }
}
