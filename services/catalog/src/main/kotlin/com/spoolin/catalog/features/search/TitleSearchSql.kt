@file:Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod")

package com.spoolin.catalog.features.search

import com.spoolin.support.policy.PublicPolicy
import com.spoolin.support.policy.PolicyEnvironment
import com.spoolin.support.policy.ReadPolicy
import com.spoolin.support.receipts.queryBound
import java.sql.Connection
import java.sql.ResultSet
import java.util.UUID
import javax.sql.DataSource


/**
 * Native SQL title search with pg_unicode_fast casefold matching.
 *
 * Ordering: release_year DESC NULLS LAST, release_month DESC NULLS LAST,
 *           release_day DESC NULLS LAST, id ASC (stable UUID tie-break).
 * Visibility: status = 'ACTIVE' only.
 * Genre: OR semantics — any one matching genre qualifies the title.
 * Year/month: "honest precision" — year-only rows have release_month IS NULL,
 *              so a month filter excludes them.
 */
class TitleSearchSql(
    private val dataSource: DataSource,
    private val policy: () -> PublicPolicy = { PolicyEnvironment.resolve(System::getenv) },
) {
    data class TitleRow(
        val id: UUID,
        val kind: String,
        val status: String,
        val displayTitle: String,
        val originalTitle: String?,
        val synopsis: String,
        val synopsisLanguage: String,
        val originalLanguage: String?,
        val countries: List<String>,
        val releaseYear: Int?,
        val releaseMonth: Int?,
        val releaseDay: Int?,
        val releaseContext: String?,
        val runtimeMinutes: Int?,
        val tvStatus: String?,
        val endYear: Int?,
        val endMonth: Int?,
        val endDay: Int?,
        val assetId: UUID?,
        val version: Long,
    )

    data class TitlePage(
        val rows: List<TitleRow>,
        val keyset: CatalogCursor.Keyset?,
        val hasNextPage: Boolean,
    )

    fun search(
        text: String?,
        kind: String?,
        genreIds: List<UUID>,
        year: Int?,
        month: Int?,
        first: Int,
        after: CatalogCursor.Keyset?,
    ): TitlePage {
        val limit = ReadPolicy(policy()).pageSize(first)
        val params = mutableListOf<Any?>()
        val where = mutableListOf<String>("t.status = 'ACTIVE'")

        // Text search using pg_unicode_fast casefold + literal LIKE
        val normalized = ReadPolicy(policy()).search(text)
        if (normalized != null) {
            val pattern = "%" + CatalogCursor.escapeWildcards(normalized) + "%"
            val condition =
                "(casefold(t.display_title COLLATE pg_unicode_fast) " +
                    "LIKE casefold(? COLLATE pg_unicode_fast) ESCAPE '\\' " +
                    "OR (t.original_title IS NOT NULL AND " +
                    "casefold(t.original_title COLLATE pg_unicode_fast) " +
                    "LIKE casefold(? COLLATE pg_unicode_fast) ESCAPE '\\') " +
                    "OR EXISTS (SELECT 1 FROM credit sc JOIN person sp ON sp.id=sc.person_id " +
                    "JOIN credit_role sr ON sr.id=sc.role_id WHERE sc.title_id=t.id " +
                    "AND sp.status='ACTIVE' AND sr.category IN ('CAST','CREATOR') " +
                    "AND casefold(sp.name COLLATE pg_unicode_fast) " +
                    "LIKE casefold(? COLLATE pg_unicode_fast) ESCAPE '\\'))"
            where.add(condition)
            params.add(pattern)
            params.add(pattern)
            params.add(pattern)
        }

        // Kind filter
        if (kind != null) {
            where.add("t.kind = ?")
            params.add(kind)
        }

        // Year/month precision filter
        if (year != null) {
            where.add("t.release_year = ?")
            params.add(year)
            if (month != null) {
                // Month filter: only rows with a known month match
                where.add("t.release_month = ?")
                params.add(month)
            }
        }

        // Genre OR filter
        if (genreIds.isNotEmpty()) {
            val placeholders = genreIds.joinToString(",") { "?" }
            where.add(
                "EXISTS (SELECT 1 FROM title_genre tg " +
                    "WHERE tg.title_id = t.id AND tg.genre_id IN ($placeholders))",
            )
            genreIds.forEach { params.add(it) }
        }

        // Keyset continuation
        if (after != null) {
            where.add(keysetCondition(after, params))
        }

        val sql = buildQuery(where, limit + 1)
        val rows =
            dataSource.connection.use { conn ->
                conn.queryBound(sql, params) { rs -> mapRow(rs) }
            }

        val hasNext = rows.size > limit
        val page = if (hasNext) rows.dropLast(1) else rows
        val lastRow = page.lastOrNull()
        val keyset =
            lastRow?.let {
                CatalogCursor.Keyset(
                    releaseYear = it.releaseYear,
                    releaseMonth = it.releaseMonth,
                    releaseDay = it.releaseDay,
                    id = it.id,
                )
            }
        return TitlePage(page, keyset, hasNext)
    }

    fun findById(id: UUID): TitleRow? {
        val sql = "SELECT $TITLE_COLUMNS FROM title t WHERE t.id = ? AND t.status = 'ACTIVE'"
        return dataSource.connection.use { conn ->
            conn.queryBound(sql, listOf(id)) { rs -> mapRow(rs) }.firstOrNull()
        }
    }

    fun findGenreIds(titleId: UUID): List<UUID> {
        val sql = "SELECT genre_id FROM title_genre WHERE title_id = ? ORDER BY genre_id"
        return dataSource.connection.use { conn ->
            conn.queryBound(sql, listOf(titleId)) { rs -> rs.getObject("genre_id", UUID::class.java) }
        }
    }

    // ── keyset continuation ───────────────────────────────────────────────────

    /**
     * Keyset predicate for DESC NULLS LAST date ordering, ASC UUID tie-break.
     *
     * Ordering: (year DESC NULLS LAST, month DESC NULLS LAST, day DESC NULLS LAST, id ASC)
     * NULL in any date field means "unknown" and sorts LAST in each DESC column.
     *
     * After cursor at (y0, m0, d0, id0), a row (y, m, d, id) is on a later page when:
     *   - y < y0 (strictly earlier year → comes after y0 in DESC)
     *   - y IS NULL (null year always comes last)
     *   - y = y0 AND (m < m0 OR m IS NULL or [further keyset for same month])
     *
     * Key invariant: if a field is NULL in the cursor, all rows with that field NOT NULL
     * within the same parent context have already appeared (they sort before NULL).
     */
    internal fun keysetCondition(
        after: CatalogCursor.Keyset,
        params: MutableList<Any?>,
    ): String {
        val y = after.releaseYear
        val m = after.releaseMonth
        val d = after.releaseDay
        val id = after.id

        return when {
            y != null && m != null && d != null -> {
                // Full-date cursor: (y0, m0, d0, id0)
                params.addAll(listOf(y, y, m, y, y, m, d, y, m, y, m, d, id))
                """(t.release_year < ?
                   OR t.release_year IS NULL
                   OR (t.release_year = ? AND t.release_month < ?)
                   OR (t.release_year = ? AND t.release_month IS NULL)
                   OR (t.release_year = ? AND t.release_month = ? AND t.release_day < ?)
                   OR (t.release_year = ? AND t.release_month = ? AND t.release_day IS NULL)
                   OR (t.release_year = ? AND t.release_month = ? AND t.release_day = ? AND t.id > ?))"""
            }
            y != null && m != null -> {
                // Year+month cursor, unknown day: (y0, m0, null, id0)
                // Rows with day IS NOT NULL at (y0, m0) came before this cursor; exclude them.
                params.addAll(listOf(y, y, m, y, y, m, id))
                """(t.release_year < ?
                   OR t.release_year IS NULL
                   OR (t.release_year = ? AND t.release_month < ?)
                   OR (t.release_year = ? AND t.release_month IS NULL)
                   OR (t.release_year = ? AND t.release_month = ? AND t.release_day IS NULL AND t.id > ?))"""
            }
            y != null -> {
                // Year-only cursor, unknown month: (y0, null, null, id0)
                // Rows with month IS NOT NULL at y0 came before this cursor; exclude them.
                params.addAll(listOf(y, y, id))
                """(t.release_year < ?
                   OR t.release_year IS NULL
                   OR (t.release_year = ? AND t.release_month IS NULL AND t.id > ?))"""
            }
            else -> {
                // Unknown-year cursor: (null, null, null, id0)
                // All rows with a known year came before; only null-year rows after UUID.
                params.add(id)
                "(t.release_year IS NULL AND t.id > ?)"
            }
        }
    }

    private fun buildQuery(
        where: List<String>,
        limit: Int,
    ): String =
        """SELECT $TITLE_COLUMNS
           FROM title t
           WHERE ${where.joinToString(" AND ")}
           ORDER BY t.release_year DESC NULLS LAST,
                    t.release_month DESC NULLS LAST,
                    t.release_day DESC NULLS LAST,
                    t.id ASC
           LIMIT $limit"""

    private fun mapRow(rs: ResultSet): TitleRow {
        val rawCountries = rs.getArray("countries")?.array
        val countries = (rawCountries as? Array<*>)?.map { it.toString() } ?: emptyList()
        return TitleRow(
            id = rs.getObject("id", UUID::class.java),
            kind = rs.getString("kind"),
            status = rs.getString("status"),
            displayTitle = rs.getString("display_title"),
            originalTitle = rs.getString("original_title"),
            synopsis = rs.getString("synopsis"),
            synopsisLanguage = rs.getString("synopsis_language"),
            originalLanguage = rs.getString("original_language"),
            countries = countries,
            releaseYear = rs.getObject("release_year") as? Int,
            releaseMonth = rs.getObject("release_month") as? Int,
            releaseDay = rs.getObject("release_day") as? Int,
            releaseContext = rs.getString("release_context"),
            runtimeMinutes = rs.getObject("runtime_minutes") as? Int,
            tvStatus = rs.getString("tv_status"),
            endYear = rs.getObject("end_year") as? Int,
            endMonth = rs.getObject("end_month") as? Int,
            endDay = rs.getObject("end_day") as? Int,
            assetId = rs.getObject("asset_id")?.let { UUID.fromString(it.toString()) },
            version = rs.getLong("version"),
        )
    }



    companion object {
        const val TITLE_COLUMNS =
            "t.id, t.kind, t.status, t.display_title, t.original_title, t.synopsis, " +
                "t.synopsis_language, t.original_language, t.countries, t.release_year, " +
                "t.release_month, t.release_day, t.release_context, t.runtime_minutes, " +
                "t.tv_status, t.end_year, t.end_month, t.end_day, t.asset_id, t.version"
    }
}
