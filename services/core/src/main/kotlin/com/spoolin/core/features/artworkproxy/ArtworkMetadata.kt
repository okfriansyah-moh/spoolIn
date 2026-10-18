package com.spoolin.core.features.artworkproxy

import java.util.UUID

/** Dimensions originate in the authorized processed Media stream header. */
data class ArtworkMetadata(
    val assetId: UUID,
    val management: Boolean,
    val width: Int,
    val height: Int,
) {
    fun fields(): Map<String, Any> =
        mapOf(
            "assetId" to assetId.toString(),
            "url" to "/artwork/$assetId${if (management) "?view=management" else ""}",
            "width" to width,
            "height" to height,
        )
}
