package com.spoolin.core.coveragehttp

import com.spoolin.core.boundary.graphql.PolicyGraphql
import com.spoolin.core.boundary.http.BrowserContext
import com.spoolin.core.features.policy.PolicyProvider
import com.spoolin.core.features.session.SessionJdbcFixture
import com.spoolin.core.operations.receipts.OwnerReceipts
import com.spoolin.support.policy.PolicyEnvironment
import com.spoolin.support.receipts.IntentHashes
import com.spoolin.support.receipts.OperationIntent
import io.grpc.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.Spoolin
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

class PolicyFailureProjectionRulesTest {
    private val id = UUID.randomUUID()
    private val provider = PolicyProvider(PolicyEnvironment.resolve { null })
    private val input = mapOf("query" to "{operation(id:\"$id\",owner:CATALOG){id state}}")
    private val schema = requireNotNull(javaClass.getResourceAsStream("/graphql/schema.graphql")).use { it.readBytes().toString(Charsets.UTF_8) }

    @Test
    fun `external RPC failures become stable GraphQL problem codes without private transport details`() {
        val cases = listOf(Status.INVALID_ARGUMENT to "FIELD_INVALID", Status.UNAUTHENTICATED to "UNAUTHENTICATED", Status.PERMISSION_DENIED to "FORBIDDEN", Status.NOT_FOUND to "NOT_FOUND", Status.RESOURCE_EXHAUSTED to "RESOURCE_EXHAUSTED", Status.DEADLINE_EXCEEDED to "DEADLINE_EXCEEDED", Status.UNAVAILABLE to "UNAVAILABLE")
        for ((status, code) in cases) {
            val channel = HttpRpcReplies(status.withDescription("private transport diagnostic"))
            val stub = CatalogServiceGrpc.newBlockingStub(channel)
            val engine = PolicyGraphql(schema, provider, operation = { requested, _ ->
                val outcome = stub.getOperation(Spoolin.Id.newBuilder().setId(requested.toString()).build())
                mapOf("id" to outcome.operationId, "state" to outcome.state)
            })
            val result = engine.execute(input, BrowserContext(null, null, null, null))
            assertEquals(code, extensions(result)["code"])
            assertNull(result["data"])
            assertFalse(result.toString().contains("private transport diagnostic"))
            assertEquals(id.toString(), (channel.requests.single() as Spoolin.Id).id)
        }
    }

    @Test
    fun `real receipt ownership and fingerprint conflicts receive distinct safe GraphQL errors`() {
        val intent = OperationIntent(id, "account:viewer", "SAVE", "revision", listOf("title" to "Film"))
        val hashes = IntentHashes(mapOf("compare" to ByteArray(32) { 3 }), "compare")
        val now = Instant.now()
        for ((scope, code) in listOf("account:other" to "NOT_FOUND", intent.actorScope to "IDEMPOTENCY_CONFLICT")) {
            val row = mapOf("id" to id, "state" to "PENDING", "expires_at" to Timestamp.from(now.plusSeconds(600)), "created_at" to Timestamp.from(now), "result" to null, "error_code" to null, "correlation_id" to UUID.randomUUID(), "actor_scope" to scope, "kind" to intent.kind, "request_hash" to ByteArray(32), "hash_key_id" to null, "receipt_type" to "REQUEST")
            val receipts = OwnerReceipts(SessionJdbcFixture(listOf(listOf(row))), hashes)
            val engine = PolicyGraphql(schema, provider, operation = { _, _ -> receipts.resolve(intent)?.let { mapOf("id" to it.id.toString()) } })
            assertEquals(code, extensions(engine.execute(input))["code"])
        }
    }

    @Test
    fun `successful remote operation projection and invalid UUID preserve schema boundaries`() {
        val channel = HttpRpcReplies(Spoolin.Outcome.newBuilder().setOperationId(id.toString()).setState("PENDING").build())
        val stub = CatalogServiceGrpc.newBlockingStub(channel)
        val engine = PolicyGraphql(schema, provider, operation = { requested, _ ->
            val outcome = stub.getOperation(Spoolin.Id.newBuilder().setId(requested.toString()).build())
            mapOf("id" to outcome.operationId, "state" to outcome.state)
        })
        val result = engine.execute(input)
        assertEquals(mapOf("operation" to mapOf("id" to id.toString(), "state" to "PENDING")), result["data"])
        assertEquals("FIELD_INVALID", extensions(engine.execute(mapOf("query" to "{operation(id:\"invalid\"){id}}")))["code"])
    }

    private fun extensions(result: Map<String, Any?>): Map<*, *> = ((result["errors"] as List<*>).single() as Map<*, *>)["extensions"] as Map<*, *>
}
