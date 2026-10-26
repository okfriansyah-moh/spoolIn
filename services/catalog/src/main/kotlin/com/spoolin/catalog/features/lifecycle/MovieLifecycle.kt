package com.spoolin.catalog.features.lifecycle

import com.spoolin.catalog.operations.receipts.OwnerReceipts
import io.grpc.stub.StreamObserver
import spoolin.v1.Spoolin.Lifecycle
import spoolin.v1.Spoolin.Outcome

class MovieLifecycle(
    private val receipts: OwnerReceipts,
    private val policyRevision: () -> String = {
        com.spoolin.support.policy.PolicyEnvironment
            .resolve(System::getenv)
            .revision
    },
) {
    fun apply(
        request: Lifecycle,
        response: StreamObserver<Outcome>,
    ) {
        LifecycleExecution(receipts, policyRevision).apply(request, response, "MovieLifecycle") { connection, command ->
            LifecycleStore().movie(connection, command)
        }
    }
}
