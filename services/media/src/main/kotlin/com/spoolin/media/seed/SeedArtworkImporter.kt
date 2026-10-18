package com.spoolin.media.seed

import com.spoolin.media.features.asset.AssetState
import com.spoolin.media.features.processing.ImageEnvelope
import com.spoolin.media.features.processing.ImageValidator
import com.spoolin.media.features.processing.ProcessingBounds
import com.spoolin.media.features.processing.ProcessorRunner
import com.spoolin.media.features.upload.QuarantineStore
import com.spoolin.support.receipts.committed
import com.spoolin.support.receipts.queryBound
import java.nio.file.Files
import java.nio.file.Path
import javax.sql.DataSource

internal class SeedArtworkImporter(
    private val database: DataSource,
    private val store: QuarantineStore,
    private val processor: ProcessorRunner = ProcessorRunner(),
) {
    fun prepare(input: SeedArtworkInput): Map<String, Any?> =
        locked {
            val assets = input.posters.map { poster -> preparePoster(input, poster) }
            mapOf("manifestVersion" to input.version, "snapshotSHA256" to input.snapshot, "assets" to assets)
        }

    fun finish(
        input: SeedArtworkInput,
        path: Path,
    ): Map<String, Any?> =
        locked {
            val receipt = SeedJson.mapper.readTree(path.toFile())
            require(receipt["manifestVersion"].asText() == input.version)
            require(receipt["snapshotSHA256"].asText() == input.snapshot)
            val rows = receipt["attachments"].toList()
            require(rows.size == input.posters.size && rows.map { it["titleId"].asText() }.distinct().size == rows.size)
            input.posters.forEach { poster ->
                val row = rows.single { it["titleId"].asText() == poster.title.toString() }
                database.committed { SeedArtworkStore.finalize(it, input, poster, row) }
            }
            mapOf("manifestVersion" to input.version, "finalized" to rows.count { it["state"].asText() == "ATTACHED" })
        }

    private fun preparePoster(
        input: SeedArtworkInput,
        poster: SeedPoster,
    ): Map<String, Any?> {
        val file = input.file(poster)
        require(Files.size(file) in 1..ProcessingBounds.MAX_INPUT_BYTES)
        val valid = Files.newInputStream(file).use { ImageValidator.validate(it, ProcessingBounds.MAX_INPUT_BYTES) }
        require(valid is ImageValidator.ValidationResult.Valid && SeedJson.hex(valid.inputHash) == poster.hash)
        require(ImageEnvelope.complete(Files.readAllBytes(file), valid.format))
        val old = database.connection.use { SeedArtworkStore.existing(it, poster) }
        if (old == null) {
            val processed = processor.process(file, store.outputDirectory(), valid.format, poster.asset)
            check(processed is ProcessorRunner.Outcome.Success) { "Seed image processing failed: $processed" }
            database.committed { SeedArtworkStore.insert(it, input, poster, processed, Files.size(file)) }
        } else {
            require(old.actorId == input.actor)
            require(SeedJson.mapper.readTree(old.provenance)["sha256"].asText() == poster.hash)
            if (old.state == AssetState.READY) verifyOutput(old, file, valid)
        }
        return projection(poster)
    }

    private fun verifyOutput(
        old: com.spoolin.media.features.asset.AssetRow,
        file: Path,
        valid: ImageValidator.ValidationResult.Valid,
    ) {
        val output = processor.process(file, store.outputDirectory(), valid.format, old.id)
        check(output is ProcessorRunner.Outcome.Success)
        check(java.security.MessageDigest.isEqual(old.contentHash, output.contentHash)) {
            "Seed output checksum differs"
        }
        check(old.storageKey == "output/${output.outputPath.fileName}" && old.outputBytes == output.outputBytes)
    }

    private fun projection(poster: SeedPoster): Map<String, Any?> =
        database.connection.use { connection ->
            val asset = requireNotNull(SeedArtworkStore.existing(connection, poster))
            val state = requireNotNull(SeedArtworkStore.reference(connection, poster))
            mapOf(
                "titleId" to poster.title.toString(),
                "assetId" to poster.asset.toString(),
                "holdId" to poster.hold.toString(),
                "width" to (asset.width ?: poster.artwork["width"].asInt()),
                "height" to (asset.height ?: poster.artwork["height"].asInt()),
                "inputHash" to poster.hash,
                "contentHash" to asset.contentHash?.let(SeedJson::hex),
                "referenceState" to state,
                "available" to (asset.state == AssetState.READY && state != "RELEASED"),
            )
        }

    private fun <T> locked(action: () -> T): T =
        database.connection.use { connection ->
            connection.queryBound("SELECT pg_advisory_lock(41003,1)", emptyList()) { true }
            try {
                action()
            } finally {
                connection.queryBound("SELECT pg_advisory_unlock(41003,1)", emptyList()) { true }
            }
        }
}
