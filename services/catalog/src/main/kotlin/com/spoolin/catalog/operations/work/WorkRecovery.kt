package com.spoolin.catalog.operations.work

import com.spoolin.support.receipts.committed
import com.spoolin.support.work.WorkRecoveryCommit
import com.spoolin.support.work.WorkRecoveryRequest
import javax.sql.DataSource

/**
 * Catalog service restricted work recovery.
 *
 * Resets an EXHAUSTED work item to READY with an incremented generation.
 * Preserves original evidence; appends a RECOVERY operational_event.
 */
class WorkRecovery(
    private val dataSource: DataSource,
) {
    fun recover(request: WorkRecoveryRequest): Boolean =
        dataSource.committed { connection ->
            val inspected = com.spoolin.support.work.WorkRecoveryOperations.inspect(connection, request.workId, true)
                ?: return@committed false
            val destination = java.util.ServiceLoader.load(com.spoolin.support.work.WorkRecoveryDestination::class.java)
                .singleOrNull { it.owner == "catalog" }
            try {
                com.spoolin.support.work.WorkRecoveryOperations.recover(
                    connection,
                    com.spoolin.support.work.WorkRecoveryAction(request.workId, inspected.generation, inspected.leaseEpoch,
                        WorkRecoveryCommit(request.operatorId, request.reason, request.correlationId)),
                    java.nio.file.Path.of("/var/spoolin/media"), destination,
                )
            } finally { destination?.close() }
        }
}
