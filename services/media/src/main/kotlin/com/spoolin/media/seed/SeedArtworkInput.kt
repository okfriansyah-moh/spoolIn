package com.spoolin.media.seed

import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.security.MessageDigest
import java.util.HexFormat
import java.util.UUID

internal data class SeedPoster(
    val title: UUID,
    val artwork: JsonNode,
) {
    val asset: UUID = UUID.fromString(artwork["assetId"].asText())
    val hold: UUID = UUID.fromString(artwork["holdId"].asText())
    val hash: String = artwork["sha256"].asText()
}

internal class SeedArtworkInput(
    path: Path,
) {
    val directory: Path = path.toAbsolutePath().parent.toRealPath()
    val root: JsonNode = JsonMapper.builder().build().readTree(path.toFile())
    val version: String = root["manifestVersion"].asText()
    val snapshot: String = root["snapshotSHA256"].asText()
    val actor: UUID = UUID.fromString(root["namespaceUUID"].asText())
    val posters: List<SeedPoster> =
        root["records"].toList().map {
            SeedPoster(UUID.fromString(it["stableUUID"].asText()), it["artwork"])
        }

    init {
        require(version.matches(Regex("[A-Za-z0-9_.-]{1,64}")))
        require(root["sourcePolicyVersion"].asText() == "public-pages-v1")
        require(snapshot == SeedJson.hash(root)) { "Seed snapshot hash differs" }
        require(posters.size == TITLE_COUNT && posters.map { it.title }.distinct().size == TITLE_COUNT)
        require(root["records"].count { it["kind"].asText() == "MOVIE" } == TYPE_COUNT)
        require(root["records"].count { it["kind"].asText() == "TV" } == TYPE_COUNT)
        posters.forEach(::validateIdentity)
    }

    fun file(poster: SeedPoster): Path {
        val relative = "posters/${poster.title}.jpg"
        require(poster.artwork["file"].asText() == relative)
        val file = directory.resolve(relative)
        require(Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) && file.toRealPath().startsWith(directory))
        return file
    }

    private fun validateIdentity(poster: SeedPoster) {
        require(poster.hash.matches(Regex("[0-9a-f]{64}")))
        require(poster.asset == SeedJson.uuid(actor, "SEED_ARTWORK:${poster.title}:${poster.hash}"))
        require(poster.hold == SeedJson.uuid(actor, "SEED_ARTWORK_REFERENCE:${poster.title}:${poster.hash}"))
    }
}

internal object SeedJson {
    val mapper: JsonMapper = JsonMapper.builder().build()
    private val excluded = setOf("generatedAt", "retrievedAt", "snapshotSHA256", "contentSHA256")

    fun hash(node: JsonNode): String {
        val bytes = mapper.writeValueAsBytes(stable(node))
        return hex(MessageDigest.getInstance("SHA-256").digest(bytes))
    }

    fun hex(bytes: ByteArray): String = HexFormat.of().formatHex(bytes)

    fun uuid(
        namespace: UUID,
        name: String,
    ): UUID =
        com.spoolin.support.identity.NamedIdentities
            .id(namespace, name.toByteArray(Charsets.UTF_8))

    private fun stable(node: JsonNode): Any? =
        when {
            node.isObject ->
                node
                    .propertyNames()
                    .filter { it !in excluded }
                    .sorted()
                    .associateWith { stable(node[it]) }
                    .toSortedMap()
            node.isArray -> node.toList().map(::stable)
            node.isNull -> null
            node.isBoolean -> node.asBoolean()
            node.isIntegralNumber -> node.asLong()
            node.isNumber -> node.decimalValue()
            else -> node.asText()
        }
}

private const val TITLE_COUNT = 200
private const val TYPE_COUNT = 100
