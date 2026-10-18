package com.spoolin.media.seed

import com.spoolin.media.features.asset.AssetRow
import com.spoolin.media.features.processing.ProcessorRunner
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import java.sql.Connection
import java.util.HexFormat

internal object SeedArtworkStore {
    fun existing(
        connection: Connection,
        poster: SeedPoster,
    ): AssetRow? =
        connection
            .queryBound("SELECT * FROM asset WHERE id=?", listOf(poster.asset), AssetRow::decode)
            .singleOrNull()

    fun reference(
        connection: Connection,
        poster: SeedPoster,
    ): String? =
        connection
            .queryBound(
                "SELECT state FROM asset_reference WHERE id=? AND title_id=? AND asset_id=?",
                listOf(poster.hold, poster.title, poster.asset),
            ) { it.getString("state") }
            .singleOrNull()

    fun insert(
        connection: Connection,
        input: SeedArtworkInput,
        poster: SeedPoster,
        result: ProcessorRunner.Outcome.Success,
        bytes: Long,
    ) {
        receipt(connection, input, poster)
        connection.executeBound(
            "INSERT INTO asset(id,upload_operation_id,actor_id,state,storage_key,content_hash,format,input_bytes," +
                "output_bytes,width,height,policy_revision,provenance) VALUES(?,?,?,'READY',?,?,?,?,?,?,?,?,?::jsonb)",
            poster.asset,
            poster.asset,
            input.actor,
            "output/${result.outputPath.fileName}",
            result.contentHash,
            result.format.label,
            bytes,
            result.outputBytes,
            result.width,
            result.height,
            input.snapshot,
            SeedJson.mapper.writeValueAsString(poster.artwork),
        )
        connection.executeBound(
            "INSERT INTO asset_reference(id,asset_id,title_id,state) VALUES(?,?,?,'RESERVED')",
            poster.hold,
            poster.asset,
            poster.title,
        )
    }

    private fun receipt(
        connection: Connection,
        input: SeedArtworkInput,
        poster: SeedPoster,
    ) {
        connection.executeBound(
            "INSERT INTO operation_receipt(id,actor_scope,kind,receipt_type,request_hash,state," +
                "result,correlation_id,expires_at) " +
                "VALUES(?,?,'SEED_ARTWORK_IMPORT','REQUEST',?,'SUCCEEDED',?::jsonb,?," +
                "CURRENT_TIMESTAMP+INTERVAL '7 days')",
            poster.asset,
            "account:${input.actor}",
            HexFormat.of().parseHex(poster.hash),
            "{\"resultId\":\"${poster.asset}\"}",
            poster.asset,
        )
    }

    fun finalize(
        connection: Connection,
        input: SeedArtworkInput,
        poster: SeedPoster,
        row: tools.jackson.databind.JsonNode,
    ) {
        require(row["assetId"].asText() == poster.asset.toString() && row["holdId"].asText() == poster.hold.toString())
        val state = reference(connection, poster) ?: error("Seed artwork reference missing")
        if (row["state"].asText() == "ATTACHED") {
            require(state != "RELEASED") { "Removed seed reference cannot be restored" }
            val version = row["catalogVersion"].asLong()
            require(version > 0)
            connection.executeBound(
                "UPDATE asset_reference SET state='ACTIVE',catalog_version=GREATEST(COALESCE(catalog_version,0),?)," +
                    "updated_at=CURRENT_TIMESTAMP WHERE id=? AND state IN ('RESERVED','ACTIVE')",
                version,
                poster.hold,
            )
        } else {
            connection.executeBound(
                "UPDATE asset_reference SET state='RELEASED',updated_at=CURRENT_TIMESTAMP " +
                    "WHERE id=? AND state='RESERVED'",
                poster.hold,
            )
        }
        require(input.version.isNotBlank())
    }
}
