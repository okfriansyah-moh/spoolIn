package com.spoolin.core.features.catalogview.read

import spoolin.v1.Spoolin

internal fun buildTitleSearchRequest(
    filter: Map<String, Any?>,
    first: Int,
    after: String?,
    validateSearchText: (String) -> Unit,
    normalizeSearchText: (String) -> String? = { it },
): Spoolin.Search {
    val text = (filter["text"] as? String)?.also { validateSearchText(it) }?.let(normalizeSearchText)
    val kind = filter["kind"] as? String
    val genreIds = (filter["genreIds"] as? List<*>)?.map { it.toString() } ?: emptyList()
    val year = filter["year"] as? Int
    val month = filter["month"] as? Int
    return Spoolin.Search
        .newBuilder()
        .setFirst(first)
        .addAllGenreIds(genreIds)
        .also { builder ->
            text?.let { builder.text = it }
            kind?.let { builder.kind = it }
            year?.let { builder.year = it }
            month?.let { builder.month = it }
            after?.let { builder.after = it }
        }.build()
}
