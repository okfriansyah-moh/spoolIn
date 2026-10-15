package com.spoolin.support.problem

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class ProblemRulesTest {
    @TestFactory
    fun mappings() =
        listOf(
            Triple(FailureKind.INVALID, "FIELD_INVALID", "INVALID_ARGUMENT"),
            Triple(FailureKind.UNAUTHENTICATED, "UNAUTHENTICATED", "UNAUTHENTICATED"),
            Triple(FailureKind.DENIED, "FORBIDDEN", "PERMISSION_DENIED"),
            Triple(FailureKind.NOT_FOUND, "NOT_FOUND", "NOT_FOUND"),
            Triple(FailureKind.STALE, "STALE_VERSION", "ABORTED"),
            Triple(FailureKind.IMPACT, "IMPACT_CHANGED", "ABORTED"),
            Triple(FailureKind.IDEMPOTENCY, "IDEMPOTENCY_CONFLICT", "ALREADY_EXISTS"),
            Triple(FailureKind.CAPACITY, "RESOURCE_EXHAUSTED", "RESOURCE_EXHAUSTED"),
            Triple(FailureKind.DEPENDENCY, "UNAVAILABLE", "UNAVAILABLE"),
            Triple(FailureKind.DEADLINE, "DEADLINE_EXCEEDED", "DEADLINE_EXCEEDED"),
        ).map { (kind, code, status) ->
            dynamicTest("ATDD-291 $kind") {
                val problem = SafeProblem.from(kind, "movie.title")
                val mcp = problem.mcpFailure().projection()
                assertEquals(setOf("isError", "problem", "safeText"), mcp.keys)
                assertEquals(true, mcp["isError"])
                assertEquals(problem.graphql(), mcp["problem"])
                assertEquals(kind.correction, mcp["safeText"])
                assertEquals(code, problem.code)
                assertEquals(status, kind.rpcStatus)
                assertEquals(setOf("code", "message", "fields", "retryAfterMs"), problem.graphql().keys)
                assertEquals(setOf("code", "fields"), problem.protobuf().keys)
                assertEquals(
                    setOf("path", "code", "message"),
                    problem.fields
                        .single()
                        .projection()
                        .keys,
                )
                assertEquals("movie.title", problem.fields.single().path)
            }
        }

    @Test
    fun `ATDD-291 error projections reject sensitive free text`() {
        val problem = SafeProblem.from(FailureKind.INVALID, "SQL provider password SYNTHETIC_PRIVATE_VALUE")
        assertTrue(problem.fields.isEmpty())
        assertFalse(problem.graphql().toString().contains("SYNTHETIC_PRIVATE_VALUE"))
        assertFalse(
            problem
                .mcpFailure()
                .projection()
                .toString()
                .contains("SYNTHETIC_PRIVATE_VALUE"),
        )
        assertFalse(LogicalOutcome.succeeded(200, problem))
        assertFalse(LogicalOutcome.succeeded(200, null, "FAILED"))
        assertTrue(LogicalOutcome.succeeded(200, null, "SUCCEEDED"))
        assertFalse(LogicalOutcome.succeeded(503, null))
        assertEquals(1500, SafeProblem.from(FailureKind.CAPACITY, retryAfterMs = 1500).protobuf()["retry_after_ms"])
        assertThrows(IllegalArgumentException::class.java) { SafeProblem.from(FailureKind.CAPACITY, retryAfterMs = 0) }
    }
}
