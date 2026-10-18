package com.spoolin.catalog.features.lifecycle

import com.spoolin.catalog.operations.receipts.OwnerReceipts
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.identity.Permissions
import io.grpc.Status
import io.grpc.stub.StreamObserver
import spoolin.v1.Spoolin.Lifecycle
import spoolin.v1.Spoolin.Outcome
import javax.sql.DataSource

class PersonLifecycle(dataSource: DataSource, private val receipts: OwnerReceipts, tokens: ImpactToken? = null) {
    private val impacts = DeletionImpact(dataSource, tokens)

    fun apply(request: Lifecycle, response: StreamObserver<Outcome>) {
        try {
            val claims = ActorRpc.current() ?: throw Status.PERMISSION_DENIED.asRuntimeException()
            if (!Permissions.management(claims.role)) throw Status.PERMISSION_DENIED.asRuntimeException()
            val command = LifecycleCommand.parse(request, claims.accountId)
            val result = receipts.execute(command.intent("PersonLifecycle"), command.correlation, true) { connection ->
                LifecycleStore().person(connection, command, impacts)
            }
            response.onNext(lifecycleOutcome(result)); response.onCompleted()
        } catch (failure: io.grpc.StatusRuntimeException) {
            if (failure.status.description == "IMPACT_CHANGED") {
                response.onNext(Outcome.newBuilder().setOperationId(request.target.context.operationId).setState("FAILED")
                    .setProblem(spoolin.v1.Spoolin.Problem.newBuilder().setCode("IMPACT_CHANGED")).build())
                response.onCompleted()
            } else response.onError(failure)
        }
        catch (_: com.spoolin.support.receipts.ReceiptConflict) {
            response.onError(Status.ALREADY_EXISTS.withDescription("IDEMPOTENCY_CONFLICT").asRuntimeException())
        } catch (_: com.spoolin.support.receipts.ReceiptHidden) { response.onError(Status.NOT_FOUND.asRuntimeException()) }
    }
}
