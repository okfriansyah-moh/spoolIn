@file:Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod")

package com.spoolin.catalog.features.search

import com.spoolin.support.policy.PolicyEnvironment
import com.spoolin.support.policy.PublicPolicy
import com.spoolin.support.policy.ReadPolicy
import com.spoolin.support.receipts.queryBound
import java.sql.Connection
import java.sql.ResultSet
import java.util.UUID
import javax.sql.DataSource

private const val GENRE_BATCH_SIZE = 100

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
        val createdAt: java.time.Instant = java.time.Instant.EPOCH,
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
        sort: TitleOrdering = TitleOrdering.RELEASE_DESC,
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
            where.add(sort.after(resolveTitleAnchor(after, sort), params))
        }

        val sql = buildQuery(where, limit + 1, sort)
        val rows =
            dataSource.connection.use { conn ->
                conn.queryBound(sql, params) { rs -> readRow(rs) }
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
                    sortValue = sort.value(it),
                )
            }
        return TitlePage(page, keyset, hasNext)
    }

    fun findById(id: UUID): TitleRow? {
        val sql = "SELECT $TITLE_COLUMNS FROM title t WHERE t.id = ? AND t.status = 'ACTIVE'"
        return dataSource.connection.use { conn ->
            conn.queryBound(sql, listOf(id)) { rs -> readRow(rs) }.firstOrNull()
        }
    }

    fun findGenreIds(titleId: UUID): List<UUID> {
        val sql = "SELECT genre_id FROM title_genre WHERE title_id = ? ORDER BY genre_id"
        return dataSource.connection.use { conn ->
            conn.queryBound(sql, listOf(titleId)) { rs -> rs.getObject("genre_id", UUID::class.java) }
        }
    }

    fun findGenresForTitles(titleIds: List<UUID>): Map<UUID, List<UUID>> {
        if (titleIds.isEmpty()) return emptyMap()
        return dataSource.connection.use { connection ->
            titleIds
                .distinct()
                .chunked(GENRE_BATCH_SIZE)
                .flatMap { batch ->
                    val placeholders = batch.joinToString(",") { "?" }
                    connection.queryBound(
                        "SELECT title_id,genre_id FROM title_genre WHERE title_id IN ($placeholders) " +
                            "ORDER BY title_id,genre_id",
                        batch,
                    ) { row ->
                        row.getObject("title_id", UUID::class.java) to row.getObject("genre_id", UUID::class.java)
                    }
                }.groupBy({ it.first }, { it.second })
        }
    }

    private fun resolveTitleAnchor(
        key: CatalogCursor.Keyset,
        sort: TitleOrdering,
    ): CatalogCursor.Keyset {
        if (key.sortHash == null) return key
        require(sort == TitleOrdering.TITLE_ASC || sort == TitleOrdering.TITLE_DESC) { "INVALID_CURSOR" }
        val title = findById(key.id)?.displayTitle
        require(title != null && TitleCursorValue.hash(title) == key.sortHash) { "INVALID_CURSOR" }
        return key.copy(sortValue = title, sortHash = null)
    }

    internal fun keysetCondition(
        after: CatalogCursor.Keyset,
        params: MutableList<Any?>,
    ): String = TitleOrdering.RELEASE_DESC.after(after, params)

    private fun buildQuery(
        where: List<String>,
        limit: Int,
        sort: TitleOrdering,
    ): String =
        """SELECT $TITLE_COLUMNS
           FROM title t
           WHERE ${where.joinToString(" AND ")}
           ORDER BY ${sort.orderBy()}
           LIMIT $limit"""

    companion object {
        internal fun readRow(rs: ResultSet): TitleRow {
            val content = TitleContent.read(rs)
            return TitleRow(
                id = rs.getObject("id", UUID::class.java),
                kind = content.kind,
                status = content.status,
                displayTitle = content.displayTitle,
                originalTitle = content.originalTitle,
                synopsis = content.synopsis,
                synopsisLanguage = content.synopsisLanguage,
                originalLanguage = content.originalLanguage,
                countries = content.countries,
                releaseYear = content.releaseYear,
                releaseMonth = content.releaseMonth,
                releaseDay = content.releaseDay,
                releaseContext = rs.getString("release_context"),
                runtimeMinutes = content.runtimeMinutes,
                tvStatus = rs.getString("tv_status"),
                endYear = rs.getObject("end_year") as? Int,
                endMonth = rs.getObject("end_month") as? Int,
                endDay = rs.getObject("end_day") as? Int,
                assetId = rs.getObject("asset_id")?.let { UUID.fromString(it.toString()) },
                version = content.version,
                createdAt = rs.getTimestamp("created_at")?.toInstant() ?: java.time.Instant.EPOCH,
            )
        }

        const val TITLE_COLUMNS =
            "t.id, t.kind, t.status, t.display_title, t.original_title, t.synopsis, " +
                "t.synopsis_language, t.original_language, t.countries, t.release_year, " +
                "t.release_month, t.release_day, t.release_context, t.runtime_minutes, " +
                "t.tv_status, t.end_year, t.end_month, t.end_day, t.asset_id, t.version, t.created_at"
    }
}
