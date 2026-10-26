package com.spoolin.catalog.features.search

import java.sql.ResultSet

internal data class TitleContent(
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
    val runtimeMinutes: Int?,
    val version: Long,
) {
    companion object {
        fun read(rs: ResultSet): TitleContent {
            val rawCountries = rs.getArray("countries")?.array
            val countries = (rawCountries as? Array<*>)?.map { it.toString() } ?: emptyList()
            return TitleContent(
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
                runtimeMinutes = rs.getObject("runtime_minutes") as? Int,
                version = rs.getLong("version"),
            )
        }

    }
}
