package com.spoolin.core.features.ratelimit

import javax.sql.DataSource

class RateMaintenance(
    private val dataSource: DataSource,
    private val agreement: RateAgreement,
) {
    fun purge(): Int =
        dataSource.connection.use { connection ->
            com.spoolin.support.maintenance.RetentionMaintenance.purgeRateWindows(connection)
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
