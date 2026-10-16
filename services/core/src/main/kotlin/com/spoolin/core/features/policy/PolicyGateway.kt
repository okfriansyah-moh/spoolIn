package com.spoolin.core.features.policy

import com.google.protobuf.Empty
import io.grpc.Status
import io.grpc.stub.StreamObserver
import spoolin.v1.McpGatewayServiceGrpc
import spoolin.v1.Spoolin.PolicySnapshot

fun interface PolicyAccess {
    fun verifiedConnector(): Boolean
}

class PolicyGateway(
    private val provider: PolicyProvider,
    private val access: PolicyAccess,
) : McpGatewayServiceGrpc.McpGatewayServiceImplBase() {
    override fun getPolicy(
        request: Empty,
        responseObserver: StreamObserver<PolicySnapshot>,
    ) {
        if (!access.verifiedConnector()) {
            responseObserver.onError(
                Status.UNAVAILABLE.withDescription("Connector verification is unavailable").asRuntimeException(),
            )
            return
        }
        responseObserver.onNext(provider.protobuf())
        responseObserver.onCompleted()
    }
}
