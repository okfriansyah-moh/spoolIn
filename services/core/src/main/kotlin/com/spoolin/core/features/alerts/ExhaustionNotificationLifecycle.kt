package com.spoolin.core.features.alerts

import com.spoolin.core.SchemaReadiness
import com.spoolin.support.notifications.ExhaustionNotifications
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
class ExhaustionNotificationLifecycle(
    dataSource: DataSource,
    private val readiness: SchemaReadiness,
    environment: Environment,
) {
    private val notifications = ExhaustionNotifications.configured(dataSource, "core") { environment.getProperty(it) }

    @Scheduled(fixedDelay = 1000)
    fun recordExhaustion() {
        if (readiness.ready()) notifications.recordExhaustion()
    }
}
