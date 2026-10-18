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
    private val metrics: ProcessingMetrics? = null,
) {
    fun handle(claim: WorkClaim) {
        require(claim.kind == "MEDIA_PROCESS")
        val asset =
            dataSource.connection.use { connection ->
                connection
                    .queryBound("SELECT * FROM asset WHERE processing_work_id=?", listOf(claim.workId)) {
                        AssetRow.decode(it)
                    }.single()
            }
        check(asset.uploadOperationId == claim.operationId && asset.state == AssetState.PROCESSING)
        val outcome = process(asset)
        val committed = finish(claim, asset, outcome.result)
        if (committed && outcome.processorRan) metrics?.committed(outcome.result)
        // Only the winning terminal transaction may discard accepted input.
        if (committed) Files.deleteIfExists(store.stagingPath(asset.id))
    }

    private fun process(asset: AssetRow): Processed {
        val input = store.stagingPath(asset.id)
        val error = inputError(asset, input)
        return if (error != null) Processed(ProcessorRunner.Outcome.Rejected(error), false) else validate(asset, input)
    }

    private fun inputError(
        asset: AssetRow,
        input: java.nio.file.Path,
    ): String? =
        when {
            asset.processingStagingKey != "quarantine/${asset.id}" -> "PROCESSING_IDENTITY_MISMATCH"
            !Files.isRegularFile(input, java.nio.file.LinkOption.NOFOLLOW_LINKS) -> "ACCEPTED_INPUT_MISSING"
            Files.size(input) != asset.inputBytes -> "INPUT_SIZE_MISMATCH"
            else -> null
        }

    private fun validate(
        asset: AssetRow,
        input: java.nio.file.Path,
    ): Processed {
        val validation =
            Files.newInputStream(input).use {
                ImageValidator.validate(it, ProcessingBounds.MAX_INPUT_BYTES)
            }
        return when (validation) {
            is ImageValidator.ValidationResult.Invalid -> Processed(ProcessorRunner.Outcome.Rejected(validation.errorCode), false)
            is ImageValidator.ValidationResult.Valid -> processValidated(asset, input, validation)
        }
    }

    private fun processValidated(
        asset: AssetRow,
        input: java.nio.file.Path,
        validation: ImageValidator.ValidationResult.Valid,
    ): Processed {
        val error =
            when {
                !java.security.MessageDigest.isEqual(validation.inputHash, asset.processingInputHash) ->
                    "INPUT_CHECKSUM_MISMATCH"
                !ImageEnvelope.complete(Files.readAllBytes(input), validation.format) -> "INVALID_IMAGE_ENVELOPE"
                else -> null
            }
        return if (error != null) {
            Processed(ProcessorRunner.Outcome.Rejected(error), false)
        } else {
            val action = { processor.process(input, store.outputDirectory(), validation.format, asset.id) }
            Processed(metrics?.measure(action) ?: action(), true)
        }
    }

    private fun finish(
        claim: WorkClaim,
        asset: AssetRow,
        outcome: ProcessorRunner.Outcome,
    ): Boolean =
        dataSource.committed { connection ->
            val completion = completion(outcome)
            // completeAttempt locks and validates the live generation/attempt tuple before any asset write.
            if (!WorkSql.completeAttempt(connection, claim, completion, clock.instant())) return@committed false
            val current = AssetSql.findLocked(connection, asset.id)
            check(current?.processingWorkId == claim.workId && current.state == AssetState.PROCESSING)
            when (outcome) {
                is ProcessorRunner.Outcome.Success -> completeReady(connection, asset.id, outcome)
                is ProcessorRunner.Outcome.Rejected ->
                    check(AssetSql.markRejected(connection, asset.id, outcome.errorCode) == 1)
            }
            completeReceipt(connection, asset, outcome is ProcessorRunner.Outcome.Success)
            true
        }

    private fun completion(outcome: ProcessorRunner.Outcome): WorkAttemptCompletion =
        when (outcome) {
            is ProcessorRunner.Outcome.Success -> WorkAttemptCompletion(AttemptOutcome.SUCCEEDED, null, "DONE")
            is ProcessorRunner.Outcome.Rejected ->
                WorkAttemptCompletion(AttemptOutcome.PERMANENT, outcome.errorCode, "EXHAUSTED")
        }

    private fun completeReceipt(
        connection: Connection,
        asset: AssetRow,
        success: Boolean,
    ) {
        check(
            connection.executeBound(
                "UPDATE operation_receipt SET state=?,result=?::jsonb,error_code=? WHERE id=? AND state='PENDING'",
                if (success) "SUCCEEDED" else "FAILED",
                if (success) "{\"resultId\":\"${asset.id}\"}" else null,
                if (success) null else "FIELD_INVALID",
                asset.uploadOperationId,
            ) == 1,
        )
    }

    private fun completeReady(
        connection: Connection,
        assetId: UUID,
        result: ProcessorRunner.Outcome.Success,
    ) {
        check(
            AssetSql.markReady(
                connection,
                assetId,
                "output/${result.outputPath.fileName}",
                result.contentHash,
                result.format.label,
                result.outputBytes,
                result.width,
                result.height,
            ) == 1,
        )
    }
}

private data class Processed(
    val result: ProcessorRunner.Outcome,
    val processorRan: Boolean,
)
