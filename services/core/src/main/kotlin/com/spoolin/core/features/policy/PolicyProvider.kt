package com.spoolin.core.features.policy

import com.spoolin.support.policy.ConfigurationSources
import com.spoolin.support.policy.PolicySettings
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
        val names =
            listOf(
                "REGISTRATION_NAME",
                "PERSON_NAME",
                "TITLE",
                "ORIGINAL_TITLE",
                "SYNOPSIS",
                "CREDIT_ROLE",
                "CHARACTER_NAME",
                "BIOGRAPHY",
                "PASSWORD",
            ).flatMap { listOf("${it}_MIN_CODE_POINTS", "${it}_MAX_CODE_POINTS") } +
                listOf(
                    "EMAIL_MAX_CODE_POINTS",
                    "SEARCH_MAX_CODE_POINTS",
                    "SOURCE_URL_MAX_CODE_POINTS",
                    "REUSE_BASIS_MAX_CODE_POINTS",
                    "ARTWORK_MAX_INPUT_BYTES",
                    "ARTWORK_FORMATS",
                ) +
                listOf(
                    "METADATA_BYTES",
                    "UPLOAD_ENVELOPE_BYTES",
                    "GRAPHQL_QUERY_BYTES",
                    "MAX_DEPTH",
                    "MAX_FIELDS",
                    "MAX_ALIASES",
                    "MAX_COST",
                    "DEFAULT_PAGE",
                    "MAX_PAGE",
                ).map {
                    "REQUEST_$it"
                }
        val values = names.mapNotNull { name -> environment.getProperty(name)?.let { name to it } }.toMap()
        val explicit = names.mapNotNull { name -> System.getenv(name)?.let { name to it } }.toMap()
        return PolicyProvider(PolicySettings.resolve(ConfigurationSources(explicit, values)))
    }
}
