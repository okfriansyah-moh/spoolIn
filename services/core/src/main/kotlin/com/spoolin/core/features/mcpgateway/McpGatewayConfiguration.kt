package com.spoolin.core.features.mcpgateway

import com.spoolin.core.features.connector.ConnectorStore
import com.spoolin.core.features.policy.PolicyProvider
import com.spoolin.support.identity.ActorSigner
import com.spoolin.support.identity.ActorTrust
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.grpc.client.GrpcChannelFactory
import spoolin.v1.CatalogServiceGrpc
import java.nio.file.Path
import java.nio.file.Files
import tools.jackson.databind.ObjectMapper

@Configuration(proxyBeanMethods = false)
class McpGatewayConfiguration {
    @Bean
    fun uiAssertionVerifier(environment: Environment, mapper: ObjectMapper): UiAssertionVerifier {
        val enabled = environment.getProperty("MCP_PUBLICATION_ENABLED", Boolean::class.java, false)
        val file = environment.getProperty("MCP_UI_ASSERTION_KEY_FILE")
        val key = if (enabled && !file.isNullOrBlank()) Files.readAllBytes(Path.of(file)) else null
        return UiAssertionVerifier(enabled, environment.getProperty("MCP_UI_ASSERTION_KEY_ID"), key, mapper)
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
    fun mcpGatewayRpc(
        connectors: ConnectorStore,
        policy: PolicyProvider,
        channels: GrpcChannelFactory,
        issuer: McpActorIssuer?,
        uiChallenges: ProposalUiChallengeStore,
        uiAssertions: UiAssertionVerifier,
    ): McpGatewayRpc =
        McpGatewayRpc(
            connectors,
            policy,
            CatalogServiceGrpc.newBlockingStub(channels.createChannel("catalog")),
            issuer,
            uiChallenges,
            uiAssertions,
        )
}
