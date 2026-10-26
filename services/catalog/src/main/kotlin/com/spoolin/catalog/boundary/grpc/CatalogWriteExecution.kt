package com.spoolin.catalog.boundary.grpc

import com.spoolin.catalog.boundary.policy.PolicyWrite
import com.spoolin.catalog.operations.receipts.OwnerReceipts
import com.spoolin.support.receipts.OperationIntent
import com.spoolin.support.receipts.ReceiptOutcome
import com.spoolin.support.receipts.SafeResult
import java.sql.Connection
import java.util.UUID

internal data class CatalogWriteVersion(
    val create: Boolean,
    val supplied: Boolean,
    val expected: Long,
)

/** Bind policy and validate optimistic concurrency only inside the owning receipt transaction. */
internal class CatalogWriteExecution(
    private val receipts: OwnerReceipts,
    private val context: CatalogWriteContext,
) {
    fun <T> execute(
        intent: OperationIntent,
        version: CatalogWriteVersion,
        bind: () -> PolicyWrite<T>,
        create: (Connection, PolicyWrite<T>, UUID, UUID, UUID) -> SafeResult,
        update: (Connection, PolicyWrite<T>, UUID, UUID, UUID) -> SafeResult,
    ): ReceiptOutcome =
        receipts.execute(intent, context.correlation, true) { connection ->
            val command = bind()
            CatalogWriteRpc.requireVersion(version.create, version.supplied, version.expected)
            val mutation = if (version.create) create else update
            mutation(connection, command, context.actor, context.operation, context.correlation)
        }
}
