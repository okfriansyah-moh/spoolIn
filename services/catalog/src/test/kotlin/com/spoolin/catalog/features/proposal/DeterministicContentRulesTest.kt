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
}
