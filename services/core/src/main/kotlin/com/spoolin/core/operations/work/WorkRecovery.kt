package com.spoolin.core.operations.work

import com.spoolin.support.receipts.committed
import com.spoolin.support.work.WorkRecoveryCommit
import com.spoolin.support.work.WorkRecoveryRequest
import javax.sql.DataSource

/**
 * Core service restricted work recovery.
 *
 * Authorized operators may reset an EXHAUSTED work item to READY with a new generation.
 * The original generation's evidence (attempt events, error codes) is preserved.  A RECOVERY
 * operational_event is appended with the operator identity and reason.
 *
 * Recovery is a restricted administrative operation:
 * - Work must be in state EXHAUSTED.
 * - A non-blank reason and a verified operator identity are required.
 * - The original generation's budget and effect identity (subjectId) are unchanged.
 * - Each recovery increments the generation; the new generation starts with attempt_count=0.
 *
 * This is not a browser feature — access must be gated by operator authentication.
 */
class WorkRecovery(
    private val dataSource: DataSource,
) {
    /**
     * Attempts to recover [request.workId].
     *
     * @return true when recovery was committed; false when the work is not EXHAUSTED
     *         (already recovered, cancelled, or done by another operator).
     */
    fun recover(request: WorkRecoveryRequest): Boolean =
        dataSource.committed { connection ->
            val inspected =
                com.spoolin.support.work.WorkRecoveryOperations
                    .inspect(connection, request.workId, true)
                    ?: return@committed false
            val destination =
                java.util.ServiceLoader
                    .load(com.spoolin.support.work.WorkRecoveryDestination::class.java)
                    .singleOrNull { it.owner == "core" }
            try {
                com.spoolin.support.work.WorkRecoveryOperations.recover(
                    connection,
                    com.spoolin.support.work.WorkRecoveryAction(
                        request.workId,
                        inspected.generation,
                        inspected.leaseEpoch,
                        WorkRecoveryCommit(request.operatorId, request.reason, request.correlationId),
                    ),
                    java.nio.file.Path
                        .of("/var/spoolin/media"),
                    destination,
                )
            } finally {
                destination?.close()
            }
        }
}
