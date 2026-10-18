package com.spoolin.catalog.features.taxonomy.read

import com.spoolin.support.receipts.queryBound
import java.util.UUID
import javax.sql.DataSource

/**
 * Genre and credit-role reads for the public catalog taxonomy.
 *
 * Genre ordering: normalized_label ASC, id ASC (stable).
 * Role ordering:  normalized_label ASC, id ASC (stable).
 */
class TaxonomyReads(
    private val dataSource: DataSource,
) {
    data class GenreRow(
        val id: UUID,
        val label: String,
    )

    data class RoleRow(
        val id: UUID,
        val category: String,
        val label: String,
        val allowsCharacters: Boolean,
    )

    fun listGenres(): List<GenreRow> =
        dataSource.connection.use { conn ->
            conn.queryBound(
                "SELECT id, label FROM genre ORDER BY normalized_label ASC, id ASC",
            ) { rs ->
                GenreRow(rs.getObject("id", UUID::class.java), rs.getString("label"))
            }
        }

    data class ReleaseYear(
        val year: Int,
        val months: List<Int>,
    )

    data class FilterOptions(
        val genres: List<GenreRow>,
        val releaseYears: List<ReleaseYear>,
    )

    fun catalogFilterOptions(): FilterOptions =
        dataSource.connection.use { conn ->
            val genres =
                conn.queryBound(
                    "SELECT g.id, g.label FROM genre g WHERE EXISTS (" +
                        "SELECT 1 FROM title_genre tg JOIN title t ON t.id = tg.title_id " +
                        "WHERE tg.genre_id = g.id AND t.status = 'ACTIVE') " +
                        "ORDER BY g.normalized_label, g.id",
                ) { rs -> GenreRow(rs.getObject("id", UUID::class.java), rs.getString("label")) }
            val dates =
                conn.queryBound(
                    "SELECT DISTINCT release_year, release_month FROM title " +
                        "WHERE status = 'ACTIVE' AND release_year IS NOT NULL " +
                        "ORDER BY release_year DESC, release_month",
                ) { rs -> rs.getInt("release_year") to (rs.getObject("release_month") as? Number)?.toInt() }
            FilterOptions(
                genres,
                dates.groupBy({ it.first }, { it.second }).map { (year, months) ->
                    ReleaseYear(year, months.filterNotNull())
                },
            )
        }

    fun listRoles(): List<RoleRow> =
        dataSource.connection.use { conn ->
            conn.queryBound(
                "SELECT id, category, label, allows_characters FROM credit_role " +
                    "ORDER BY normalized_label ASC, id ASC",
            ) { rs ->
                RoleRow(
                    rs.getObject("id", UUID::class.java),
                    rs.getString("category"),
                    rs.getString("label"),
                    rs.getBoolean("allows_characters"),
                )
            }
        }
}
