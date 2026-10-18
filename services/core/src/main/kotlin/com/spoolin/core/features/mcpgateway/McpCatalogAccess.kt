package com.spoolin.core.features.mcpgateway

import com.spoolin.core.features.connector.ConnectorRecord
import com.spoolin.core.features.connector.ConnectorStore
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.identity.Permissions
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.MetadataUtils
import io.grpc.stub.StreamObserver
import spoolin.v1.CatalogServiceGrpc
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val CATALOG_DEADLINE_SECONDS = 5L

internal data class McpCatalogInvocation(
    val method: String,
    val operationId: UUID? = UUID.randomUUID(),
    val correlationId: UUID = UUID.randomUUID(),
)

/** Every downstream method receives its own actor assertion after current connector authorization. */
internal class McpCatalogAccess(
    private val connectors: ConnectorStore,
    private val catalog: CatalogServiceGrpc.CatalogServiceBlockingStub,
    private val issuer: McpActorIssuer?,
) {
    fun currentConnector(): ConnectorRecord? = ConnectorRpcContext.current()
        ?.let(connectors::findByTokenHash)?.takeIf(connectors::live)

    fun managementConnector(): ConnectorRecord? = currentConnector()?.takeIf {
        Permissions.management(role(it))
    }

    fun <T> invoke(invocation: McpCatalogInvocation, call: (CatalogServiceGrpc.CatalogServiceBlockingStub) -> T): T {
        val connector = managementConnector() ?: denied()
        val signer = issuer ?: denied()
        val token = signer.issue(connector, role(connector), invocation.method,
            invocation.operationId, invocation.correlationId)
        val metadata = Metadata().apply { put(ActorRpc.metadata, token) }
        val stub = catalog.withDeadlineAfter(CATALOG_DEADLINE_SECONDS, TimeUnit.SECONDS)
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
        return call(stub)
    }

    fun <T> forward(
        invocation: McpCatalogInvocation,
        response: StreamObserver<T>,
        call: (CatalogServiceGrpc.CatalogServiceBlockingStub) -> T,
    ) {
        try {
            response.onNext(invoke(invocation, call))
            response.onCompleted()
        } catch (failure: StatusRuntimeException) {
            response.onError(failure)
        }
    }

    private fun role(connector: ConnectorRecord): String = connectors.accountRole(connector.accountId) ?: "VIEWER"
}

internal fun denied(): Nothing = throw Status.PERMISSION_DENIED.withDescription("CONNECTOR_DENIED").asRuntimeException()
