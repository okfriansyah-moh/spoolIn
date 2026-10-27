package com.spoolin.core.boundary.graphql

import spoolin.v1.Spoolin

internal object GraphqlOutcomeProjection {
    fun mutation(outcome: Spoolin.Outcome): Map<String, Any?> =
        mapOf(
            "operation" to operation(outcome),
            "problem" to if (outcome.hasProblem()) problem(outcome.problem) else null,
        )

    fun operation(outcome: Spoolin.Outcome): Map<String, Any?> =
        mapOf(
            "id" to outcome.operationId,
            "state" to outcome.state,
            "resultId" to if (outcome.hasResultId()) outcome.resultId else null,
            "version" to if (outcome.hasVersion()) outcome.version.toString() else null,
            "resultExpired" to outcome.resultExpired,
            "problem" to if (outcome.hasProblem()) problem(outcome.problem) else null,
        )

    fun problem(p: Spoolin.Problem): Map<String, Any?> =
        mapOf(
            "code" to p.code,
            "message" to "The operation could not be completed.",
            "fields" to p.fieldsList.map { mapOf("path" to it.path, "code" to it.code, "message" to it.message) },
        )
}
