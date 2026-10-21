package com.spoolin.core.features.mcpgateway

import com.spoolin.core.features.connector.ConnectorStore
import com.spoolin.core.features.policy.PolicyProvider
import com.spoolin.support.identity.ActorSigner
import com.spoolin.support.identity.ActorTrust
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.grpc.client.GrpcChannelFactory
import spoolin.v1.CatalogServiceGrpc
import tools.jackson.databind.ObjectMapper
import java.nio.file.Path

@Configuration(proxyBeanMethods = false)
class McpGatewayConfiguration {
    @Bean(destroyMethod = "close")
    fun uiAssertionVerifier(
        environment: Environment,
        mapper: ObjectMapper,
    ): UiAssertionVerifier {
        val enabled = environment.getProperty("MCP_PUBLICATION_ENABLED", Boolean::class.java, false)
        return UiAssertionVerifier(enabled, if (enabled) UiAssertionKeys.load(environment) else null, mapper)
    }

    @Bean
    fun proposalUiChallengeStore(): ProposalUiChallengeStore = ProposalUiChallengeStore()

    @Bean
    fun mcpActorIssuer(environment: Environment): McpActorIssuer? {
        if (!environment.getProperty("spoolin.actor.enabled", Boolean::class.java, false)) return null
        return McpActorIssuer(
            ActorSigner(
                environment.getRequiredProperty("ACTOR_SIGNING_KEY_ID"),
                ActorTrust.signer(Path.of(environment.getRequiredProperty("ACTOR_SIGNING_PRIVATE_KEY_FILE"))),
            ),
        )
    }

    @Bean
    fun mcpGatewaySecurity(
        issuer: McpActorIssuer?,
        uiChallenges: ProposalUiChallengeStore,
        uiAssertions: UiAssertionVerifier,
    ): McpGatewaySecurity = McpGatewaySecurity(issuer, uiChallenges, uiAssertions)

    @Bean
    fun preparedPosterUpload(channels: GrpcChannelFactory, policy: PolicyProvider) =
        com.spoolin.core.features.artworkproxy.PreparedPosterUpload(
            spoolin.v1.MediaServiceGrpc.newStub(channels.createChannel("media")), policy.policy.artwork.maxInputBytes,
        )

    @Bean
    fun mcpGatewayRpc(
        connectors: ConnectorStore,
        policy: PolicyProvider,
        channels: GrpcChannelFactory,
        security: McpGatewaySecurity,
        uploads: com.spoolin.core.features.artworkproxy.PreparedPosterUpload,
    ): McpGatewayRpc =
        McpGatewayRpc(
            connectors,
            policy,
            CatalogServiceGrpc.newBlockingStub(channels.createChannel("catalog")),
            security.issuer,
            security.challenges,
            security.assertions,
            uploads,
        )
}

data class McpGatewaySecurity(
    val issuer: McpActorIssuer?,
    val challenges: ProposalUiChallengeStore,
    val assertions: UiAssertionVerifier,
)
