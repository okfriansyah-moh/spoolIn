package com.spoolin.integration.catalog.reads

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
 * Cross-service catalog read integration: receipt operation state is readable under scoped admission.
 */
class CatalogReadsBoundaryTest {
    @Test
    fun `catalog operation read uses live identity and receipt owner`() {
        SignedReceiptFixture("t009-catalog-read-boundary.json").use { proof ->
            val token = proof.signer.sign(proof.claims)
            val request = Id.newBuilder().setId(proof.operation.toString()).build()
            val metadata = Metadata().apply { put(ActorRpc.metadata, token) }
            val stub =
                CatalogServiceGrpc
                    .newBlockingStub(proof.caller)
                    .withDeadlineAfter(3, TimeUnit.SECONDS)
                    .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
            val operation = stub.getOperation(request)
            assertEquals("SUCCEEDED", operation.state)
            assertEquals(7, operation.version)
        }
    }
}
