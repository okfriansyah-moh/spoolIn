package com.spoolin.core.features.mcpgateway

import com.google.protobuf.Empty
import com.spoolin.core.features.connector.ConnectorStore
import com.spoolin.core.features.policy.PolicyProvider
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.identity.Permissions
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.stub.MetadataUtils
import io.grpc.stub.StreamObserver
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.McpGatewayServiceGrpc
import spoolin.v1.Spoolin
import java.util.UUID
import java.util.concurrent.TimeUnit

class McpGatewayRpc(
    private val connectors: ConnectorStore,
    private val policy: PolicyProvider,
    private val catalog: CatalogServiceGrpc.CatalogServiceBlockingStub,
    private val issuer: McpActorIssuer?,
) : McpGatewayServiceGrpc.McpGatewayServiceImplBase() {
    override fun getPolicy(
        request: Empty,
        response: StreamObserver<Spoolin.PolicySnapshot>,
    ) {
        val connector = currentConnector() ?: return deny(response)
        if (!Permissions.management(roleFor(connector))) return deny(response)
        response.onNext(policy.protobuf())
        response.onCompleted()
    }

    override fun getMovieContext(
        request: Spoolin.MovieContextRequest,
        response: StreamObserver<Spoolin.MovieContext>,
    ) {
        val connector = currentConnector() ?: return deny(response)
        if (!Permissions.management(roleFor(connector))) return deny(response)
        val correlation = UUID.randomUUID()
        val token =
            issuer?.issue(
                connector,
                roleFor(connector),
                "/spoolin.v1.CatalogService/SearchTitles",
                null,
                correlation,
            ) ?: return deny(response)
        val titles =
            catalogStub(token)
                .searchTitles(
                    Spoolin.Search
                        .newBuilder()
                        .setText(request.query)
                        .setFirst(request.limit.coerceIn(1, 20))
                        .build(),
                )
        val genres = catalogStub(token).listGenres(Empty.getDefaultInstance())
        val roles = catalogStub(token).listRoles(Empty.getDefaultInstance())
        response.onNext(
            Spoolin.MovieContext
                .newBuilder()
                .setTitles(titles)
                .setGenres(genres)
                .setRoles(roles)
                .setPolicy(policy.protobuf())
                .build(),
        )
        response.onCompleted()
    }

    override fun proposeMovie(
        request: Spoolin.Proposal,
        response: StreamObserver<Spoolin.ProposalRevision>,
    ) {
        forwardCatalog(connector = currentConnector(), response) { token ->
            catalogStub(token).proposeMovie(request)
        }
    }

    override fun approveMovie(
        request: Spoolin.ApprovalInput,
        response: StreamObserver<Spoolin.Outcome>,
    ) {
        forwardCatalog(connector = currentConnector(), response) { token ->
            catalogStub(token).approveAndCreate(request)
        }
    }

    override fun rejectMovie(
        request: Spoolin.ApprovalInput,
        response: StreamObserver<Spoolin.Outcome>,
    ) {
        forwardCatalog(connector = currentConnector(), response) { token ->
            catalogStub(token).rejectProposal(request)
        }
    }

    override fun getMovieOperation(
        request: Spoolin.Id,
        response: StreamObserver<Spoolin.Outcome>,
    ) {
        forwardCatalog(connector = currentConnector(), response) { token ->
            catalogStub(token).getOperation(request)
        }
    }

    private fun <T> forwardCatalog(
        connector: com.spoolin.core.features.connector.ConnectorRecord?,
        response: StreamObserver<T>,
        call: (String) -> T,
    ) {
        if (connector == null || issuer == null) return deny(response)
        if (!Permissions.management(roleFor(connector))) return deny(response)
        val operationId = UUID.randomUUID()
        val token =
            issuer.issue(
                connector,
                roleFor(connector),
                "/spoolin.v1.CatalogService/ProposeMovie",
                operationId,
                UUID.randomUUID(),
            )
        try {
            response.onNext(call(token))
            response.onCompleted()
        } catch (failure: io.grpc.StatusRuntimeException) {
            response.onError(failure)
        }
    }

    private fun catalogStub(token: String): CatalogServiceGrpc.CatalogServiceBlockingStub {
        val metadata = Metadata().apply { put(ActorRpc.metadata, token) }
        return catalog
            .withDeadlineAfter(5, TimeUnit.SECONDS)
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
    }

    private fun currentConnector(): com.spoolin.core.features.connector.ConnectorRecord? {
        val hash = ConnectorRpcContext.tokenHash.get() ?: return null
        val record = connectors.findByTokenHash(hash) ?: return null
        return if (connectors.live(record)) record else null
    }

    private fun roleFor(connector: com.spoolin.core.features.connector.ConnectorRecord): String =
        connectors.accountRole(connector.accountId) ?: "VIEWER"

    private fun <T> deny(response: StreamObserver<T>) {
        response.onError(Status.PERMISSION_DENIED.withDescription("CONNECTOR_DENIED").asRuntimeException())
    }
}
