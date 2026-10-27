package com.spoolin.catalog.unit

import com.spoolin.catalog.boundary.policy.CatalogWriteIntent
import com.spoolin.catalog.boundary.policy.CatalogWritePolicy
import com.spoolin.catalog.features.taxonomy.roles.RoleWrites
import com.spoolin.support.receipts.OperationIntent
import io.grpc.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin

class RoleMutationRulesTest {
    private val policy = CatalogWritePolicy.environment().revision

    private fun request() =
        Spoolin.RoleWrite
            .newBuilder()
            .setContext(
                Spoolin.Context
                    .newBuilder()
                    .setOperationId(unitOperation.toString())
                    .setCorrelationId(unitCorrelation.toString())
                    .setPolicyRevision(policy),
            ).setCategory("CREATOR")
            .setLabel(" Cinematographer ")
            .build()

    private fun intent(request: Spoolin.RoleWrite) =
        OperationIntent(unitOperation, "account:$unitActor", "CreateRole", policy, CatalogWriteIntent.role(request))

    @Test fun `custom role creation normalizes label defaults characters off and returns safe identity`() =
        withMaintainer {
            val request = request()
            val intent = intent(request)
            val resultJson = """{"resultId":"$unitTarget","version":"0"}"""
            val db =
                JdbcRows(
                    listOf(mapOf("lock" to 1)),
                    listOf(receiptRow(intent)),
                    emptyList(),
                    listOf(receiptRow(intent, "SUCCEEDED", resultJson)),
                )
            val result = RpcResult<Spoolin.Outcome>()
            RoleWrites(ownerReceipts(db), unitClock).createRole(request, result)
            assertEquals("SUCCEEDED", result.value().state)
            assertEquals(unitTarget.toString(), result.value().resultId)
            assertTrue(db.writes.any { "Cinematographer" in it && "CREATOR" in it })
            assertTrue(
                db.writes
                    .flatten()
                    .filterIsInstance<String>()
                    .any { it.contains("Cinematographer") && it.contains("changes") },
            )
        }

    @Test fun `duplicate role returns conflict and invalid category returns field error`() =
        withMaintainer {
            val request = request()
            val db = JdbcRows(listOf(mapOf("lock" to 1)), listOf(receiptRow(intent(request))), listOf(mapOf("exists" to 1)))
            val duplicate = RpcResult<Spoolin.Outcome>()
            RoleWrites(ownerReceipts(db), unitClock).createRole(request, duplicate)
            assertEquals("ROLE_EXISTS", Status.fromThrowable(duplicate.failure).description)
            assertEquals(1, db.writes.size)
            val invalid = request.toBuilder().setCategory("UNKNOWN").build()
            val bad = JdbcRows(listOf(mapOf("lock" to 1)), listOf(receiptRow(intent(invalid))))
            val result = RpcResult<Spoolin.Outcome>()
            RoleWrites(ownerReceipts(bad), unitClock).createRole(invalid, result)
            assertEquals("FIELD_INVALID", Status.fromThrowable(result.failure).description)
        }
}
