package com.spoolin.catalog.features.proposal

import spoolin.v1.Spoolin
import spoolin.v1.Spoolin.FieldSource
import spoolin.v1.Spoolin.MovieValues
import spoolin.v1.Spoolin.Proposal
import tools.jackson.databind.json.JsonMapper

object ProposalContent {
    private val mapper = JsonMapper.builder().build()

    fun fromProposal(request: Proposal): Map<String, Any?> =
        mapOf(
            "policy_revision" to request.context.policyRevision,
            "values" to valuesMap(request.values),
            "sources" to request.sourcesList.map(::sourceMap).sortedBy { it["field"] as String },
        )

    fun policyRevision(content: Map<String, Any?>): String =
        content["policy_revision"] as? String ?: ""

    fun parseJson(raw: String): Map<String, Any?> {
        @Suppress("UNCHECKED_CAST")
        return mapper.readValue(raw, Map::class.java) as Map<String, Any?>
    }

    fun movieValues(content: Map<String, Any?>): MovieValues {
        @Suppress("UNCHECKED_CAST")
        val values = content["values"] as? Map<String, Any?> ?: throw IllegalArgumentException("values missing")
        val builder = MovieValues.newBuilder()
        builder.title = values["title"] as? String ?: throw IllegalArgumentException("title missing")
        builder.synopsis = values["synopsis"] as? String ?: throw IllegalArgumentException("synopsis missing")
        builder.synopsisLanguage = values["synopsis_language"] as? String ?: "en"
        (values["original_title"] as? String)?.let(builder::setOriginalTitle)
        (values["original_language"] as? String)?.let(builder::setOriginalLanguage)
        (values["runtime_minutes"] as? Number)?.toInt()?.let(builder::setRuntimeMinutes)
        @Suppress("UNCHECKED_CAST")
        (values["genre_ids"] as? List<String>)?.let(builder::addAllGenreIds)
        @Suppress("UNCHECKED_CAST")
        (values["countries"] as? List<String>)?.let(builder::addAllCountries)
        @Suppress("UNCHECKED_CAST")
        val release = values["release"] as? Map<String, Any?>
        if (release != null) {
            val partial =
                Spoolin.PartialDate
                    .newBuilder()
                    .setYear((release["year"] as Number).toInt())
            (release["month"] as? Number)?.toInt()?.let(partial::setMonth)
            (release["day"] as? Number)?.toInt()?.let(partial::setDay)
            builder.release = partial.build()
        }
        return builder.build()
    }

    private fun valuesMap(values: MovieValues): Map<String, Any?> {
        val release =
            if (values.hasRelease()) {
                buildMap {
                    put("year", values.release.year)
                    if (values.release.hasMonth()) put("month", values.release.month)
                    if (values.release.hasDay()) put("day", values.release.day)
                }
            } else {
                null
            }
        return buildMap {
            put("title", values.title)
            if (values.hasOriginalTitle()) put("original_title", values.originalTitle)
            put("synopsis", values.synopsis)
            put("synopsis_language", values.synopsisLanguage)
            if (values.hasOriginalLanguage()) put("original_language", values.originalLanguage)
            if (values.hasRuntimeMinutes()) put("runtime_minutes", values.runtimeMinutes)
            if (values.genreIdsCount > 0) put("genre_ids", values.genreIdsList.sorted())
            if (values.countriesCount > 0) put("countries", values.countriesList.sorted())
            if (release != null) put("release", release)
        }.entries.sortedBy { it.key }.associate { it.key to it.value }
    }

    private fun sourceMap(source: FieldSource): Map<String, Any?> =
        buildMap {
            put("field", source.field)
            put("url", source.url)
            put("reuse_basis", source.reuseBasis)
            put("retrieved_at", source.retrievedAt.seconds)
            if (source.hasPrecision()) put("precision", source.precision)
        }
}
