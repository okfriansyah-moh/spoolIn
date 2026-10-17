package com.spoolin.catalog.features.proposal

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import java.util.HexFormat

class DeterministicContentRulesTest {
    @Test
    fun vectorA_digest_matches_fixture() {
        val content =
            mapOf(
                "genres" to listOf("a", "b"),
                "synopsis" to "A\nB",
                "title" to "Café",
                "unknown" to null,
            )
        val digest = DeterministicContentSerializer.digest(content)
        val expected = HexFormat.of().parseHex("6c35f24215047d8053eaeba11848dc0b1bd44144067e952c5ad5c4fc34240903")
        assertArrayEquals(expected, digest)
    }

    @Test
    fun reversed_keys_and_genres_match_vector_a() {
        val vectorA =
            mapOf(
                "genres" to listOf("a", "b"),
                "synopsis" to "A\nB",
                "title" to "Café",
                "unknown" to null,
            )
        val vectorB =
            mapOf(
                "title" to "Café",
                "unknown" to null,
                "synopsis" to "A\nB",
                "genres" to listOf("b", "a"),
            )
        assertArrayEquals(
            DeterministicContentSerializer.digest(vectorA),
            DeterministicContentSerializer.digest(vectorB),
        )
    }

    @Test
    fun ordered_characters_differ_when_reversed() {
        val forward = mapOf("characters" to listOf("Maya", "Maya / Echo"))
        val reversed = mapOf("characters" to listOf("Maya / Echo", "Maya"))
        assertArrayEquals(
            DeterministicContentSerializer.digest(forward),
            DeterministicContentSerializer.digest(forward),
        )
        org.junit.jupiter.api.Assertions.assertFalse(
            DeterministicContentSerializer.digest(forward).contentEquals(
                DeterministicContentSerializer.digest(reversed),
            ),
        )
    }
}
