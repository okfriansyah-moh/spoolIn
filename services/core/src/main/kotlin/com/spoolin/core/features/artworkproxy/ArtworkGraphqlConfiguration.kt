package com.spoolin.core.features.artworkproxy

import com.spoolin.core.boundary.grpc.ActorIssuer
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.grpc.client.GrpcChannelFactory
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.MediaServiceGrpc

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = ["spoolin.catalog-view.enabled"], havingValue = "true", matchIfMissing = true)
class ArtworkGraphqlConfiguration {
    @Bean
    fun artworkGraphql(
        channels: GrpcChannelFactory,
        issuerProvider: ObjectProvider<ActorIssuer>,
    ): ArtworkGraphql =
        ArtworkGraphql(
            catalog = CatalogServiceGrpc.newBlockingStub(channels.createChannel("catalog")),
            media = MediaServiceGrpc.newStub(channels.createChannel("media")),
            issuer = issuerProvider.ifAvailable,
        )
}
