package com.spoolin.core.features.artworkproxy

import io.grpc.Status
import spoolin.v1.Spoolin
import java.util.Locale
import java.util.UUID

private const val MAX_OUTPUT_BYTES = 8_388_608L
private const val MAX_DIMENSION = 8192
private const val MAX_PIXELS = 40_000_000L
private const val CONTENT_HASH_BYTES = 32

internal object ArtworkReadValidation {
    fun grant(grant: Spoolin.ArtworkGrant, assetId: UUID) {
        if (grant.assetId != assetId.toString() || grant.catalogVersion < 0) {
            throw Status.DATA_LOSS.asRuntimeException()
        }
        if (!isUuid(grant.holdId) || !isUuid(grant.titleId)) throw Status.DATA_LOSS.asRuntimeException()
    }

    private fun isUuid(value: String): Boolean = try {
        UUID.fromString(value).toString() == value
    } catch (_: IllegalArgumentException) {
        false
    }

    fun contentType(header: Spoolin.AssetReadHeader): String {
        validateBounds(header)
        return when (header.format.lowercase(Locale.ROOT)) {
            "jpeg", "image/jpeg" -> "image/jpeg"
            "png", "image/png" -> "image/png"
            "webp", "image/webp" -> "image/webp"
            else -> throw Status.DATA_LOSS.asRuntimeException()
        }
    }

    private fun validateBounds(header: Spoolin.AssetReadHeader) {
        if (header.outputBytes !in 1L..MAX_OUTPUT_BYTES || header.contentHash.size() != CONTENT_HASH_BYTES) {
            throw Status.DATA_LOSS.asRuntimeException()
        }
        validateDimensions(header)
    }

    private fun validateDimensions(header: Spoolin.AssetReadHeader) {
        if (header.width !in 1..MAX_DIMENSION || header.height !in 1..MAX_DIMENSION) {
            throw Status.DATA_LOSS.asRuntimeException()
        }
        if (header.width.toLong() * header.height > MAX_PIXELS) throw Status.DATA_LOSS.asRuntimeException()
    }
}
