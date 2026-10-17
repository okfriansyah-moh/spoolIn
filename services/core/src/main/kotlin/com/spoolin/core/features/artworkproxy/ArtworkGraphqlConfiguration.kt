package com.spoolin.core.features.artworkproxy

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = ["spoolin.catalog-view.enabled"], havingValue = "true", matchIfMissing = true)
class ArtworkGraphqlConfiguration {
    @Bean
    fun artworkGraphql(): ArtworkGraphql = ArtworkGraphql()
}
