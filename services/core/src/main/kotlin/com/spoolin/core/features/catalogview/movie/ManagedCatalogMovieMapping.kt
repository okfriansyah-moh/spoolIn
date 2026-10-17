package com.spoolin.core.features.catalogview.movie

import com.google.protobuf.FieldMask
import spoolin.v1.Spoolin

internal fun movieValuesFromInput(input: Map<String, Any?>): Spoolin.MovieValues =
    applyMovieCreateInput(Spoolin.MovieValues.newBuilder(), input).build()

private fun applyMovieCreateInput(
    builder: Spoolin.MovieValues.Builder,
    input: Map<String, Any?>,
): Spoolin.MovieValues.Builder {
    (input["title"] as? String)?.let { builder.title = it }
    (input["originalTitle"] as? String)?.let { builder.originalTitle = it }
    (input["synopsis"] as? String)?.let { builder.synopsis = it }
    (input["synopsisLanguage"] as? String)?.let { builder.synopsisLanguage = it }
    (input["originalLanguage"] as? String)?.let { builder.originalLanguage = it }
    @Suppress("UNCHECKED_CAST")
    (input["countries"] as? List<String>)?.let { builder.addAllCountries(it) }
    return applyMovieCreateInputTail(builder, input)
}

private fun applyMovieCreateInputTail(
    builder: Spoolin.MovieValues.Builder,
    input: Map<String, Any?>,
): Spoolin.MovieValues.Builder {
    @Suppress("UNCHECKED_CAST")
    (input["genreIds"] as? List<String>)?.let { builder.addAllGenreIds(it) }
    (input["release"] as? Map<*, *>)?.let { builder.setRelease(partialDateFromInput(it)) }
    (input["runtimeMinutes"] as? Int)?.let { builder.runtimeMinutes = it }
    return builder
}

internal fun moviePatchFromInput(patch: Map<String, Any?>): Pair<Spoolin.MovieValues, FieldMask> {
    val paths = mutableListOf<String>()
    val builder = Spoolin.MovieValues.newBuilder()
    applyMovieStringPatch(patch, "title", "values.title", builder::setTitle, paths)
    applyMovieStringPatch(patch, "originalTitle", "values.original_title", builder::setOriginalTitle, paths)
    applyMovieStringPatch(patch, "synopsis", "values.synopsis", builder::setSynopsis, paths)
    applyMovieStringPatch(patch, "synopsisLanguage", "values.synopsis_language", builder::setSynopsisLanguage, paths)
    applyMovieStringPatch(patch, "originalLanguage", "values.original_language", builder::setOriginalLanguage, paths)
    if (patch.containsKey("countries")) {
        @Suppress("UNCHECKED_CAST")
        (patch["countries"] as? List<String>)?.let { builder.addAllCountries(it) }
        paths.add("values.countries")
    }
    if (patch.containsKey("genreIds")) {
        @Suppress("UNCHECKED_CAST")
        (patch["genreIds"] as? List<String>)?.let { builder.addAllGenreIds(it) }
        paths.add("values.genre_ids")
    }
    if (patch.containsKey("release")) {
        (patch["release"] as? Map<*, *>)?.let { builder.setRelease(partialDateFromInput(it)) }
        paths.add("values.release")
    }
    if (patch.containsKey("runtimeMinutes")) {
        (patch["runtimeMinutes"] as? Int)?.let { builder.runtimeMinutes = it }
        paths.add("values.runtime_minutes")
    }
    val mask = FieldMask.newBuilder().addAllPaths(paths).build()
    return Pair(builder.build(), mask)
}

private fun applyMovieStringPatch(
    patch: Map<String, Any?>,
    key: String,
    path: String,
    assign: (String) -> Unit,
    paths: MutableList<String>,
) {
    if (!patch.containsKey(key)) return
    (patch[key] as? String)?.let(assign)
    paths.add(path)
}

internal fun partialDateFromInput(map: Map<*, *>): Spoolin.PartialDate {
    val builder = Spoolin.PartialDate.newBuilder()
    (map["year"] as? Int)?.let { builder.year = it }
    (map["month"] as? Int)?.let { builder.month = it }
    (map["day"] as? Int)?.let { builder.day = it }
    return builder.build()
}
