package com.spoolin.integration.catalog.people

import com.spoolin.integration.identity.SignedReceiptFixture
import com.spoolin.support.identity.ActorRpc
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.Spoolin.Id
import java.util.concurrent.TimeUnit

/** Cross-service person write boundary: signed Core actor to catalog receipt RPC. */
class PeopleWritesBoundaryTest {
    @Test
    fun `signed actor reaches catalog receipt owner for person writes`() {
        SignedReceiptFixture("t009-people-write-boundary.json").use { proof ->
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
