package com.spoolin.catalog.features.lifecycle

import com.spoolin.catalog.operations.receipts.OwnerReceipts
import io.grpc.stub.StreamObserver
import spoolin.v1.Spoolin.Lifecycle
import spoolin.v1.Spoolin.Outcome
import javax.sql.DataSource

class PersonLifecycle(
    dataSource: DataSource,
    private val receipts: OwnerReceipts,
    tokens: ImpactToken? = null,
    private val policyRevision: () -> String = {
        com.spoolin.support.policy.PolicyEnvironment
            .resolve(System::getenv)
            .revision
    },
) {
    private val impacts = DeletionImpact(dataSource, tokens)

    fun apply(
        request: Lifecycle,
        response: StreamObserver<Outcome>,
    ) {
        LifecycleExecution(receipts, policyRevision).apply(request, response, "PersonLifecycle") { connection, command ->
            LifecycleStore().person(connection, command, impacts)
        }
    }
}
