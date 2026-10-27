package com.spoolin.core.boundary.graphql

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin

class GraphqlOutcomeProjectionRulesTest {
    @Test fun `pending and expired operations preserve absent results without invented values`() {
        for (expired in listOf(false, true)) {
            val outcome =
                Spoolin.Outcome
                    .newBuilder()
                    .setOperationId("operation")
                    .setState(if (expired) "EXPIRED" else "PENDING")
                    .setResultExpired(expired)
                    .build()
            val mapped = GraphqlOutcomeProjection.operation(outcome)
            assertEquals("operation", mapped["id"])
            assertEquals(outcome.state, mapped["state"])
            assertEquals(expired, mapped["resultExpired"])
            assertNull(mapped["resultId"])
            assertNull(mapped["version"])
            assertNull(mapped["problem"])
            assertNull(GraphqlOutcomeProjection.mutation(outcome)["problem"])
        }
    }

    @Test fun `successful results preserve zero and large versions as decimal strings`() {
        for (version in listOf(0L, 9007199254740993L)) {
            val outcome =
                Spoolin.Outcome
                    .newBuilder()
                    .setOperationId("operation")
                    .setState("SUCCEEDED")
                    .setResultId("result")
                    .setVersion(version)
                    .build()
            val mutation = GraphqlOutcomeProjection.mutation(outcome)
            val operation = mutation["operation"] as Map<*, *>
            assertEquals("result", operation["resultId"])
            assertEquals(version.toString(), operation["version"])
            assertEquals(false, operation["resultExpired"])
            assertNull(mutation["problem"])
        }
    }

    @Test fun `mutation and remembered operation expose the same safe problem and ordered field issues`() {
        val problem =
            Spoolin.Problem
                .newBuilder()
                .setCode("INPUT_INVALID")
                .addFields(
                    Spoolin.FieldIssue
                        .newBuilder()
                        .setPath("title")
                        .setCode("REQUIRED")
                        .setMessage("Supply a title."),
                ).addFields(
                    Spoolin.FieldIssue
                        .newBuilder()
                        .setPath(
                            "release",
                        ).setCode("INVALID")
                        .setMessage("Check the release date."),
                ).setRetryAfterMs(17)
                .build()
        val outcome =
            Spoolin.Outcome
                .newBuilder()
                .setState("FAILED")
                .setProblem(problem)
                .build()
        val mutation = GraphqlOutcomeProjection.mutation(outcome)
        val mapped = mutation["problem"] as Map<*, *>
        assertEquals("INPUT_INVALID", mapped["code"])
        assertEquals("The operation could not be completed.", mapped["message"])
        assertEquals(
            listOf(
                mapOf("path" to "title", "code" to "REQUIRED", "message" to "Supply a title."),
                mapOf("path" to "release", "code" to "INVALID", "message" to "Check the release date."),
            ),
            mapped["fields"],
        )
        assertEquals(setOf("code", "message", "fields"), mapped.keys)
        assertEquals(mapped, (mutation["operation"] as Map<*, *>)["problem"])
    }
}
