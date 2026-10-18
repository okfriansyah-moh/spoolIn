package com.spoolin.core.features.policy

import com.spoolin.support.policy.PublicPolicy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import spoolin.v1.Spoolin.ArtworkPolicy
import spoolin.v1.Spoolin.PolicySnapshot
import spoolin.v1.Spoolin.RequestLimits
import spoolin.v1.Spoolin.TextBound

class PolicyProvider(
    val policy: PublicPolicy,
) {
    fun projection(): Map<String, Any> = policy.projection()

    fun protobuf(): PolicySnapshot =
        PolicySnapshot
            .newBuilder()
            .setRevision(policy.revision)
            .addAllTextBounds(
                policy.textBounds.map { (field, bound) ->
                    TextBound
                        .newBuilder()
                        .setField(field)
                        .setMinCodePoints(bound.minCodePoints)
                        .setMaxCodePoints(bound.maxCodePoints)
                        .setRequired(bound.required)
                        .build()
                },
            ).setArtwork(
                ArtworkPolicy
                    .newBuilder()
                    .setMaxInputBytes(policy.artwork.maxInputBytes)
                    .addAllFormats(policy.artwork.formats)
                    .setMaxDimensionPx(policy.artwork.maxDimensionPx)
                    .setMaxPixels(policy.artwork.maxPixels),
            ).setRequestLimits(
                RequestLimits
                    .newBuilder()
                    .setMaxPage(policy.requestLimits.getValue("maxPage"))
                    .setMaxMetadataBytes(
                        policy.requestLimits.getValue("metadataBytes"),
                    ).setMaxSearchCodePoints(policy.textBounds.getValue("search").maxCodePoints),
            ).build()
}

@Configuration
class PolicyConfiguration {
    @Bean
    fun policyProvider(environment: Environment): PolicyProvider {
        return PolicyProvider(com.spoolin.support.policy.PolicyEnvironment.resolve { environment.getProperty(it) })
    }
}
