package com.spoolin.support.observability

import io.grpc.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LogicalResultRulesTest {
    @Test
    fun `caller supplied metric names cannot create new labels`() {
        assertNull(MetricOperations.graphql("private-person-name"))
        assertNull(MetricOperations.rpc("untrusted-service", "WriteMovie"))
        assertNull(MetricOperations.named("core", "private-id", "ingress"))
        assertEquals(false, MetricOperations.graphql("titles")?.mutation)
        assertEquals(true, MetricOperations.graphql("createMovie")?.mutation)
        assertEquals(true, MetricOperations.graphql("operation")?.receiptLookup)
        assertEquals("register", MetricOperations.named("core", "REGISTER", "owner")?.name)
        assertEquals("GraphQL", MetricOperations.named("core", "GraphQL", "ingress")?.name)
        assertNull(MetricOperations.named("core", "GraphQL", "owner"))
    }

    @Test
    fun `transport interruptions preserve mutation uncertainty`() {
        listOf(Status.DEADLINE_EXCEEDED to MetricCode.DEADLINE_EXCEEDED, Status.CANCELLED to MetricCode.CANCELLED)
            .forEach { (status, code) ->
                assertEquals(MetricOutcome.UNCERTAIN, LogicalResult.fromStatus(status, true).outcome)
                assertEquals(MetricOutcome.SYSTEM_FAILED, LogicalResult.fromStatus(status, false).outcome)
                assertEquals(code, LogicalResult.fromStatus(status, true).code)
            }
        assertEquals(MetricOutcome.DOMAIN_REJECTED, LogicalResult.fromStatus(Status.PERMISSION_DENIED, true).outcome)
        assertEquals(MetricOutcome.SYSTEM_FAILED, LogicalResult.fromStatus(Status.UNAVAILABLE, false).outcome)
        assertEquals(MetricOutcome.UNCERTAIN, LogicalResult.fromStatus(Status.UNKNOWN, false).outcome)
    }

    @Test
    fun `only successful problem-free receipts are successes`() {
        assertEquals(LogicalResult.success(), LogicalResult.fromReceipt("SUCCEEDED", null))
        assertEquals(LogicalResult.unknown(), LogicalResult.fromReceipt("SUCCEEDED", "CONFLICT"))
        assertEquals(MetricCode.CONFLICT, LogicalResult.fromReceipt("FAILED", "CONFLICT").code)
        assertEquals(MetricCode.PENDING, LogicalResult.fromReceipt("PENDING", null).code)
        assertEquals(MetricCode.RESULT_EXPIRED, LogicalResult.fromReceipt("EXPIRED", null).code)
        assertEquals(LogicalResult.unknown(), LogicalResult.fromReceipt(null, null))
        assertEquals(LogicalResult.unknown(), LogicalResult.fromProblem("private unexpected code"))
        assertEquals(MetricCode.PERMISSION_DENIED, LogicalResult.fromProblem("FORBIDDEN").code)
        assertEquals(MetricCode.FAILED_PRECONDITION, LogicalResult.fromProblem("POLICY_CHANGED").code)
    }

    @Test
    fun `GraphQL HTTP failures cannot be mistaken for successful data`() {
        val expected = mapOf(400 to MetricCode.INVALID_ARGUMENT, 401 to MetricCode.UNAUTHENTICATED,
            403 to MetricCode.PERMISSION_DENIED, 413 to MetricCode.RESOURCE_EXHAUSTED,
            429 to MetricCode.RESOURCE_EXHAUSTED, 503 to MetricCode.UNAVAILABLE, 500 to MetricCode.INTERNAL)
        expected.forEach { (status, code) ->
            assertEquals(code, LogicalResult.fromGraphql(mapOf("data" to mapOf("titles" to emptyList<Any>())), status, setOf("titles")).code)
        }
        assertEquals(LogicalResult.unknown(), LogicalResult.fromGraphql(null, 200, setOf("titles")))
        assertEquals(LogicalResult.unknown(), LogicalResult.fromGraphql(emptyMap<String, Any>(), 200, emptySet()))
    }

    @Test
    fun `GraphQL considers selected roots and their persisted mutation outcomes`() {
        assertEquals(LogicalResult.success(), classify(mapOf("data" to mapOf("titles" to emptyList<Any>()))))
        val unrelated = mapOf("path" to listOf("person"), "extensions" to mapOf("code" to "INTERNAL"))
        assertEquals(LogicalResult.success(), classify(mapOf("data" to mapOf("titles" to emptyList<Any>()), "errors" to listOf(unrelated))))
        val invalid = mapOf("extensions" to mapOf("classification" to "ValidationError"))
        assertEquals(MetricCode.INVALID_ARGUMENT, classify(mapOf("errors" to listOf(invalid))).code)
        assertEquals(LogicalResult.unknown(), classify(mapOf("errors" to "not a list")))
        assertEquals(LogicalResult.unknown(), classify(mapOf("data" to emptyMap<String, Any>())))
        val receipt = mapOf("operation" to mapOf("state" to "SUCCEEDED"))
        assertEquals(LogicalResult.success(), LogicalResult.fromGraphql(mapOf("data" to mapOf("createMovie" to receipt)), 200, setOf("createMovie")))
        val failure = mapOf("problem" to mapOf("code" to "CONFLICT"))
        assertEquals(MetricCode.CONFLICT, LogicalResult.fromGraphql(mapOf("data" to mapOf("createMovie" to failure)), 200, setOf("createMovie")).code)
    }

    private fun classify(body: Map<String, Any?>): LogicalResult = LogicalResult.fromGraphql(body, 200, setOf("titles"))
}
