package com.spoolin.core.features.ratelimit

import org.springframework.scheduling.annotation.Scheduled
import javax.sql.DataSource

class RateMaintenance(
    private val dataSource: DataSource,
    private val agreement: RateAgreement,
) {
    @Scheduled(fixedDelayString = "PT1H")
    fun purge(): Int =
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate("DELETE FROM appdata.request_rate_window WHERE expires_at<=clock_timestamp()")
            }
        }

    fun ready(): Boolean =
        runCatching {
            agreement.check()
            dataSource.connection.use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT 1 FROM appdata.request_rate_window LIMIT 1").close()
                }
            }
            true
        }.getOrDefault(false)
}
