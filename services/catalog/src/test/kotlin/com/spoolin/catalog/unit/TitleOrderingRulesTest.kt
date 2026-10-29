package com.spoolin.catalog.unit

import com.spoolin.catalog.features.search.CatalogCursor
import com.spoolin.catalog.features.search.TitleOrdering
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class TitleOrderingRulesTest {
    private val id = UUID(0, 1)
    private val now = Instant.parse("2026-01-01T00:00:00Z")

    @Test
    fun `signed values round trip arbitrary Unicode inside existing cursor bounds`() {
        val cursor = CatalogCursor(ByteArray(32))
        val random = java.util.Random(17)
        val longTitle = buildString { repeat(255) { appendCodePoint(0x10000 + random.nextInt(0x100000)) } }
        listOf("quotes \" and slash \\ and , colon :", longTitle, "σßÉ", now.toString()).forEach { value ->
            val key = CatalogCursor.Keyset(2024, 10, 9, UUID.randomUUID(), value)
            val encoded = cursor.encode(key, "a".repeat(64), now)
            assertTrue(java.util.Base64.getUrlDecoder().decode(encoded).size <= 1024)
            assertEquals(key, cursor.decode(encoded, "a".repeat(64), now))
            assertNull(cursor.decode(encoded, "different", now))
            assertNull(cursor.decode(encoded, "a".repeat(64), now.plusSeconds(900)))
        }
        val largest = CatalogCursor.Keyset(9999, 12, 31, id, "𐐀".repeat(255))
        val widestScope = "￿".repeat(64)
        val largestEncoded = cursor.encode(largest, widestScope, now)
        assertEquals(1024, java.util.Base64.getUrlDecoder().decode(largestEncoded).size)
        assertEquals(largest, cursor.decode(largestEncoded, widestScope, now))
        val oversized = "𐐀".repeat(1000)
        val encoded = cursor.encode(CatalogCursor.Keyset(null, null, null, id, oversized), "a".repeat(64), now)
        val decoded = requireNotNull(cursor.decode(encoded, "a".repeat(64), now))
        assertNull(decoded.sortValue)
        assertEquals(64, decoded.sortHash?.length)
        val legacy = CatalogCursor.Keyset(2024, 10, null, id)
        assertEquals(legacy, cursor.decode(cursor.encode(legacy, "old", now), "old", now))
    }

    @Test
    fun `date continuation retains unknown parts last and binds each comparison`() {
        val key = CatalogCursor.Keyset(2024, null, null, id)
        val params = mutableListOf<Any?>()
        val sql = TitleOrdering.RELEASE_ASC.after(key, params)
        assertTrue(sql.contains("t.release_year > ? OR t.release_year IS NULL"))
        assertTrue(sql.contains("t.release_month IS NULL AND t.release_day IS NULL AND t.id > ?"))
        assertEquals(listOf(2024, 2024, id), params)
        TitleOrdering.entries.forEach { assertTrue(it.orderBy().endsWith("t.id ASC")) }
        assertEquals(TitleOrdering.RELEASE_DESC, TitleOrdering.parse(null))
        assertThrows(IllegalArgumentException::class.java) { TitleOrdering.parse("invalid") }
    }
}
