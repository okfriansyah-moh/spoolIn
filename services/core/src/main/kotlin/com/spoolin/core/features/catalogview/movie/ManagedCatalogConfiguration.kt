package com.spoolin.core.features.catalogview.movie

import com.spoolin.core.boundary.grpc.ActorIssuer
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.grpc.client.GrpcChannelFactory
import spoolin.v1.CatalogServiceGrpc

/**
 * Wires ManagedCatalogGraphql into the Spring context.
 *
 * Enabled when spoolin.catalog-view.enabled=true (inherits the catalog-view toggle).
 * The actor issuer is optional — writes degrade gracefully when signing is disabled.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = ["spoolin.catalog-view.enabled"], havingValue = "true", matchIfMissing = true)
class ManagedCatalogConfiguration {

    @Bean
    fun managedCatalogGraphql(
        channels: GrpcChannelFactory,
        issuerProvider: ObjectProvider<ActorIssuer>,
    ): ManagedCatalogGraphql =
        ManagedCatalogGraphql(
            catalog = CatalogServiceGrpc.newBlockingStub(channels.createChannel("catalog")),
            issuer = issuerProvider.ifAvailable,
        )
}
