package com.spoolin.media.features.processing

import com.spoolin.media.features.asset.AssetRow
import com.spoolin.media.features.asset.AssetSql
import com.spoolin.media.features.asset.AssetState
import com.spoolin.media.features.upload.QuarantineStore
import com.spoolin.support.receipts.committed
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import com.spoolin.support.work.AttemptOutcome
import com.spoolin.support.work.WorkAttemptCompletion
import com.spoolin.support.work.WorkClaim
import com.spoolin.support.work.WorkSql
import java.nio.file.Files
import java.sql.Connection
import java.time.Clock
import java.util.UUID
import javax.sql.DataSource

/** Slow codec work happens outside transactions; the terminal owner mutation shares the work fence. */
class MediaProcessingHandler(
    private val dataSource: DataSource,
    private val store: QuarantineStore,
    private val processor: ProcessorRunner = ProcessorRunner(),
    private val clock: Clock = Clock.systemUTC(),
) {
    fun handle(claim: WorkClaim) {
        require(claim.kind == "MEDIA_PROCESS")
        val asset = dataSource.connection.use { connection ->
            connection.queryBound("SELECT * FROM asset WHERE processing_work_id=?", listOf(claim.workId)) {
                AssetRow.decode(it)
            }.single()
        }
        check(asset.uploadOperationId == claim.operationId && asset.state == AssetState.PROCESSING)
        val outcome = process(asset)
        val committed = finish(claim, asset, outcome)
        // Only the winning terminal transaction may discard accepted input.
        if (committed) Files.deleteIfExists(store.stagingPath(asset.id))
    }

    private fun process(asset: AssetRow): ProcessorRunner.Outcome {
        if (asset.processingStagingKey != "quarantine/${asset.id}") {
            return ProcessorRunner.Outcome.Rejected("PROCESSING_IDENTITY_MISMATCH")
        }
        val input = store.stagingPath(asset.id)
        if (!Files.isRegularFile(input, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            return ProcessorRunner.Outcome.Rejected("ACCEPTED_INPUT_MISSING")
        }
        if (Files.size(input) != asset.inputBytes) return ProcessorRunner.Outcome.Rejected("INPUT_SIZE_MISMATCH")
        val validation = Files.newInputStream(input).use {
            ImageValidator.validate(it, ProcessingBounds.MAX_INPUT_BYTES)
        }
        if (validation is ImageValidator.ValidationResult.Invalid) {
            return ProcessorRunner.Outcome.Rejected(validation.errorCode)
        }
        validation as ImageValidator.ValidationResult.Valid
        if (!java.security.MessageDigest.isEqual(validation.inputHash, asset.processingInputHash)) {
            return ProcessorRunner.Outcome.Rejected("INPUT_CHECKSUM_MISMATCH")
        }
        if (!ImageEnvelope.complete(Files.readAllBytes(input), validation.format)) {
            return ProcessorRunner.Outcome.Rejected("INVALID_IMAGE_ENVELOPE")
        }
        return processor.process(input, store.outputDirectory(), validation.format, asset.id)
    }

    private fun finish(claim: WorkClaim, asset: AssetRow, outcome: ProcessorRunner.Outcome): Boolean =
        dataSource.committed { connection ->
            val success = outcome is ProcessorRunner.Outcome.Success
            val error = (outcome as? ProcessorRunner.Outcome.Rejected)?.errorCode
            val completion = WorkAttemptCompletion(
                if (success) AttemptOutcome.SUCCEEDED else AttemptOutcome.PERMANENT,
                error, if (success) "DONE" else "EXHAUSTED",
            )
            // completeAttempt locks and validates the live generation/attempt tuple before any asset write.
            if (!WorkSql.completeAttempt(connection, claim, completion, clock.instant())) return@committed false
            val current = AssetSql.findLocked(connection, asset.id)
            check(current?.processingWorkId == claim.workId && current.state == AssetState.PROCESSING)
            when (outcome) {
                is ProcessorRunner.Outcome.Success -> completeReady(connection, asset.id, outcome)
                is ProcessorRunner.Outcome.Rejected -> check(AssetSql.markRejected(connection, asset.id, error!!) == 1)
            }
            check(connection.executeBound(
                "UPDATE operation_receipt SET state=?,result=?::jsonb,error_code=? WHERE id=? AND state='PENDING'",
                if (success) "SUCCEEDED" else "FAILED",
                if (success) "{\"resultId\":\"${asset.id}\"}" else null,
                if (success) null else "FIELD_INVALID", asset.uploadOperationId,
            ) == 1)
            true
        }

    private fun completeReady(connection: Connection, assetId: UUID, result: ProcessorRunner.Outcome.Success) {
        check(AssetSql.markReady(
            connection, assetId, "output/${result.outputPath.fileName}", result.contentHash,
            result.format.label, result.outputBytes, result.width, result.height,
        ) == 1)
    }
}
