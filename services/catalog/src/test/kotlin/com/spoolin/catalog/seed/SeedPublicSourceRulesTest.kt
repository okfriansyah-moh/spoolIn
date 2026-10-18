package com.spoolin.catalog.seed

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

class SeedPublicSourceRulesTest {
    private val namespace = UUID.fromString("9da61864-80f5-5c70-bc79-3914da94079e")
    private val mapper = JsonMapper.builder().build()

    @Test
    fun `public page sources preserve unspecified rights without claiming an open license`() {
        SeedStructure(namespace, true).sources(
            source("https://www.themoviedb.org/movie/11", "not-specified"),
            setOf("title"),
        )
    }

    @Test
    fun `legacy source policy rejects the public page policy`() {
        assertThrows(IllegalArgumentException::class.java) {
            SeedStructure(namespace).sources(
                source("https://www.themoviedb.org/movie/11", "not-specified"),
                setOf("title"),
            )
        }
    }

    @Test
    fun `source provenance rejects credentials and insecure URLs`() {
        listOf("http://example.com/title", "https://secret@example.com/title").forEach { url ->
            assertThrows(IllegalArgumentException::class.java) {
                SeedStructure(namespace, true).sources(source(url, "not-specified"), setOf("title"))
            }
        }
    }

    private fun source(
        url: String,
        license: String,
    ) = mapper.readTree(
        mapper.writeValueAsString(
            mapOf(
                "sources" to
                    listOf(
                        mapOf(
                            "field" to "title",
                            "url" to url,
                            "license" to license,
                            "permissionBasis" to "Public webpage source recorded; no open-license claim.",
                            "retrievedAt" to "2026-10-18T00:00:00Z",
                        ),
                    ),
            ),
        ),
    )
}
