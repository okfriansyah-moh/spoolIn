package com.spoolin.media.features.asset

import io.grpc.Status
import io.grpc.StatusRuntimeException
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.Spoolin.Attachment
import spoolin.v1.Spoolin.Id
import java.util.UUID
import java.util.concurrent.TimeUnit

/** The channel factory supplies Media's mTLS identity; Core actor tokens are never forwarded. */
class CatalogAttachments(private val catalog: CatalogServiceGrpc.CatalogServiceBlockingStub) {
    fun find(hold: UUID): Attachment = try {
        catalog.withDeadlineAfter(2, TimeUnit.SECONDS).getAttachment(Id.newBuilder().setId(hold.toString()).build())
    } catch (failure: StatusRuntimeException) {
        if (failure.status.code == Status.Code.CANCELLED) throw Status.CANCELLED.asRuntimeException()
        if (failure.status.code == Status.Code.NOT_FOUND) {
            throw Status.NOT_FOUND.asRuntimeException()
        }
        throw Status.UNAVAILABLE.withDescription("ATTACHMENT_AUTHORITY_UNAVAILABLE").asRuntimeException()
    }
}
