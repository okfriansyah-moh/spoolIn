package com.spoolin.media.seed

import com.spoolin.media.features.processing.CodecExecution
import com.spoolin.media.features.processing.ProcessorRunner
import com.spoolin.media.features.upload.QuarantineStore
import com.spoolin.media.fixtures.JdbcBoundary
import com.spoolin.media.fixtures.actorId
import com.spoolin.media.fixtures.assetId
import com.spoolin.media.fixtures.assetRecord
import com.spoolin.media.fixtures.holdId
import com.spoolin.media.fixtures.titleId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tools.jackson.databind.node.ObjectNode
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.UUID
import javax.imageio.ImageIO

class SeedArtworkWorkflowRulesTest {
    @TempDir lateinit var root: Path
    private val bytes =
        ByteArrayOutputStream()
            .also {
                ImageIO.write(BufferedImage(2, 3, BufferedImage.TYPE_INT_RGB), "jpeg", it)
            }.toByteArray()
    private val hash = SeedJson.hex(MessageDigest.getInstance("SHA-256").digest(bytes))

    private fun input(): SeedArtworkInput {
        val posters = Files.createDirectories(root.resolve("posters"))
        val records =
            (0 until 200).map { index ->
                val title = UUID.nameUUIDFromBytes("seed-title:$index".toByteArray())
                Files.write(posters.resolve("$title.jpg"), bytes)
                mapOf(
                    "stableUUID" to title.toString(),
                    "kind" to if (index < 100) "MOVIE" else "TV",
                    "artwork" to
                        mapOf(
                            "assetId" to SeedJson.uuid(actorId, "SEED_ARTWORK:$title:$hash").toString(),
                            "holdId" to SeedJson.uuid(actorId, "SEED_ARTWORK_REFERENCE:$title:$hash").toString(),
                            "sha256" to hash,
                            "width" to 2,
                            "height" to 3,
                            "file" to "posters/$title.jpg",
                        ),
                )
            }
        val tree =
            SeedJson.mapper.valueToTree(
                mapOf(
                    "manifestVersion" to "unit-seed-v1",
                    "sourcePolicyVersion" to "public-pages-v1",
                    "namespaceUUID" to actorId.toString(),
                    "records" to records,
                ),
            ) as ObjectNode
        tree.put("snapshotSHA256", SeedJson.hash(tree))
        val path = root.resolve("manifest.json")
        Files.write(path, SeedJson.mapper.writeValueAsBytes(tree))
        return SeedArtworkInput(path)
    }

    private fun record(poster: SeedPoster) =
        assetRecord() +
            mapOf(
                "id" to poster.asset,
                "upload_operation_id" to poster.asset,
                "storage_key" to "output/${poster.asset}.jpeg",
                "format" to "JPEG",
                "output_bytes" to bytes.size.toLong(),
                "width" to 2,
                "height" to 3,
                "content_hash" to MessageDigest.getInstance("SHA-256").digest(bytes),
                "provenance" to SeedJson.mapper.writeValueAsString(poster.artwork),
            )

    private fun processor() =
        ProcessorRunner(
            sandbox =
                CodecExecution { _, scratch, args, _ ->
                    if (args.first().endsWith("spoolin-image-header")) {
                        Files.writeString(scratch.resolve("dimensions"), "2 3 3")
                    } else {
                        Files.write(scratch.resolve("encoded.jpeg"), bytes)
                    }
                    null
                },
        )

    @Test fun `seed preparation imports each validated poster and returns only safe attachment metadata`() {
        val input = input()
        val db = JdbcBoundary().rows()
        input.posters.forEach { db.rows().rows(record(it)).rows(mapOf("state" to "RESERVED")) }
        db.rows()
        val result = SeedArtworkImporter(db.source, QuarantineStore(root.resolve("volume")), processor()).prepare(input)
        assertEquals("unit-seed-v1", result["manifestVersion"])
        val assets = result["assets"] as List<*>
        assertEquals(200, assets.size)
        val first = assets.first() as Map<*, *>
        assertEquals(
            input.posters
                .first()
                .asset
                .toString(),
            first["assetId"],
        )
        assertEquals(true, first["available"])
        assertEquals("RESERVED", first["referenceState"])
        assertFalse(first.containsKey("actorId"))
        assertEquals(200, db.commits)
    }

    @Test fun `already imported ready posters verify immutable output and preserve released references`() {
        val input = input()
        val store = QuarantineStore(root.resolve("volume"))
        val db = JdbcBoundary().rows()
        input.posters.forEach { poster ->
            Files.write(store.outputDirectory().resolve("${poster.asset}.jpeg"), bytes)
            db.rows(record(poster)).rows(record(poster)).rows(mapOf("state" to "RELEASED"))
        }
        db.rows()
        val result = SeedArtworkImporter(db.source, store, processor()).prepare(input)
        val first = (result["assets"] as List<*>).first() as Map<*, *>
        assertEquals(false, first["available"])
        assertEquals("RELEASED", first["referenceState"])
        assertEquals(0, db.commits)
    }

    @Test fun `finalization reconciles complete catalog receipt and returns attached count`() {
        val input = input()
        val attachments =
            input.posters.mapIndexed { index, poster ->
                mapOf(
                    "titleId" to poster.title.toString(),
                    "assetId" to poster.asset.toString(),
                    "holdId" to poster.hold.toString(),
                    "state" to
                        if (index ==
                            0
                        ) {
                            "CANCELLED"
                        } else {
                            "ATTACHED"
                        },
                    "catalogVersion" to 4,
                )
            }
        val receipt = root.resolve("receipt.json")
        Files.write(
            receipt,
            SeedJson.mapper.writeValueAsBytes(
                mapOf(
                    "manifestVersion" to input.version,
                    "snapshotSHA256" to input.snapshot,
                    "attachments" to attachments,
                ),
            ),
        )
        val db = JdbcBoundary().rows()
        input.posters.forEach { db.rows(mapOf("state" to "RESERVED")) }
        db.rows()
        assertEquals(
            mapOf("manifestVersion" to input.version, "finalized" to 199),
            SeedArtworkImporter(db.source, QuarantineStore(root.resolve("volume")), processor()).finish(input, receipt),
        )
        assertEquals(200, db.commits)
    }

    @Test fun `finalization rejects mismatched manifest and removed references before restoration`() {
        val input = input()
        val receipt = root.resolve("receipt.json")
        Files.writeString(receipt, "{\"manifestVersion\":\"wrong\"}")
        assertThrows(IllegalArgumentException::class.java) {
            SeedArtworkImporter(JdbcBoundary().source, QuarantineStore(root.resolve("volume")), processor()).finish(input, receipt)
        }
        val poster = input.posters.first()
        val row =
            SeedJson.mapper.readTree(
                "{\"assetId\":\"${poster.asset}\",\"holdId\":\"${poster.hold}\",\"state\":\"ATTACHED\",\"catalogVersion\":4}",
            )
        assertThrows(IllegalArgumentException::class.java) {
            SeedArtworkStore.finalize(JdbcBoundary().rows(mapOf("state" to "RELEASED")).connection, input, poster, row)
        }
    }
}
