package com.spoolin.media.seed

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class SeedArtworkRulesTest {
    @TempDir
    lateinit var temporary: Path

    private fun packagePath(): Path =
        generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .map { it.resolve("data/seed/manifest.json") }
            .first(Files::isRegularFile)

    @Test
    fun `frozen package has deterministic title and artwork identities`() {
        val input = SeedArtworkInput(packagePath())
        assertEquals(200, input.posters.size)
        assertEquals(
            200,
            input.posters
                .map { it.asset }
                .distinct()
                .size,
        )
        input.posters.forEach { input.file(it) }
    }

    @Test
    fun `changed package is rejected before any database write`() {
        val tree = SeedJson.mapper.readTree(packagePath().toFile()).deepCopy() as tools.jackson.databind.node.ObjectNode
        tree.put("manifestVersion", "tampered")
        val file = temporary.resolve("manifest.json")
        Files.write(file, SeedJson.mapper.writeValueAsBytes(tree))
        assertThrows(IllegalArgumentException::class.java) { SeedArtworkInput(file) }
    }

    @Test
    fun `poster path cannot escape the frozen package`() {
        val input = SeedArtworkInput(packagePath())
        val art =
            input.posters
                .first()
                .artwork
                .deepCopy() as tools.jackson.databind.node.ObjectNode
        art.put("file", "../outside.jpg")
        assertThrows(IllegalArgumentException::class.java) {
            input.file(SeedPoster(input.posters.first().title, art))
        }
    }
}
