package com.spoolin.core.operations.receipts

import com.spoolin.support.receipts.IntentHashes
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import javax.sql.DataSource

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = ["spoolin.operations.enabled"], havingValue = "true")
class ReceiptConfiguration {
    @Bean
    fun operationComparisonKeys(environment: Environment): IntentHashes = ComparisonKeySettings.load(environment)

    @Bean
    fun receiptExpiry(@org.springframework.beans.factory.annotation.Qualifier("maintenanceDataSource") dataSource: DataSource): com.spoolin.core.operations.maintenance.ReceiptExpiry =
        com.spoolin.core.operations.maintenance
            .ReceiptExpiry(dataSource)

    @Bean
    fun ownerReceipts(
        dataSource: DataSource,
        keys: IntentHashes,
    ): OwnerReceipts = OwnerReceipts(dataSource, keys)
}
