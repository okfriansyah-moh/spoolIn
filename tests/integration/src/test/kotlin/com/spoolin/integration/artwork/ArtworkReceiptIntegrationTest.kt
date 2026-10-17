package com.spoolin.integration.artwork

import com.spoolin.integration.identity.SignedReceiptFixture
import com.spoolin.support.identity.ActorRpc
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.Spoolin.Id
import java.util.concurrent.TimeUnit

/**
 * Artwork attachment coordinates catalog receipts with media processing (T023); receipt admission is
 * validated on the catalog service boundary before attachment state advances.
 */
class ArtworkReceiptIntegrationTest {

    @Test
    fun `artwork attachment path shares catalog receipt admission with media coordination`() {
        SignedReceiptFixture("t009-artwork-receipt-boundary.json").use { proof ->
            val token = proof.signer.sign(proof.claims)
            val request = Id.newBuilder().setId(proof.operation.toString()).build()
            val metadata = Metadata().apply { put(ActorRpc.metadata, token) }
            val stub =
                CatalogServiceGrpc
                    .newBlockingStub(proof.caller)
                    .withDeadlineAfter(3, TimeUnit.SECONDS)
                    .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
            assertEquals("SUCCEEDED", stub.getOperation(request).state)
        }
    }
}
