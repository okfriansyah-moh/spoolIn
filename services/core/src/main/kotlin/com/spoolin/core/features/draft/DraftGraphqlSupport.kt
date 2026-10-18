package com.spoolin.core.features.draft

import com.spoolin.support.receipts.ReceiptOutcome
import tools.jackson.databind.ObjectMapper
import java.util.UUID

internal fun draftGraphqlProjection(draft: DraftProjection): Map<String, Any?> =
    mapOf(
        "id" to draft.id.toString(),
        "targetKind" to draft.targetKind,
        "targetId" to draft.targetId?.toString(),
        "baseVersion" to draft.baseVersion?.toString(),
        "values" to ObjectMapper().readValue(draft.values, Map::class.java),
        "operationId" to draft.operationId?.toString(),
        "version" to draft.version.toString(),
        "expiresAt" to draft.expiresAt.toString(),
    )

internal fun saveDraftMutationProjection(outcome: ReceiptOutcome): Map<String, Any?> =
    mapOf(
        "operation" to
            mapOf(
                "id" to outcome.id.toString(),
                "state" to outcome.state.name,
                "resultId" to outcome.result?.resultId?.toString(),
                "version" to outcome.result?.version?.toString(),
                "resultExpired" to outcome.resultExpired,
                "problem" to null,
            ),
        "problem" to draftProblemProjection(outcome),
    )

internal fun discardDraftMutationProjection(outcome: ReceiptOutcome): Map<String, Any?> =
    mapOf(
        "operation" to
            mapOf(
                "id" to outcome.id.toString(),
                "state" to outcome.state.name,
                "resultId" to outcome.result?.resultId?.toString(),
                "resultExpired" to outcome.resultExpired,
                "problem" to null,
            ),
        "discarded" to (outcome.result?.discarded ?: false),
        "problem" to draftProblemProjection(outcome),
    )

private fun draftProblemProjection(outcome: ReceiptOutcome): Map<String, Any?>? {
    val code = outcome.errorCode ?: return null
    return mapOf(
        "code" to code,
        "message" to "The draft operation could not be completed.",
        "fields" to emptyList<Any>(),
        "retryAfterMs" to null,
    )
}

internal fun parseDraftGraphqlInput(raw: Map<String, Any?>): DraftInput =
    DraftInput(
        targetKind = requireNotNull(raw["targetKind"] as? String) { "targetKind is required" },
        targetId = (raw["targetId"] as? String)?.let { UUID.fromString(it) },
        baseVersion = (raw["baseVersion"] as? String)?.toLong(),
        values =
            requireNotNull(
                when (val v = raw["values"]) {
                    is String -> v
                    is Map<*, *> -> ObjectMapper().writeValueAsString(v)
                    else -> null
                },
            ) { "values is required" },
        operationId = (raw["operationId"] as? String)?.let { UUID.fromString(it) },
        policyRevision = requireNotNull(raw["policyRevision"] as? String) { "policyRevision is required" },
    )
