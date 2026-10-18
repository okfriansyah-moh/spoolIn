package com.spoolin.core.features.connector

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import javax.sql.DataSource

@Configuration(proxyBeanMethods = false)
class ConnectorConfiguration {
    @Bean
    fun connectorStore(
        dataSource: DataSource,
        clock: org.springframework.beans.factory.ObjectProvider<Clock>,
    ): ConnectorStore = ConnectorStore(dataSource, clock.ifAvailable ?: Clock.systemUTC())
}
