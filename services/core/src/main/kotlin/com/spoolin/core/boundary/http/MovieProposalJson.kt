package com.spoolin.core.boundary.http

import spoolin.v1.Spoolin
import java.time.Instant

internal object MovieProposalJson {
    fun revision(value: Spoolin.ProposalRevision): Map<String, Any?> = mapOf(
        "proposalId" to value.proposalId, "revision" to value.revision, "contentHash" to value.contentHash,
        "challenge" to value.uiChallenge.toStringUtf8(),
        "expiresAt" to Instant.ofEpochSecond(value.expiresAt.seconds, value.expiresAt.nanos.toLong()).toString(),
        "state" to value.state, "operationId" to value.operationId,
        "values" to values(value.values), "poster" to if (value.hasPoster()) poster(value.poster) else null,
        "credits" to value.creditsList.map { credit -> mapOf(
            "personId" to credit.personId.takeIf { credit.hasPersonId() },
            "personName" to credit.personName.takeIf { credit.hasPersonName() },
            "personLabel" to credit.personLabel.takeIf { credit.hasPersonLabel() },
            "roleId" to credit.roleId,
            "roleLabel" to credit.roleLabel.takeIf { credit.hasRoleLabel() },
            "characterNames" to credit.characterNamesList,
            "billingOrder" to credit.billingOrder.takeIf { credit.hasBillingOrder() },
        ) },
        "sources" to value.sourcesList.map { mapOf("field" to it.field, "url" to it.url, "note" to it.reuseBasis) },
    )

    fun poster(value: Spoolin.PreparedPoster): Map<String, Any?> = mapOf(
        "refId" to value.refId, "assetId" to value.assetId, "contentHash" to value.contentHash,
        "url" to "/api/movie-posters/${value.refId}", "mediaType" to value.mediaType,
        "width" to value.width, "height" to value.height, "state" to value.state,
        "sourceUrl" to value.sourceUrl, "reuseBasis" to value.reuseBasis,
    )

    private fun values(value: Spoolin.MovieValues): Map<String, Any?> = mapOf(
        "title" to value.title, "originalTitle" to value.originalTitle.takeIf { value.hasOriginalTitle() },
        "synopsis" to value.synopsis, "genreIds" to value.genreIdsList,
        "release" to mapOf("year" to value.release.year, "month" to value.release.month.takeIf { value.release.hasMonth() },
            "day" to value.release.day.takeIf { value.release.hasDay() }),
        "runtimeMinutes" to value.runtimeMinutes.takeIf { value.hasRuntimeMinutes() },
        "originalLanguage" to value.originalLanguage.takeIf { value.hasOriginalLanguage() },
        "synopsisLanguage" to value.synopsisLanguage.takeIf { value.hasSynopsisLanguage() }, "countries" to value.countriesList,
    )

    fun outcome(value: Spoolin.Outcome): Map<String, Any?> = mapOf(
        "operationId" to value.operationId, "state" to value.state, "resultExpired" to value.resultExpired,
        "resultId" to value.resultId.takeIf { value.hasResultId() }, "version" to value.version.takeIf { value.hasVersion() },
        "problem" to if (value.hasProblem()) mapOf("code" to value.problem.code,
            "message" to "The request could not be completed.", "fields" to emptyList<Any>(), "retryAfterMs" to null) else null,
    )
}
