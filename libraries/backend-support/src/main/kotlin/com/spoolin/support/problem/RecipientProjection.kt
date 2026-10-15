package com.spoolin.support.problem

import com.spoolin.support.problem.ProjectionValues.fields
import com.spoolin.support.problem.ProjectionValues.objectValue
import com.spoolin.support.problem.ProjectionValues.objects
import com.spoolin.support.problem.ProjectionValues.strings

class ProjectionShape(
    private val fields: Map<String, (Any?) -> Any?>,
    private val nullable: Set<String> = emptySet(),
) {
    fun project(input: Map<String, Any?>): Map<String, Any?> =
        fields.mapValues { (key, project) ->
            val value = project(input[key])
            require(value != null || key in nullable) { "Required public field is absent" }
            value
        }
}

object RecipientProjection {
    private val publicFields =
        setOf(
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
            "name",
            "biography",
            "characters",
            "roleId",
            "state",
            "version",
        )
    private val nullableTitle =
        setOf("originalTitle", "runtimeMinutes", "originalLanguage", "tvStatus", "releaseContext", "artwork")

    private val date = ProjectionShape(fields("year", "month", "day", "precision"), setOf("year", "month", "day"))
    private val inputDate = ProjectionShape(fields("year", "month", "day"), setOf("year", "month", "day"))
    private val genre = ProjectionShape(fields("id", "label"))
    private val role = ProjectionShape(fields("id", "label", "category", "allowsCharacters"))
    private val artwork = ProjectionShape(fields("assetId", "url", "width", "height"))
    private val evidence = ProjectionShape(fields("url", "retrievedAt", "reuseBasis", "precision"), setOf("precision"))
    private val page = ProjectionShape(fields("endCursor", "hasNextPage"), setOf("endCursor"))
    private val provenance =
        ProjectionShape(
            fields("source", "externalId", "recordUrl", "retrievedAt", "reuseNote", "locallyChanged") +
                ("fieldSources" to { value: Any? -> evidenceFields(value) }),
            setOf("recordUrl"),
        )
    private val credit =
        ProjectionShape(
            fields("id", "personId", "personName", "titleId", "titleName", "characters", "version") +
                ("role" to { value: Any? -> objectValue(value, role) }),
        )
    private val credits =
        ProjectionShape(
            mapOf(
                "nodes" to { value: Any? -> objects(value, credit) },
                "pageInfo" to { value: Any? -> objectValue(value, page) },
            ),
        )
    private val title =
        ProjectionShape(
            fields(
                "id",
                "kind",
                "state",
                "title",
                "originalTitle",
                "synopsis",
                "runtimeMinutes",
                "originalLanguage",
                "synopsisLanguage",
                "countries",
                "tvStatus",
                "releaseContext",
                "version",
            ) +
                mapOf(
                    "genres" to { value: Any? -> objects(value, genre) },
                    "release" to { value: Any? -> objectValue(value, date) },
                    "tvEnd" to { value: Any? -> objectValue(value, date) },
                    "artwork" to { value: Any? -> objectValue(value, artwork) },
                    "provenance" to { value: Any? -> objects(value, provenance) },
                    "credits" to { value: Any? -> objectValue(value, credits) },
                ),
            nullableTitle,
        )

    fun publicTitle(input: Map<String, Any?>): Map<String, Any?> {
        require(input["state"] == "ACTIVE")
        return title.project(input)
    }

    fun ownSession(
        input: Map<String, Any?>,
        ownSession: Boolean,
    ): Map<String, Any?> {
        require(ownSession)
        return ProjectionShape(
            fields("authenticated", "role", "accountId", "displayName", "expiresAt", "csrfToken"),
            setOf("role", "accountId", "displayName", "expiresAt"),
        ).project(input)
    }

    fun maintainerAudit(
        input: Map<String, Any?>,
        maintainer: Boolean,
    ): Map<String, Any?> {
        require(maintainer)
        require(input["outcome"] in setOf("SUCCEEDED", "DENIED", "FAILED", "APPROVED", "REJECTED"))
        val shape =
            ProjectionShape(
                fields("id", "actorRef", "action", "subjectId", "operationId", "at", "outcome", "subjectKind") +
                    ("changes" to { value: Any? -> changes(value) }),
                setOf("subjectId", "operationId"),
            )
        return shape.project(input)
    }

    fun modelProposal(input: Map<String, Any?>): Map<String, Any?> {
        require(input["status"] == "REVIEW")
        val shape =
            ProjectionShape(
                fields(
                    "proposalId",
                    "revision",
                    "contentHash",
                    "expiresAt",
                    "policyRevision",
                    "status",
                    "reviewResourceUri",
                ) +
                    ("normalizedMovie" to { value: Any? -> movie(value) }),
            )
        return shape.project(input)
    }

    private fun evidenceFields(value: Any?): Map<String, Any?> {
        require(value is Map<*, *>)
        return value.entries.filter { it.key in publicFields }.associate {
            it.key.toString() to
                objectValue(it.value, evidence)
        }
    }

    private fun changes(value: Any?): Map<String, Any?> {
        require(value is Map<*, *>)
        val pair =
            ProjectionShape(
                mapOf(
                    "before" to { item: Any? ->
                        changedValue(item)
                    },
                    "after" to { item: Any? -> changedValue(item) },
                ),
                setOf("before", "after"),
            )
        return value.entries.filter { it.key in publicFields }.associate {
            it.key.toString() to
                objectValue(it.value, pair)
        }
    }

    private fun changedValue(value: Any?): Any? =
        when (value) {
            is Map<*, *> -> objectValue(value, date)
            is List<*> -> strings(value)
            null, is String, is Int, is Long, is Boolean -> value
            else -> throw IllegalArgumentException("Supply a typed public change.")
        }

    private fun movie(value: Any?): Any? =
        objectValue(
            value,
            ProjectionShape(
                fields(
                    "title",
                    "originalTitle",
                    "synopsis",
                    "genreIds",
                    "runtimeMinutes",
                    "originalLanguage",
                    "synopsisLanguage",
                    "countries",
                ) +
                    ("release" to { item: Any? -> objectValue(item, inputDate) }),
                setOf("originalTitle", "runtimeMinutes", "originalLanguage"),
            ),
        )
}
