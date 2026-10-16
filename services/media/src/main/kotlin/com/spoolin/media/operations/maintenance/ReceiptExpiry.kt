package com.spoolin.media.operations.maintenance

import java.sql.Timestamp
import java.time.Clock
import javax.sql.DataSource

private const val BODY_PURGE_INTERVAL_MS = 3600000L

class ReceiptExpiry(
    private val maintenance: DataSource,
    private val clock: Clock = Clock.systemUTC(),
) {
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = BODY_PURGE_INTERVAL_MS)
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
