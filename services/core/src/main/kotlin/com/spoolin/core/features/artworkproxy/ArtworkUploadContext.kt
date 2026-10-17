package com.spoolin.core.features.artworkproxy

import com.spoolin.core.boundary.http.BrowserContext
import com.spoolin.core.features.session.SessionPrincipal
import spoolin.v1.Spoolin
import java.util.UUID

internal data class ArtworkUploadContext(
    val browser: BrowserContext,
    val principal: SessionPrincipal,
    val uploadOperationId: UUID,
    val policyRevision: String,
    val assetId: UUID,
    val content: ByteArray,
)

internal data class ArtworkAttachContext(
    val browser: BrowserContext,
    val principal: SessionPrincipal,
    val uploadOperationId: UUID,
    val titleId: String,
    val expectedVersion: Long,
    val policyRevision: String,
    val assetId: UUID,
    val uploadOutcome: Spoolin.Outcome,
)
