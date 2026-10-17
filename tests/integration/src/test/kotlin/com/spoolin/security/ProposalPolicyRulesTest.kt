package com.spoolin.security

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

/** Policy/security skeleton assertions for proposal approval (T042–T046). */
class ProposalPolicyRulesTest {
    @Test
    fun catalog_actors_require_connector_client_for_approval() {
        val source =
            Files.readString(
                repoRoot().resolve(
                    "services/catalog/src/main/kotlin/com/spoolin/catalog/boundary/grpc/CatalogActors.kt",
                ),
            )
        assertTrue(source.contains("ApproveAndCreate"))
        assertTrue(source.contains("claims.clientKind == null"))
    }

    @Test
    fun mcp_gateway_issues_method_scoped_tokens() {
        val source =
            Files.readString(
                repoRoot().resolve(
                    "services/core/src/main/kotlin/com/spoolin/core/features/mcpgateway/McpGatewayRpc.kt",
                ),
            )
        assertTrue(source.contains("ApproveAndCreate"))
        assertTrue(source.contains("RejectProposal"))
    }

    private fun repoRoot(): Path =
        Path.of(System.getProperty("user.dir")).let { cwd ->
            if (cwd.endsWith("tests/integration")) cwd.parent.parent else cwd
        }
}
