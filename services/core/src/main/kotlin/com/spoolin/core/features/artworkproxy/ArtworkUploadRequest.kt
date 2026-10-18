package com.spoolin.core.features.artworkproxy

import com.spoolin.core.features.session.SessionPrincipal
import spoolin.v1.MediaServiceGrpc
import java.util.UUID

internal data class ArtworkUploadRequest(
    val media: MediaServiceGrpc.MediaServiceStub,
    val actorToken: String,
    val principal: SessionPrincipal,
    val uploadOperationId: UUID,
    val correlationId: UUID,
    val policyRevision: String,
    val assetId: UUID,
    val intentId: UUID,
    val spool: ArtworkSpool,
)
