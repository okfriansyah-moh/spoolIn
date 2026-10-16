package com.spoolin.core.operations.receipts

import com.spoolin.support.receipts.ReceiptOutcome
import com.spoolin.support.receipts.queryBound
import java.time.Clock
import java.util.UUID
import javax.sql.DataSource

class OwnerReceiptReads(
    private val dataSource: DataSource,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun anonymous(
        id: UUID,
        scope: String,
    ): Boolean =
        dataSource.connection.use { connection ->
            connection
                .queryBound(
                    "SELECT 1 FROM operation_receipt WHERE id=? AND actor_scope=? AND receipt_type='REQUEST' " +
                        "AND kind IN ('REGISTER','SignIn')",
                    listOf(id, scope),
                ) { true }
                .singleOrNull() ?: false
        }

    fun isSignOut(
        id: UUID,
        actorScope: String,
    ): Boolean =
        dataSource.connection.use { connection ->
            connection
                .queryBound(
                    "SELECT 1 FROM operation_receipt WHERE id=? AND actor_scope=? " +
                        "AND receipt_type='REQUEST' AND kind='SignOut'",
                    listOf(id, actorScope),
                ) { true }
                .singleOrNull() ?: false
        }

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
