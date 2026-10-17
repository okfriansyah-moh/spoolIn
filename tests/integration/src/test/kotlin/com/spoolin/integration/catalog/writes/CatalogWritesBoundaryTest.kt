package com.spoolin.integration.catalog.writes

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
 * Cross-service catalog write integration: signed Core actor through mTLS to catalog receipt RPC.
 */
class CatalogWritesBoundaryTest {

    @Test
    fun `signed actor receipt read succeeds across core and catalog`() {
        SignedReceiptFixture("t009-catalog-write-boundary.json").use { proof ->
            val token = proof.signer.sign(proof.claims)
            val request = Id.newBuilder().setId(proof.operation.toString()).build()
            val metadata = Metadata().apply { put(ActorRpc.metadata, token) }
            val stub =
                CatalogServiceGrpc
                    .newBlockingStub(proof.caller)
                    .withDeadlineAfter(3, TimeUnit.SECONDS)
                    .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
            assertEquals("SUCCEEDED", stub.getOperation(request).state)
            assertEquals(1, proof.lookups.get())
        }
    }
}
