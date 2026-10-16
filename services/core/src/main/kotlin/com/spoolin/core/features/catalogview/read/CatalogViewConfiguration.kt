package com.spoolin.core.features.catalogview.read

import com.spoolin.core.features.policy.PolicyProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.grpc.client.GrpcChannelFactory
import spoolin.v1.CatalogServiceGrpc

/**
 * Wires the CatalogViewGraphql resolver into the Spring context.
 *
 * Enabled when spoolin.catalog-view.enabled=true (default: false to allow
 * incremental activation; T013 will activate for full integration).
 * The enabled condition mirrors the operations config pattern.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = ["spoolin.catalog-view.enabled"], havingValue = "true", matchIfMissing = true)
class CatalogViewConfiguration {
    @Bean
    fun catalogViewGraphql(
        provider: PolicyProvider,
        channels: GrpcChannelFactory,
    ): CatalogViewGraphql =
        CatalogViewGraphql(
            catalog = CatalogServiceGrpc.newBlockingStub(channels.createChannel("catalog")),
            policy = provider.policy,
        )
}
