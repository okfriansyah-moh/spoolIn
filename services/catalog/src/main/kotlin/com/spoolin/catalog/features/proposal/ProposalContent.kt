package com.spoolin.catalog.features.proposal

import spoolin.v1.Spoolin
import spoolin.v1.Spoolin.FieldSource
import spoolin.v1.Spoolin.MovieValues
import spoolin.v1.Spoolin.Proposal
import tools.jackson.databind.json.JsonMapper

object ProposalContent {
    private val mapper = JsonMapper.builder().build()

    fun fromProposal(request: Proposal): Map<String, Any?> =
        buildMap { putAll(mapOf(
            "policy_revision" to request.context.policyRevision,
            "values" to valuesMap(request.values),
            "sources" to request.sourcesList.map(::sourceMap),
        )); if (request.hasPosterRef()) put("poster_ref",request.posterRef)
            if (request.creditsCount > 0) put("credits", request.creditsList.map(ProposalCredits::content)) }

    fun credits(content: Map<String, Any?>): List<Spoolin.ProposalCredit> {
        @Suppress("UNCHECKED_CAST")
        return (content["credits"] as? List<Map<String, Any?>> ?: emptyList()).map(ProposalCredits::fromContent)
    }

    fun poster(content: Map<String,Any?>): Spoolin.PreparedPoster? {
        @Suppress("UNCHECKED_CAST")
        val map = content["poster"] as? Map<String,Any?> ?: return null
        return posterFromMap(map)
    }

    fun sources(content: Map<String,Any?>): List<FieldSource> {
        @Suppress("UNCHECKED_CAST")
        return (content["sources"] as? List<Map<String,Any?>> ?: emptyList()).map { source ->
            FieldSource.newBuilder().setField(source["field"] as String).setUrl(source["url"] as String)
                .setReuseBasis(source["reuse_basis"] as String)
                .setRetrievedAt(com.google.protobuf.Timestamp.newBuilder().setSeconds((source["retrieved_at"] as Number).toLong()))
                .also { b -> (source["precision"] as? String)?.let(b::setPrecision) }.build()
        }
    }

    fun policyRevision(content: Map<String, Any?>): String = content["policy_revision"] as? String ?: ""

    fun parseJson(raw: String): Map<String, Any?> {
        @Suppress("UNCHECKED_CAST")
        return mapper.readValue(raw, Map::class.java) as Map<String, Any?>
    }

    fun movieValues(content: Map<String, Any?>): MovieValues {
        @Suppress("UNCHECKED_CAST")
        val values = content["values"] as? Map<String, Any?> ?: throw IllegalArgumentException("values missing")
        val builder = MovieValues.newBuilder()
        builder.title = requireNotNull(values["title"] as? String) { "title missing" }
        builder.synopsis = requireNotNull(values["synopsis"] as? String) { "synopsis missing" }
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
        val release = releaseMap(values)
        return buildMap {
            put("title", values.title)
            put("original_title", values.originalTitle.takeIf { values.hasOriginalTitle() })
            put("synopsis", values.synopsis)
            put("synopsis_language", values.synopsisLanguage)
            put("original_language", values.originalLanguage.takeIf { values.hasOriginalLanguage() })
            put("runtime_minutes", values.runtimeMinutes.takeIf { values.hasRuntimeMinutes() })
            put("genre_ids", values.genreIdsList.sorted())
            put("countries", values.countriesList.sorted())
            put("release", release)
        }.entries.sortedBy { it.key }.associate { it.key to it.value }
    }

    private fun releaseMap(values: MovieValues): Map<String, Int>? =
        if (values.hasRelease()) {
            buildMap {
                put("year", values.release.year)
                if (values.release.hasMonth()) put("month", values.release.month)
                if (values.release.hasDay()) put("day", values.release.day)
            }
        } else {
            null
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
