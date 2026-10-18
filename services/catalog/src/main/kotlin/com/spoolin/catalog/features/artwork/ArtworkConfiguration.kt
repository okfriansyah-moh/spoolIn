package com.spoolin.catalog.features.artwork

import com.spoolin.catalog.SchemaReadiness
import com.spoolin.catalog.operations.receipts.OwnerReceipts
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.grpc.client.GrpcChannelFactory
import spoolin.v1.IdentityServiceGrpc
import spoolin.v1.MediaServiceGrpc
import javax.sql.DataSource

@Configuration(proxyBeanMethods = false)
class ArtworkConfiguration {
    @Bean(destroyMethod = "close")
    fun artworkDispatcher(
        dataSource: DataSource,
        receipts: OwnerReceipts,
        readiness: SchemaReadiness,
        channels: GrpcChannelFactory,
        environment: Environment,
    ): ArtworkDispatcher {
        val store =
            ArtworkWorkStore(
                dataSource,
                receipts,
            ) {
                com.spoolin.support.policy.PolicyEnvironment
                    .resolve { environment.getProperty(it) }
                    .revision
            }
        val handler =
            ArtworkWorkHandler(
                store,
                ArtworkReads(dataSource),
                MediaServiceGrpc.newBlockingStub(channels.createChannel("media")),
                IdentityServiceGrpc.newBlockingStub(channels.createChannel("core")),
                dataSource,
            )
        return ArtworkDispatcher(
            dataSource,
            readiness,
            handler,
            ArtworkNotifications(
                com.spoolin.support.notifications.ExhaustionNotifications
                    .configured(
                        dataSource,
                        "catalog",
                    ) {
                        environment.getProperty(it)
                    },
            ),
        )
    }
}
