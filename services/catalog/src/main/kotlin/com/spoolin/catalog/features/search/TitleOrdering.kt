package com.spoolin.catalog.features.search

import java.sql.Timestamp
import java.time.Instant

/** Public title order and its matching keyset predicate share the same columns. */
enum class TitleOrdering {
    RELEASE_DESC,
    RELEASE_ASC,
    TITLE_ASC,
    TITLE_DESC,
    ADDED_DESC,
    ADDED_ASC,
    ;

    private val ascending: Boolean
        get() = this in setOf(RELEASE_ASC, TITLE_ASC, ADDED_ASC)

    private fun columns(key: CatalogCursor.Keyset): List<Pair<String, Any?>> =
        when (this) {
            RELEASE_ASC, RELEASE_DESC ->
                listOf(
                    "t.release_year" to key.releaseYear,
                    "t.release_month" to key.releaseMonth,
                    "t.release_day" to key.releaseDay,
                )
            TITLE_ASC, TITLE_DESC -> listOf(TITLE_EXPRESSION to requireNotNull(key.sortValue))
            ADDED_ASC, ADDED_DESC ->
                listOf("t.created_at" to Timestamp.from(Instant.parse(requireNotNull(key.sortValue))))
        }

    fun orderBy(): String {
        val direction = if (ascending) "ASC" else "DESC"
        val fields =
            when (this) {
                RELEASE_ASC, RELEASE_DESC -> listOf("t.release_year", "t.release_month", "t.release_day")
                TITLE_ASC, TITLE_DESC -> listOf(TITLE_EXPRESSION)
                ADDED_ASC, ADDED_DESC -> listOf("t.created_at")
            }
        return fields.joinToString(", ") { "$it $direction NULLS LAST" } + ", t.id ASC"
    }

    fun after(
        key: CatalogCursor.Keyset,
        params: MutableList<Any?>,
    ): String {
        val fields = columns(key) + ("t.id" to key.id)
        val branches = fields.indices.mapNotNull { index -> branch(fields, index, params) }
        return "(" + branches.joinToString(" OR ") + ")"
    }

    private fun branch(
        fields: List<Pair<String, Any?>>,
        index: Int,
        params: MutableList<Any?>,
    ): String? {
        val (column, value) = fields[index]
        if (value == null) return null
        val equals =
            fields.take(index).map { (field, prior) ->
                if (prior == null) "$field IS NULL" else "$field = ${placeholder(field)}".also { params.add(prior) }
            }
        params.add(value)
        val operator = if (ascending || column == "t.id") ">" else "<"
        val comparison =
            if (column == "t.id") {
                "$column $operator ?"
            } else {
                "($column $operator ${placeholder(column)} OR $column IS NULL)"
            }
        return (equals + comparison).joinToString(" AND ", "(", ")")
    }

    private fun placeholder(column: String): String =
        if (column == TITLE_EXPRESSION) "casefold(? COLLATE pg_unicode_fast)" else "?"

    fun value(row: TitleSearchSql.TitleRow): String? =
        when (this) {
            TITLE_ASC, TITLE_DESC -> row.displayTitle
            ADDED_ASC, ADDED_DESC -> row.createdAt.toString()
            else -> null
        }

    companion object {
        const val TITLE_EXPRESSION = "casefold(t.display_title COLLATE pg_unicode_fast)"

        fun parse(raw: String?): TitleOrdering = if (raw == null) RELEASE_DESC else valueOf(raw)
    }
}
