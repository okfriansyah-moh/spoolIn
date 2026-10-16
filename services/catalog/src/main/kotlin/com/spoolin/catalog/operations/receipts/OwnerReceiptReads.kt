package com.spoolin.catalog.operations.receipts

import com.spoolin.support.receipts.ReceiptOutcome
import com.spoolin.support.receipts.queryBound
import java.time.Clock
import java.util.UUID
import javax.sql.DataSource

class OwnerReceiptReads(
    private val dataSource: DataSource,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun find(
        id: UUID,
        actorScope: String,
    ): ReceiptOutcome? =
        dataSource.connection.use { connection ->
            connection
                .queryBound(
                    "SELECT * FROM operation_receipt WHERE id=? AND actor_scope=? AND receipt_type='REQUEST'",
                    listOf(id, actorScope),
                ) { ReceiptRows.decode(it, clock.instant()).outcome }
                .singleOrNull()
        }
}
