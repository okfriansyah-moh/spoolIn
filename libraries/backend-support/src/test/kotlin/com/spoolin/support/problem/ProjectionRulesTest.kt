package com.spoolin.support.problem

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ProjectionRulesTest {
    private val id = "c1bb0e81-232b-5f83-9d73-a5273dcac001"
    private val privateValues =
        mapOf(
            "email" to "synthetic-private-email",
            "password" to "synthetic-private-password",
            "token" to "synthetic-private-token",
            "ui_challenge" to "synthetic-private-challenge",
            "keyId" to "internal-key",
            "payload" to "internal-work",
        )
    private val unknown = mapOf("year" to null, "month" to null, "day" to null, "precision" to "UNKNOWN")
    private val title =
        mapOf(
            "id" to id,
            "kind" to "MOVIE",
            "state" to "ACTIVE",
            "title" to "The Last Light",
            "synopsis" to "A story",
            "version" to "5",
            "countries" to emptyList<String>(),
            "synopsisLanguage" to "und",
            "genres" to listOf(mapOf("id" to id, "label" to "Drama") + privateValues),
            "release" to (unknown + privateValues),
            "tvEnd" to unknown,
            "artwork" to null,
            "provenance" to emptyList<Map<String, Any?>>(),
            "credits" to
                mapOf("nodes" to emptyList<Any>(), "pageInfo" to mapOf("endCursor" to null, "hasNextPage" to false)),
        ) + privateValues
    private val movie =
        mapOf(
            "title" to "The Last Light",
            "synopsis" to "A story",
            "synopsisLanguage" to "und",
            "genreIds" to listOf(id),
            "release" to unknown,
            "countries" to emptyList<String>(),
        ) + privateValues

    @Test
    fun `ATDD-292 public Title filters nested internals with complete field set`() {
        val result = RecipientProjection.publicTitle(title)
        assertEquals(
            setOf(
                "id",
                "kind",
                "state",
                "title",
                "originalTitle",
                "synopsis",
                "genres",
                "release",
                "runtimeMinutes",
                "originalLanguage",
                "synopsisLanguage",
                "countries",
                "tvStatus",
                "tvEnd",
                "releaseContext",
                "provenance",
                "artwork",
                "credits",
                "version",
            ),
            result.keys,
        )
        assertEquals("The Last Light", result["title"])
        assertEquals(listOf(mapOf("id" to id, "label" to "Drama")), result["genres"])
        assertEquals(unknown, result["release"])
        assertFalse(result.toString().contains("synthetic-private"))
        assertThrows(IllegalArgumentException::class.java) {
            RecipientProjection.publicTitle(title + ("state" to "ARCHIVED"))
        }
        assertThrows(IllegalArgumentException::class.java) { RecipientProjection.publicTitle(title - "synopsis") }
    }

    @Test
    fun `ATDD-292 own Session keeps exact shape and owning display name`() {
        val result =
            RecipientProjection.ownSession(
                mapOf(
                    "authenticated" to true,
                    "displayName" to "V",
                    "csrfToken" to "anti-forgery-only",
                ) + privateValues,
                true,
            )
        assertEquals(setOf("authenticated", "role", "accountId", "displayName", "expiresAt", "csrfToken"), result.keys)
        assertEquals("V", result["displayName"])
        assertFalse(result.toString().contains("synthetic-private"))
        assertThrows(IllegalArgumentException::class.java) { RecipientProjection.ownSession(emptyMap(), false) }
    }

    @Test
    fun `ATDD-292 Maintainer Audit includes corrected outcome and subject`() {
        val input =
            mapOf(
                "id" to id,
                "actorRef" to "account-opaque",
                "action" to "TITLE_UPDATED",
                "at" to "2026-10-12T03:00:00Z",
                "outcome" to "SUCCEEDED",
                "subjectKind" to "TITLE",
                "changes" to mapOf("title" to mapOf("before" to "Old", "after" to "New"), "email" to privateValues),
            ) + privateValues
        val result = RecipientProjection.maintainerAudit(input, true)
        assertEquals(
            setOf("id", "actorRef", "action", "subjectId", "changes", "operationId", "at", "outcome", "subjectKind"),
            result.keys,
        )
        assertEquals("TITLE", result["subjectKind"])
        assertEquals("SUCCEEDED", result["outcome"])
        assertEquals(mapOf("title" to mapOf("before" to "Old", "after" to "New")), result["changes"])
        assertFalse(result.toString().contains("synthetic-private"))
        assertThrows(IllegalArgumentException::class.java) { RecipientProjection.maintainerAudit(emptyMap(), false) }
    }

    @Test
    fun `ATDD-292 model proposal omits challenge and preserves exact normalized movie keys`() {
        val input =
            mapOf(
                "proposalId" to id,
                "revision" to "5",
                "status" to "REVIEW",
                "normalizedMovie" to movie,
                "contentHash" to "a".repeat(64),
                "policyRevision" to "b".repeat(64),
                "expiresAt" to "2026-10-12T03:10:00Z",
                "reviewResourceUri" to "spoolin://proposal/$id/5",
            ) + privateValues
        val result = RecipientProjection.modelProposal(input)
        assertEquals(
            setOf(
                "proposalId",
                "revision",
                "contentHash",
                "expiresAt",
                "normalizedMovie",
                "policyRevision",
                "status",
                "reviewResourceUri",
            ),
            result.keys,
        )
        val expectedMovie =
            mapOf(
                "title" to "The Last Light",
                "originalTitle" to null,
                "synopsis" to "A story",
                "genreIds" to listOf(id),
                "release" to mapOf("year" to null, "month" to null, "day" to null),
                "runtimeMinutes" to null,
                "originalLanguage" to null,
                "synopsisLanguage" to "und",
                "countries" to emptyList<String>(),
            )
        assertEquals(expectedMovie, result["normalizedMovie"])
        assertFalse(result.toString().contains("synthetic-private"))
        assertEquals("REVIEW", result["status"])
        assertThrows(IllegalArgumentException::class.java) {
            RecipientProjection.modelProposal(
                mapOf("status" to "CREATED"),
            )
        }
    }
}
