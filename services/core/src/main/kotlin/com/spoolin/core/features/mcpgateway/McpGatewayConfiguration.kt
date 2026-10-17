package com.spoolin.core.features.mcpgateway

import com.spoolin.core.boundary.grpc.ActorConfiguration
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

@Configuration(proxyBeanMethods = false)
class McpGatewayConfiguration {
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
    ): McpGatewayRpc =
        McpGatewayRpc(
            connectors,
            policy,
            CatalogServiceGrpc.newBlockingStub(channels.createChannel("catalog")),
            issuer,
        )
}
