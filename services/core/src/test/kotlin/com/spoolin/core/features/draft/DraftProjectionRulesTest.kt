package com.spoolin.core.features.draft

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class DraftProjectionRulesTest {
    @Test
    fun `recovered person draft exposes an object that can be edited and saved again`() {
        val values = mapOf("name" to "A. Rivers", "biography" to "A fictional director.")
        val draft = projection("PERSON", """{"name":"A. Rivers","biography":"A fictional director."}""")
        val recovered = draftGraphqlProjection(draft)["values"]
        assertEquals(values, recovered)
        val encoded = tools.jackson.databind.ObjectMapper().writeValueAsString(recovered)
        assertTrue(DraftPayload().encode("PERSON", encoded).isNotEmpty())
    }

    @Test
    fun `recovered movie arrays and nullable values retain their JSON shape`() {
        val draft = projection("MOVIE", """{"title":"Unfinished","genreIds":["drama"],"synopsis":null}""")
        assertEquals(
            mapOf("title" to "Unfinished", "genreIds" to listOf("drama"), "synopsis" to null),
            draftGraphqlProjection(draft)["values"],
        )
    }

    private fun projection(
        kind: String,
        values: String,
    ) = DraftProjection(
        id = UUID.randomUUID(),
        targetKind = kind,
        targetId = UUID.randomUUID(),
        baseVersion = 0,
        operationId = null,
        values = values,
        version = 0,
        expiresAt = Instant.parse("2026-10-19T00:00:00Z"),
    )
}
