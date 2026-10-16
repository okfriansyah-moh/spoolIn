package com.spoolin.core.operations.receipts

import com.spoolin.core.boundary.grpc.ActorIssuer
import com.spoolin.core.features.session.SessionStore
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.grpc.client.GrpcChannelFactory
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.MediaServiceGrpc
import javax.sql.DataSource

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = ["spoolin.operations.enabled"], havingValue = "true")
class OperationQueryConfiguration {
    @Bean
    fun operationQueries(
        dataSource: DataSource,
        receipts: OwnerReceipts,
        sessions: ObjectProvider<SessionStore>,
        issuer: ObjectProvider<ActorIssuer>,
        channels: GrpcChannelFactory,
    ): OperationQueries =
        OperationQueries(
            OwnerReceiptReads(dataSource),
            receipts,
            sessions.ifAvailable,
            issuer.ifAvailable,
            CatalogServiceGrpc.newBlockingStub(channels.createChannel("catalog")),
            MediaServiceGrpc.newBlockingStub(channels.createChannel("media")),
        )
}
