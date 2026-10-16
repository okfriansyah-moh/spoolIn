package com.spoolin.catalog.operations.maintenance

import java.sql.Timestamp
import java.time.Clock
import javax.sql.DataSource

class ReceiptExpiry(
    private val maintenance: DataSource,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun purgeBodies(): Int =
        maintenance.connection.use { connection ->
            connection
                .prepareStatement(
                    "UPDATE operation_receipt SET result=NULL WHERE receipt_type='REQUEST' " +
                        "AND state IN ('SUCCEEDED','FAILED','EXPIRED') AND expires_at<=? AND result IS NOT NULL",
                ).use { statement ->
                    statement.setTimestamp(1, Timestamp.from(clock.instant()))
                    statement.executeUpdate()
                }
        }
}
