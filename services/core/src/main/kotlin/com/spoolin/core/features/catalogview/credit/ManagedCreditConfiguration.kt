package com.spoolin.core.features.catalogview.credit

import com.spoolin.core.boundary.grpc.ActorIssuer
import com.spoolin.core.features.policy.PolicyProvider
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.grpc.client.GrpcChannelFactory
import spoolin.v1.CatalogServiceGrpc

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = ["spoolin.catalog-view.enabled"], havingValue = "true", matchIfMissing = true)
class ManagedCreditConfiguration {
    @Bean
    fun managedCreditGraphql(
        channels: GrpcChannelFactory,
        issuerProvider: ObjectProvider<ActorIssuer>,
        provider: PolicyProvider,
    ): ManagedCreditGraphql =
        ManagedCreditGraphql(
            catalog = CatalogServiceGrpc.newBlockingStub(channels.createChannel("catalog")),
            issuer = issuerProvider.ifAvailable,
            policy = { provider.policy },
        )
}
