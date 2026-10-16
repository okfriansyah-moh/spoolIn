package com.spoolin.core.boundary.grpc

import com.google.protobuf.Timestamp
import com.spoolin.core.features.session.SessionStore
import io.grpc.Status
import io.grpc.stub.StreamObserver
import spoolin.v1.IdentityServiceGrpc
import spoolin.v1.Spoolin.Actor
import spoolin.v1.Spoolin.CheckActorRequest
import java.util.UUID

class IdentityRpc(
    private val sessions: () -> SessionStore?,
) : IdentityServiceGrpc.IdentityServiceImplBase() {
    override fun checkActor(
        request: CheckActorRequest,
        response: StreamObserver<Actor>,
    ) {
        try {
            require(request.audience == "catalog" && request.authEpoch >= 0)
            val account = uuid(request.accountId)
            val credential = uuid(request.credentialId)
            val store = sessions() ?: throw Status.UNAVAILABLE.asRuntimeException()
            val current =
                store.checkActor(account, credential, request.authEpoch)
                    ?: throw Status.PERMISSION_DENIED.asRuntimeException()
            response.onNext(
                Actor
                    .newBuilder()
                    .setAccountId(current.account.id.toString())
                    .setCredentialId(current.credentialId.toString())
                    .setRole(current.account.role.name)
                    .setAuthEpoch(current.account.authEpoch)
                    .setExpiresAt(
                        Timestamp
                            .newBuilder()
                            .setSeconds(current.expiresAt.epochSecond)
                            .setNanos(current.expiresAt.nano)
                            .build(),
                    ).build(),
            )
            response.onCompleted()
        } catch (failure: io.grpc.StatusRuntimeException) {
            response.onError(failure)
        } catch (_: IllegalArgumentException) {
            response.onError(Status.PERMISSION_DENIED.asRuntimeException())
        } catch (_: Exception) {
            response.onError(Status.UNAVAILABLE.asRuntimeException())
        }
    }

    private fun uuid(value: String): UUID = UUID.fromString(value).also { require(it.toString() == value) }
}
