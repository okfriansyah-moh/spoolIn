package com.spoolin.core.features.mcpgateway

import com.google.protobuf.Empty
import com.spoolin.core.features.connector.ConnectorStore
import com.spoolin.core.features.policy.PolicyProvider
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.McpGatewayServiceGrpc
import spoolin.v1.Spoolin
import java.util.UUID

private const val MAX_CONTEXT_TITLES = 20
private const val CATALOG_SERVICE = "/spoolin.v1.CatalogService/"
private const val GATEWAY_SERVICE = "/spoolin.v1.McpGatewayService/"

class McpGatewayRpc(
    connectors: ConnectorStore,
    private val policy: PolicyProvider,
    catalog: CatalogServiceGrpc.CatalogServiceBlockingStub,
    issuer: McpActorIssuer?,
    private val uiChallenges: ProposalUiChallengeStore,
    private val uiAssertions: UiAssertionVerifier? = null,
) : McpGatewayServiceGrpc.McpGatewayServiceImplBase() {
    private val access = McpCatalogAccess(connectors, catalog, issuer)

    override fun getPolicy(
        request: Empty,
        response: StreamObserver<Spoolin.PolicySnapshot>,
    ) {
        if (access.managementConnector() == null) {
            response.onError(Status.PERMISSION_DENIED.withDescription("CONNECTOR_DENIED").asRuntimeException())
        } else {
            response.onNext(policy.protobuf())
            response.onCompleted()
        }
    }

    override fun getMovieContext(
        request: Spoolin.MovieContextRequest,
        response: StreamObserver<Spoolin.MovieContext>,
    ) {
        val correlation = UUID.randomUUID()
        access.forward(McpCatalogInvocation(CATALOG_SERVICE + "SearchTitles", null, correlation), response) { stub ->
            val titles =
                stub.searchTitles(
                    Spoolin.Search
                        .newBuilder()
                        .setText(request.query)
                        .setFirst(request.limit.coerceIn(1, MAX_CONTEXT_TITLES))
                        .build(),
                )
            val genres =
                access.invoke(McpCatalogInvocation(CATALOG_SERVICE + "ListGenres", null, correlation)) {
                    it.listGenres(Empty.getDefaultInstance())
                }
            val roles =
                access.invoke(McpCatalogInvocation(CATALOG_SERVICE + "ListRoles", null, correlation)) {
                    it.listRoles(Empty.getDefaultInstance())
                }
            Spoolin.MovieContext
                .newBuilder()
                .setTitles(titles)
                .setGenres(genres)
                .setRoles(roles)
                .setPolicy(policy.protobuf())
                .build()
        }
    }

    override fun proposeMovie(
        request: Spoolin.Proposal,
        response: StreamObserver<Spoolin.ProposalRevision>,
    ) {
        val invocation =
            McpCatalogInvocation(
                CATALOG_SERVICE + "ProposeMovie",
                UUID.fromString(request.context.operationId),
                UUID.fromString(request.context.correlationId),
            )
        access.forward(invocation, response) { stub ->
            val revision = stub.proposeMovie(request)
            if (revision.hasUiChallenge() && !revision.uiChallenge.isEmpty) {
                uiChallenges.put(
                    revision.proposalId,
                    revision.revision,
                    revision.contentHash,
                    revision.uiChallenge.toStringUtf8(),
                )
            }
            // Adapter relocates ui_challenge into private UI metadata and omits model content.
            revision
        }
    }

    override fun approveMovie(
        request: Spoolin.ApprovalInput,
        response: StreamObserver<Spoolin.Outcome>,
    ) {
        if (requireTrustedUi(request, GATEWAY_SERVICE + "ApproveMovie", response)) {
            access.forward(McpCatalogInvocation(CATALOG_SERVICE + "ApproveAndCreate"), response) {
                it.approveAndCreate(request)
            }
        }
    }

    fun approveMovieBlocking(request: Spoolin.ApprovalInput): Spoolin.Outcome =
        forwardTrustedCatalogBlocking(request, "ApproveMovie", "ApproveAndCreate") { it.approveAndCreate(request) }

    override fun rejectMovie(
        request: Spoolin.ApprovalInput,
        response: StreamObserver<Spoolin.Outcome>,
    ) {
        if (requireTrustedUi(request, GATEWAY_SERVICE + "RejectMovie", response)) {
            access.forward(McpCatalogInvocation(CATALOG_SERVICE + "RejectProposal"), response) {
                it.rejectProposal(request)
            }
        }
    }

    fun rejectMovieBlocking(request: Spoolin.ApprovalInput): Spoolin.Outcome =
        forwardTrustedCatalogBlocking(request, "RejectMovie", "RejectProposal") { it.rejectProposal(request) }

    override fun getMovieOperation(
        request: Spoolin.Id,
        response: StreamObserver<Spoolin.Outcome>,
    ) {
        access.forward(McpCatalogInvocation(CATALOG_SERVICE + "GetOperation", UUID.fromString(request.id)), response) {
            it.getOperation(request)
        }
    }

    private fun <T> requireTrustedUi(
        request: Spoolin.ApprovalInput,
        method: String,
        response: StreamObserver<T>,
    ): Boolean =
        try {
            val connector = access.currentConnector() ?: denied()
            val verifier = uiAssertions ?: denied()
            verifier.requireTrustedUi(connector, request, method)
            true
        } catch (failure: StatusRuntimeException) {
            response.onError(failure)
            false
        }

    private fun <T> forwardTrustedCatalogBlocking(
        request: Spoolin.ApprovalInput,
        method: String,
        catalogMethod: String,
        call: (CatalogServiceGrpc.CatalogServiceBlockingStub) -> T,
    ): T {
        val connector = access.currentConnector() ?: denied()
        val verifier = uiAssertions ?: denied()
        verifier.requireTrustedUi(connector, request, GATEWAY_SERVICE + method)
        return access.invoke(McpCatalogInvocation(CATALOG_SERVICE + catalogMethod), call)
    }
}
