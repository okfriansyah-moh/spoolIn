package com.spoolin.catalog.features.taxonomy.write

import com.spoolin.support.policy.InputViolation
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import java.sql.Connection
import java.util.UUID

private const val MAXIMUM_GENRES = 20

/**
 * Genre association writes for movie titles.
 *
 * All operations execute within the caller's transaction scope.
 * Genre validation requires all supplied IDs to reference existing rows.
 * D18: Uses parameterised native SQL — no genre label text appears in SQL.
 */
object GenreTagging {

/**
 * Parses and bounds-checks genre ID strings; does not validate database existence.
 * Used for unit-testable pre-validation before the DB lookup in [resolve].
 */
fun validateGenreIds(genreIds: Set<String>): Set<UUID> {
    if (genreIds.isEmpty()) throw InputViolation("genreIds", "Choose at least one genre.")
    if (genreIds.size > MAXIMUM_GENRES) throw InputViolation("genreIds", "Choose at most 20 genres.")
    return genreIds.map { id ->
        runCatching { UUID.fromString(id) }.getOrElse { throw InputViolation("genreIds", "Supply valid genre UUIDs.") }
    }.toSet()
}

/**
 * Validates that all [genreIds] reference known genres and returns their UUIDs.
 * Throws [InputViolation] on empty, too many, malformed, or unknown IDs.
 */
fun resolve(connection: Connection, genreIds: Set<String>): Set<UUID> {
    val uuids = validateGenreIds(genreIds)
        val placeholders = uuids.joinToString(",") { "?" }
        val found = connection.queryBound(
            "SELECT id FROM genre WHERE id IN ($placeholders)",
            uuids.toList(),
        ) { rs -> rs.getObject("id", UUID::class.java) }.toSet()
        if (found.size != uuids.size) throw InputViolation("genreIds", "One or more genre IDs are unknown.")
        return uuids
    }

    /**
     * Replaces all genre associations for [titleId] with [genreUuids].
     * Stable insertion order (sorted UUID) avoids deadlocks in concurrent writes.
     */
    fun replace(connection: Connection, titleId: UUID, genreUuids: Set<UUID>) {
        connection.executeBound("DELETE FROM title_genre WHERE title_id = ?", titleId)
        for (genreId in genreUuids.sortedBy { it.toString() }) {
            connection.executeBound(
                "INSERT INTO title_genre (title_id, genre_id) VALUES (?, ?)",
                titleId, genreId,
            )
        }
    }

    /** Returns the current genre IDs for [titleId], sorted for stable comparison. */
    fun current(connection: Connection, titleId: UUID): Set<UUID> =
        connection.queryBound(
            "SELECT genre_id FROM title_genre WHERE title_id = ? ORDER BY genre_id",
            listOf(titleId),
        ) { rs -> rs.getObject("genre_id", UUID::class.java) }.toSet()
}
