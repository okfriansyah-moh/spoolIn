package com.spoolin.catalog.features.search

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

/**
 * ATDD-282: Cursor binding and expiry with a controlled clock.
 *
 * Signed synthetic cursor bound to query Light/public scope/A, first tuple,
 * and expiresAt=T.
 *
 * Then:
 *   - Only authentic matching scope before T is eligible.
 *   - Expiry equality (at T), changed binding, altered signature → invalid cursor.
 *
 * Observation: in-process MAC verification on synthetic bytes; no DB/network involved.
 */
class CursorBindingRulesTest {

    private val fixedKey = ByteArray(32) { it.toByte() }
    private val cursor = CatalogCursor(fixedKey)
    private val idA = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001")
    private val scopeA = "abc123scope"
    private val baseInstant = Instant.parse("2026-10-17T06:00:00Z")
    private val T = baseInstant.plusSeconds(900L) // expiry point

    private fun clockAt(instant: Instant) = Clock.fixed(instant, ZoneOffset.UTC)

    // ── valid cases ───────────────────────────────────────────────────────────

    @Test
    fun `cursor valid one microsecond before expiry`() {
        val encoded = cursor.encode(
            CatalogCursor.Keyset(2024, 10, 9, idA),
            scopeA,
            baseInstant,
        )
        val clockBeforeT = clockAt(T.minusNanos(1000)) // T - 1 microsecond
        assertNotNull(cursor.decode(encoded, scopeA, clockBeforeT.instant()), "Must be valid before T")
    }

    // ── expiry boundary ───────────────────────────────────────────────────────

    @Test
    fun `cursor invalid at exactly T (expiry equality)`() {
        val encoded = cursor.encode(
            CatalogCursor.Keyset(2024, 10, 9, idA),
            scopeA,
            baseInstant,
        )
        // At T: now.epochSecond >= expiresAt.epochSecond (>= check)
        val result = cursor.decode(encoded, scopeA, T)
        assertNull(result, "Cursor must be invalid at exactly T")
    }

    @Test
    fun `cursor invalid one microsecond after T`() {
        val encoded = cursor.encode(
            CatalogCursor.Keyset(2024, 10, 9, idA),
            scopeA,
            baseInstant,
        )
        val result = cursor.decode(encoded, scopeA, T.plusNanos(1000))
        assertNull(result, "Cursor must be invalid after T")
    }

    // ── scope binding ─────────────────────────────────────────────────────────

    @Test
    fun `changed scope hash returns invalid cursor`() {
        val encoded = cursor.encode(
            CatalogCursor.Keyset(2024, 10, 9, idA),
            scopeA,
            baseInstant,
        )
        val differentScope = "different-scope"
        val result = cursor.decode(encoded, differentScope, T.minusNanos(1000))
        assertNull(result, "Changed scope must return invalid cursor")
    }

    @Test
    fun `scope hash is stable for same inputs`() {
        val h1 = CatalogCursor.scopeHash("star wars", "MOVIE", listOf("genre-1", "genre-2"), 2024, 10, "public")
        val h2 = CatalogCursor.scopeHash("star wars", "MOVIE", listOf("genre-2", "genre-1"), 2024, 10, "public")
        assertEquals(h1, h2, "Genre list order must not affect scope hash (sorted canonically)")
    }

    // ── signature alteration ──────────────────────────────────────────────────

    @Test
    fun `altered signature returns invalid cursor`() {
        val encoded = cursor.encode(
            CatalogCursor.Keyset(2024, 10, 9, idA),
            scopeA,
            baseInstant,
        )
        val bytes = java.util.Base64.getUrlDecoder().decode(encoded).toMutableList()
        // Flip one bit in the last byte (HMAC region)
        bytes[bytes.size - 1] = (bytes[bytes.size - 1].toInt() xor 0x01).toByte()
        val tampered = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(bytes.toByteArray())
        val result = cursor.decode(tampered, scopeA, T.minusNanos(1000))
        assertNull(result, "Tampered signature must produce invalid cursor")
    }

    @Test
    fun `truncated cursor returns invalid cursor`() {
        val encoded = cursor.encode(
            CatalogCursor.Keyset(2024, 10, 9, idA),
            scopeA,
            baseInstant,
        )
        val truncated = encoded.dropLast(10)
        val result = cursor.decode(truncated, scopeA, T.minusNanos(1000))
        assertNull(result, "Truncated cursor must return invalid")
    }

    // ── keyset roundtrip ──────────────────────────────────────────────────────

    @Test
    fun `full keyset roundtrip preserves year, month, day, uuid`() {
        val original = CatalogCursor.Keyset(2024, 10, 9, idA)
        val encoded = cursor.encode(original, scopeA, baseInstant)
        val decoded = cursor.decode(encoded, scopeA, T.minusNanos(1000))
        assertNotNull(decoded)
        assertEquals(original.releaseYear, decoded!!.releaseYear)
        assertEquals(original.releaseMonth, decoded.releaseMonth)
        assertEquals(original.releaseDay, decoded.releaseDay)
        assertEquals(original.id, decoded.id)
    }

    @Test
    fun `unknown-year keyset roundtrip`() {
        val original = CatalogCursor.Keyset(null, null, null, idA)
        val encoded = cursor.encode(original, scopeA, baseInstant)
        val decoded = cursor.decode(encoded, scopeA, T.minusNanos(1000))
        assertNotNull(decoded)
        assertNull(decoded!!.releaseYear)
        assertNull(decoded.releaseMonth)
        assertNull(decoded.releaseDay)
        assertEquals(idA, decoded.id)
    }

    // ── size constraint ───────────────────────────────────────────────────────

    @Test
    fun `encoded cursor fits within 1024 bytes`() {
        val encoded = cursor.encode(
            CatalogCursor.Keyset(9999, 12, 31, UUID.randomUUID()),
            "a".repeat(64), // maximum plausible scope hash
            baseInstant,
        )
        assert(encoded.length <= 1024) { "Cursor must be ≤ 1024 bytes, got ${encoded.length}" }
    }
}
