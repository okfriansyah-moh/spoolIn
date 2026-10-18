package com.spoolin.catalog.seed

import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.HexFormat
import java.util.UUID
import javax.imageio.ImageIO

internal object SeedArtworkValidation {
    private const val MAX_DIMENSION = 4096
    private const val MAX_BYTES = 10_000_000L

    fun validate(seed: SeedManifest) {
        require(seed.root.path("files").size() == SeedLimits.TITLES)
        val files = seed.root["files"].associateBy { it["titleId"].asText() }
        require(files.size == SeedLimits.TITLES)
        seed.records.forEach { record ->
            val art = record.path("artwork")
            val id = record["stableUUID"].asText()
            require(art.isObject && art.path("file").asText() == "posters/$id.jpg")
            require(art.path("sha256").asText().matches(Regex("[a-f0-9]{64}")))
            require(art.path("mimeType").asText() == "image/jpeg")
            require(art.path("width").asInt() in 1..MAX_DIMENSION && art.path("height").asInt() in 1..MAX_DIMENSION)
            val hash = art["sha256"].asText()
            val asset = SeedIdentities.named(seed.namespace, "SEED_ARTWORK:$id:$hash")
            require(art.path("assetId").asText() == asset.toString())
            val hold = SeedIdentities.named(seed.namespace, "SEED_ARTWORK_REFERENCE:$id:$hash")
            require(art.path("holdId").asText() == hold.toString())
            listOf("sourceUrl", "imageUrl").forEach { field ->
                val uri = URI(art.path(field).asText())
                require(uri.scheme == "https" && uri.host != null && uri.userInfo == null)
            }
            java.time.Instant.parse(art.path("retrievedAt").asText())
            require(art.path("sourceName").asText().isNotBlank())
            val listed = files.getValue(id)
            art.propertyNames().forEach { require(listed[it] == art[it]) }
        }
    }

    fun validateFiles(
        seed: SeedManifest,
        directory: Path,
    ) {
        if (!seed.publicPages) return
        seed.records.forEach { record ->
            val art = record["artwork"]
            val path = directory.resolve(art["file"].asText())
            require(!Files.isSymbolicLink(path) && Files.size(path) in 1..MAX_BYTES)
            val hash = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))
            require(HexFormat.of().formatHex(hash) == art["sha256"].asText())
            validateImage(path, art["width"].asInt(), art["height"].asInt())
        }
    }

    private fun validateImage(
        path: Path,
        width: Int,
        height: Int,
    ) {
        ImageIO.createImageInputStream(path.toFile()).use { stream ->
            val readers = ImageIO.getImageReaders(stream)
            require(readers.hasNext()) { "Unsupported poster image" }
            val reader = readers.next()
            try {
                reader.input = stream
                require(reader.formatName.equals("JPEG", true))
                require(reader.getWidth(0) == width)
                require(reader.getHeight(0) == height)
                reader.read(0)
            } finally {
                reader.dispose()
            }
        }
    }
}
