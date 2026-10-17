package com.spoolin.catalog.operations.audit

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

// ATDD-298: Safe diff with allowlisted paths, size guard, and summarised form when oversized.
// Forbidden fields never appear; domain inputs are never modified.
private const val CHANGES_BYTE_LIMIT = 65536

/**
 * Computes immutable before/after diffs for business audit records.
 *
 * Within-bound diffs are stored inline.  Oversized diffs retain paths, exact byte lengths,
 * SHA-256 digests and valueOmitted=true — the original domain inputs are never mutated.
 * Private/unlisted fields are filtered before any processing.
 */
object AuditDiff {

    // Safe domain fields for movie change records (no credentials, tokens or private state).
    val MOVIE_PATHS: Set<String> = setOf(
        "displayTitle", "originalTitle", "synopsis", "synopsisLanguage",
        "originalLanguage", "countries", "releaseYear", "releaseMonth",
        "releaseDay", "releaseContext", "runtimeMinutes", "genreIds", "status", "version",
    )

    // Safe domain fields for person change records (no credentials, tokens or private state).
    val PERSON_PATHS: Set<String> = setOf(
        "name", "biography", "status", "version",
    )

    data class Change(val path: String, val before: String?, val after: String?)

    /** Produces a changes JSON string for [before] → [after] restricted to [allowed] paths. */
    fun compute(before: Map<String, Any?>, after: Map<String, Any?>, allowed: Set<String>): String {
        val changes = buildList(before, after, allowed)
        return encode(changes)
    }

    /** Convenience wrapper for movie domain snapshots. */
    fun movie(before: Map<String, Any?>, after: Map<String, Any?>): String =
        compute(before, after, MOVIE_PATHS)

    /** Convenience wrapper for person domain snapshots. */
    fun person(before: Map<String, Any?>, after: Map<String, Any?>): String =
        compute(before, after, PERSON_PATHS)

    internal fun buildList(
        before: Map<String, Any?>,
        after: Map<String, Any?>,
        allowed: Set<String>,
    ): List<Change> {
        val paths = (before.keys + after.keys).filter { it in allowed }.distinct().sorted()
        return paths.mapNotNull { path ->
            val b = before[path]
            val a = after[path]
            if (b == a) null else Change(path, encodeValue(b), encodeValue(a))
        }
    }

    internal fun encode(changes: List<Change>): String {
        val inline = encodeInline(changes)
        if (inline.toByteArray(StandardCharsets.UTF_8).size <= CHANGES_BYTE_LIMIT) return inline
        return encodeSummarised(changes, inline.toByteArray(StandardCharsets.UTF_8).size)
    }

    private fun encodeInline(changes: List<Change>): String {
        val entries = changes.joinToString(",") { c ->
            val path = jsonStr(c.path)
            val b = if (c.before == null) "null" else c.before
            val a = if (c.after == null) "null" else c.after
            """{"path":$path,"before":$b,"after":$a}"""
        }
        return """{"changes":[$entries],"summarised":false}"""
    }

    private fun encodeSummarised(changes: List<Change>, totalBytes: Int): String {
        val entries = changes.joinToString(",") { c ->
            val path = jsonStr(c.path)
            val bBytes = c.before?.toByteArray(StandardCharsets.UTF_8)?.size ?: 0
            val aBytes = c.after?.toByteArray(StandardCharsets.UTF_8)?.size ?: 0
            val bDigest = c.before?.let { jsonStr(sha256Hex(it)) } ?: "null"
            val aDigest = c.after?.let { jsonStr(sha256Hex(it)) } ?: "null"
            """{"path":$path,"byteLength":${bBytes + aBytes},"beforeDigest":$bDigest,"afterDigest":$aDigest,"valueOmitted":true}"""
        }
        val result = """{"changes":[$entries],"summarised":true,"totalBytes":$totalBytes}"""
        return if (result.toByteArray(StandardCharsets.UTF_8).size <= CHANGES_BYTE_LIMIT) result
        else """{"changes":[],"summarised":true,"totalBytes":$totalBytes,"truncated":true}"""
    }

    private fun sha256Hex(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    internal fun encodeValue(value: Any?): String? = when (value) {
        null -> null
        is String -> jsonStr(value)
        is Number -> value.toString()
        is Boolean -> value.toString()
        is List<*> -> "[${value.joinToString(",") { encodeValue(it) ?: "null" }}]"
        is Set<*> -> "[${value.sortedBy { it.toString() }.joinToString(",") { encodeValue(it) ?: "null" }}]"
        else -> jsonStr(value.toString())
    }

    internal fun jsonStr(value: String): String =
        "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
}
