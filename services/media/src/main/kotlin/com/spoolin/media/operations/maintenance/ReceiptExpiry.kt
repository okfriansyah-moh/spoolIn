package com.spoolin.media.operations.maintenance

import com.spoolin.support.maintenance.RetentionMaintenance
import java.time.Clock
import javax.sql.DataSource

class ReceiptExpiry(
    maintenance: DataSource,
    @Suppress("UNUSED_PARAMETER") clock: Clock = Clock.systemUTC(),
) {
    private val log =
        java.util.logging.Logger
            .getLogger(ReceiptExpiry::class.java.name)
    private val retention = RetentionMaintenance(maintenance, "media")

    @org.springframework.scheduling.annotation.Scheduled(fixedDelayString = "PT1H")
    fun purgeBodies(): Int =
        retention
            .hourly()
            .also { log.info("Hourly retention counts: $it") }
            .values
            .sum()

    @org.springframework.scheduling.annotation.Scheduled(fixedDelayString = "P1D")
    fun purgeHistory(): Map<String, Int> = retention.daily().also { log.info("Daily retention counts: $it") }
}
