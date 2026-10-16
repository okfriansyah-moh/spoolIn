package com.spoolin.media.operations.work

import com.spoolin.support.receipts.committed
import com.spoolin.support.work.WorkRecoveryRequest
import com.spoolin.support.work.WorkSql
import java.time.Clock
import java.util.UUID
import javax.sql.DataSource

/**
 * Media service restricted work recovery.
 *
 * Resets an EXHAUSTED work item to READY with an incremented generation.
 * Preserves original evidence; appends a RECOVERY operational_event.
 */
class WorkRecovery(
    private val dataSource: DataSource,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun recover(request: WorkRecoveryRequest): Boolean =
        dataSource.committed { connection ->
            WorkSql.recoverWork(
                connection,
                request.workId,
                request.operatorId,
                request.reason,
                request.correlationId,
                newLeaseEpoch = 0L,
                eventId = UUID.randomUUID(),
                now = clock.instant(),
            )
        }
}
