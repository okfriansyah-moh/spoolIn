package com.spoolin.catalog.features.lifecycle

import com.spoolin.catalog.operations.receipts.OwnerReceipts
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.identity.Permissions
import io.grpc.Status
import io.grpc.stub.StreamObserver
import spoolin.v1.Spoolin.Lifecycle
import spoolin.v1.Spoolin.Outcome

class MovieLifecycle(private val receipts: OwnerReceipts) {
    fun apply(request: Lifecycle, response: StreamObserver<Outcome>) {
        try {
            val claims = ActorRpc.current() ?: throw Status.PERMISSION_DENIED.asRuntimeException()
            if (!Permissions.management(claims.role)) throw Status.PERMISSION_DENIED.asRuntimeException()
            val command = LifecycleCommand.parse(request, claims.accountId)
            val result = receipts.execute(command.intent("MovieLifecycle"), command.correlation, true) { connection ->
                LifecycleStore().movie(connection, command)
            }
            response.onNext(lifecycleOutcome(result)); response.onCompleted()
        } catch (failure: io.grpc.StatusRuntimeException) { response.onError(failure) }
        catch (_: com.spoolin.support.receipts.ReceiptConflict) {
            response.onError(Status.ALREADY_EXISTS.withDescription("IDEMPOTENCY_CONFLICT").asRuntimeException())
        } catch (_: com.spoolin.support.receipts.ReceiptHidden) { response.onError(Status.NOT_FOUND.asRuntimeException()) }
    }
}
