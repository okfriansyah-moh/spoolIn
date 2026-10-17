package com.spoolin.integration.lifecycle

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
 * Lifecycle mutations are receipt-gated on catalog; this exercises the live cross-service receipt read
 * used before archive/restore/delete outcomes are applied (T024).
 */
class LifecycleReceiptIntegrationTest {
    @Test
    fun `lifecycle outcomes use catalog receipt rpc under signed actor admission`() {
        SignedReceiptFixture("t009-lifecycle-receipt-boundary.json").use { proof ->
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
