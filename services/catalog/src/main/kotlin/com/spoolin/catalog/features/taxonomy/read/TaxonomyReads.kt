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
class TaxonomyReads(private val dataSource: DataSource) {

    data class GenreRow(val id: UUID, val label: String)

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
