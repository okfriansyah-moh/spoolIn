package com.spoolin.core.features.catalogview.lifecycle

import com.spoolin.core.boundary.grpc.ActorIssuer
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.grpc.client.GrpcChannelFactory
import spoolin.v1.CatalogServiceGrpc

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = ["spoolin.catalog-view.enabled"], havingValue = "true", matchIfMissing = true)
class LifecycleGraphqlConfiguration {
    @Bean
    fun lifecycleGraphql(
        channels: GrpcChannelFactory,
        issuerProvider: ObjectProvider<ActorIssuer>,
        policy: com.spoolin.core.features.policy.PolicyProvider,
    ): LifecycleGraphql =
        LifecycleGraphql(
            catalog = CatalogServiceGrpc.newBlockingStub(channels.createChannel("catalog")),
            issuer = issuerProvider.ifAvailable,
            policyRevision = { policy.policy.revision },
        )
}
