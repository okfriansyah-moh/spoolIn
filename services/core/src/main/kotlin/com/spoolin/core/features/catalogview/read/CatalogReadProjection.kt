package com.spoolin.core.features.catalogview.read

import spoolin.v1.Spoolin
import java.time.Instant

internal object CatalogReadProjection {
    fun titlePage(proto: Spoolin.TitlePage, management: Boolean): Map<String, Any?> =
        page(proto.nodesList.map { title(it, management) }, proto.endCursor, proto.hasNextPage)

    fun personPage(proto: Spoolin.PersonPage): Map<String, Any?> =
        page(proto.nodesList.map(::person), proto.endCursor, proto.hasNextPage)

    fun creditPage(proto: Spoolin.CreditPage): Map<String, Any?> =
        page(proto.nodesList.map(::credit), proto.endCursor, proto.hasNextPage)

    fun page(nodes: List<Map<String, Any?>>, endCursor: String, hasNextPage: Boolean): Map<String, Any?> =
        mapOf(
            "nodes" to nodes,
            "pageInfo" to mapOf("endCursor" to endCursor.takeIf { it.isNotEmpty() }, "hasNextPage" to hasNextPage),
        )

    fun title(t: Spoolin.Title, management: Boolean): Map<String, Any?> =
        mapOf(
            "id" to t.id,
            "kind" to t.kind,
            "state" to t.state,
            "title" to t.values.title,
            "originalTitle" to t.values.originalTitle.takeIf { it.isNotEmpty() },
            "synopsis" to t.values.synopsis,
            "synopsisLanguage" to t.values.synopsisLanguage,
            "originalLanguage" to t.values.originalLanguage.takeIf { it.isNotEmpty() },
            "countries" to t.values.countriesList,
            "genres" to t.values.genreIdsList.map { mapOf("id" to it) },
            "release" to partialDate(t.values.release),
            "runtimeMinutes" to t.values.runtimeMinutes.takeIf { it > 0 },
            "tvStatus" to t.tvStatus.takeIf { it.isNotEmpty() },
            "tvEnd" to partialDate(t.tvEnd),
            "releaseContext" to t.releaseContext.takeIf { it.isNotEmpty() },
            "provenance" to t.provenanceList.map { provenance(it) },
            "_artworkAssetId" to t.assetId.takeIf { it.isNotBlank() },
            "_artworkManagement" to management,
            "version" to t.version.toString(),
        )

    fun person(p: Spoolin.Person): Map<String, Any?> =
        mapOf(
            "id" to p.id,
            "name" to p.values.name,
            "biography" to p.values.biography.takeIf { it.isNotEmpty() },
            "state" to p.state,
            "version" to p.version.toString(),
        )

    fun credit(c: Spoolin.Credit): Map<String, Any?> =
        mapOf(
            "id" to c.id,
            "personId" to c.personId,
            "personName" to c.personName,
            "titleId" to c.titleId,
            "titleName" to c.titleName,
            "role" to
                mapOf(
                    "id" to c.role.id,
                    "label" to c.role.label,
                    "category" to c.role.category,
                    "allowsCharacters" to c.role.allowsCharacters,
                ),
            "characters" to c.charactersList,
            "version" to c.version.toString(),
        )

    private fun partialDate(pd: Spoolin.PartialDate): Map<String, Any?> =
        mapOf(
            "year" to pd.year.takeIf { pd.hasYear() },
            "month" to pd.month.takeIf { pd.hasMonth() },
            "day" to pd.day.takeIf { pd.hasDay() },
            "precision" to
                when {
                    pd.hasDay() -> "DAY"
                    pd.hasMonth() -> "MONTH"
                    pd.hasYear() -> "YEAR"
                    else -> "UNKNOWN"
                },
        )

    private fun provenance(p: Spoolin.Provenance): Map<String, Any?> =
        mapOf(
            "source" to p.source,
            "externalId" to p.externalId,
            "recordUrl" to p.recordUrl.takeIf { it.isNotEmpty() },
            "retrievedAt" to Instant.ofEpochSecond(p.retrievedAt.seconds, p.retrievedAt.nanos.toLong()).toString(),
            "reuseNote" to p.reuseNote,
            "fieldSources" to CatalogMetadataProjection.fieldSources(p),
            "locallyChanged" to p.locallyChangedList,
        )
}
