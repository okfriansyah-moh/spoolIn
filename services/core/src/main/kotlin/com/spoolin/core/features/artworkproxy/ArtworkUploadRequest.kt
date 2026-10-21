package com.spoolin.core.features.artworkproxy

import com.spoolin.core.features.session.SessionPrincipal
import spoolin.v1.MediaServiceGrpc
import java.util.UUID

internal data class ArtworkUploadRequest(
    val media: MediaServiceGrpc.MediaServiceStub,
    val actorToken: String,
    val actorId: UUID,
    val uploadOperationId: UUID,
    val correlationId: UUID,
    val policyRevision: String,
    val assetId: UUID,
    val intentId: UUID,
    val spool: ArtworkSpool,
) {
    constructor(
        media: MediaServiceGrpc.MediaServiceStub,
        actorToken: String,
        principal: SessionPrincipal,
        uploadOperationId: UUID,
        correlationId: UUID,
        policyRevision: String,
        assetId: UUID,
        intentId: UUID,
        spool: ArtworkSpool,
    ) : this(media, actorToken, principal.accountId, uploadOperationId, correlationId, policyRevision, assetId, intentId, spool)
}
