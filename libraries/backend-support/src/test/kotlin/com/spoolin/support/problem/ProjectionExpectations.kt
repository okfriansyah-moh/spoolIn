package com.spoolin.support.problem

internal object ProjectionExpectations {
    private const val ID = "c1bb0e81-232b-5f83-9d73-a5273dcac001"
    private val unknown = mapOf("year" to null, "month" to null, "day" to null, "precision" to "UNKNOWN")
    val title =
        mapOf(
            "id" to ID,
            "kind" to "MOVIE",
            "state" to "ACTIVE",
            "title" to "The Last Light",
            "originalTitle" to null,
            "synopsis" to "A story",
            "genres" to listOf(mapOf("id" to ID, "label" to "Drama")),
            "release" to unknown,
            "runtimeMinutes" to null,
            "originalLanguage" to null,
            "synopsisLanguage" to "und",
            "countries" to emptyList<String>(),
            "tvStatus" to null,
            "tvEnd" to unknown,
            "releaseContext" to null,
            "provenance" to emptyList<Any>(),
            "artwork" to null,
            "credits" to
                mapOf("nodes" to emptyList<Any>(), "pageInfo" to mapOf("endCursor" to null, "hasNextPage" to false)),
            "version" to "5",
        )
    val session =
        mapOf(
            "authenticated" to true,
            "role" to null,
            "accountId" to null,
            "displayName" to "V",
            "expiresAt" to null,
            "csrfToken" to "anti-forgery-only",
        )
    val audit =
        mapOf(
            "id" to ID,
            "actorRef" to "account-opaque",
            "action" to "TITLE_UPDATED",
            "subjectId" to null,
            "changes" to mapOf("title" to mapOf("before" to "Old", "after" to "New")),
            "operationId" to null,
            "at" to "2026-10-12T03:00:00Z",
            "outcome" to "SUCCEEDED",
            "subjectKind" to "TITLE",
        )
    val model =
        mapOf(
            "proposalId" to ID,
            "revision" to "5",
            "contentHash" to "a".repeat(64),
            "expiresAt" to "2026-10-12T03:10:00Z",
            "normalizedMovie" to
                mapOf(
                    "title" to "The Last Light",
                    "originalTitle" to null,
                    "synopsis" to "A story",
                    "genreIds" to listOf(ID),
                    "release" to mapOf("year" to null, "month" to null, "day" to null),
                    "runtimeMinutes" to null,
                    "originalLanguage" to null,
                    "synopsisLanguage" to "und",
                    "countries" to emptyList<String>(),
                ),
            "policyRevision" to "b".repeat(64),
            "status" to "REVIEW",
            "reviewResourceUri" to "spoolin://proposal/$ID/5",
        )
}
