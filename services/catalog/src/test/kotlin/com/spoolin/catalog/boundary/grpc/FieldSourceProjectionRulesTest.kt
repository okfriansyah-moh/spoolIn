package com.spoolin.catalog.boundary.grpc

import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Files
import java.nio.file.Path

class FieldSourceProjectionRulesTest {
    @Test
    fun `owner source facts become typed fields without private ledger extras`() {
        val result = FieldSourceProjection.parse(source(", \"credential\":\"private-value\""))
        val field = result.single()
        assertEquals("release", field.field)
        assertEquals("https://www.wikidata.org/wiki/Q1", field.url)
        assertEquals("CC0 facts", field.reuseBasis)
        assertEquals("YEAR", field.precision)
        assertEquals(1_735_689_600L, field.retrievedAt.seconds)
        assertFalse(field.toString().contains("private-value"))
    }

    @Test
    fun `malformed source shape cannot become fabricated empty evidence`() {
        listOf(
            "[]",
            "{",
            """{"session":{"url":"https://example.invalid"}}""",
            source().replace("https://www.wikidata.org/wiki/Q1", "javascript:alert(1)"),
            source().replace("2025-01-01T00:00:00Z", "invalid"),
            source().replace("CC0 facts", ""),
        ).forEach { invalid ->
            val failure = assertThrows(StatusRuntimeException::class.java) { FieldSourceProjection.parse(invalid) }
            assertEquals(Status.Code.DATA_LOSS, failure.status.code)
        }
    }

    @Test
    fun `empty source ledger is represented as no asserted field facts`() {
        assertEquals(emptyList<Any>(), FieldSourceProjection.parse("{}"))
    }

    @Test
    fun `duplicate and oversized ledgers fail closed`() {
        val duplicate = """{"release":{},"release":{}}"""
        listOf(duplicate, " ".repeat(16_385)).forEach { value ->
            assertThrows(StatusRuntimeException::class.java) { FieldSourceProjection.parse(value) }
        }
    }

    @Test
    fun `every frozen title source projects without changing source field identities`() {
        val mapper = JsonMapper.builder().build()
        val manifest = mapper.readTree(repository().resolve("data/seed/manifest.json").toFile())
        val records = manifest["records"].toList()
        assertEquals(200, records.size)
        assertEquals(100, records.count { it["kind"].asText() == "MOVIE" })
        assertEquals(100, records.count { it["kind"].asText() == "TV" })
        records.forEach { record ->
            val sources = record["sources"].associateBy { it["field"].asText() }
            val projected = FieldSourceProjection.parse(mapper.writeValueAsString(sources))
            assertEquals(sources.keys, projected.map { it.field }.toSet(), record["stableUUID"].asText())
            projected.forEach { field ->
                val original = sources.getValue(field.field)
                assertEquals(original["url"].asText(), field.url)
                assertEquals(original["permissionBasis"].asText(), field.reuseBasis)
                assertFalse(field.toString().contains("permissionBasis"))
                assertFalse(field.toString().contains("CC0-1.0"))
            }
        }
        assertEquals(100, records.count { record -> record["sources"].any { it["field"].asText() == "end" } })
    }

    private fun repository(): Path =
        generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) { it.parent }
            .first { Files.isRegularFile(it.resolve("data/seed/manifest.json")) }

    private fun source(extra: String = ""): String =
        """{"release":{"field":"release","url":"https://www.wikidata.org/wiki/Q1",
        "permissionBasis":"CC0 facts","retrievedAt":"2025-01-01T00:00:00Z","precision":"YEAR"$extra}}"""
}
