package com.spoolin.core.boundary.grpc

import com.spoolin.core.features.ratelimit.RateUnavailable
import com.spoolin.core.features.session.SessionPrincipal
import com.spoolin.support.identity.ActorRpc
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import spoolin.v1.CatalogServiceGrpc
import java.util.UUID
import java.util.concurrent.TimeUnit

internal class CatalogActorRpc(
    private val catalog: CatalogServiceGrpc.CatalogServiceBlockingStub,
    private val issuer: ActorIssuer?,
) {
    fun issue(
        principal: SessionPrincipal,
        method: String,
        operationId: UUID,
        correlation: UUID,
    ): String {
        val signer = issuer ?: throw RateUnavailable()
        return signer.issue(principal, ActorTarget("catalog", method, operationId, correlation))
    }

    fun stub(
        token: String,
        deadlineSeconds: Long = 5L,
    ): CatalogServiceGrpc.CatalogServiceBlockingStub {
        val meta = Metadata().apply { put(ActorRpc.metadata, token) }
        return catalog
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(meta))
            .withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS)
    }
}
