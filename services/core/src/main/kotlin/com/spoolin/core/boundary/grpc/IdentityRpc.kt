package com.spoolin.core.boundary.grpc

import com.google.protobuf.Timestamp
import com.spoolin.core.features.connector.ConnectorStore
import com.spoolin.core.features.session.SessionStore
import io.grpc.Status
import io.grpc.stub.StreamObserver
import spoolin.v1.IdentityServiceGrpc
import spoolin.v1.Spoolin.Actor
import spoolin.v1.Spoolin.CheckActorRequest
import java.util.UUID

class IdentityRpc(
    private val sessions: () -> SessionStore?,
    private val connectors: () -> ConnectorStore?,
) : IdentityServiceGrpc.IdentityServiceImplBase() {
    override fun checkActor(
        request: CheckActorRequest,
        response: StreamObserver<Actor>,
    ) {
        try {
            require(request.audience == "catalog" && request.authEpoch >= 0)
            val account = uuid(request.accountId)
            val credential = uuid(request.credentialId)
            val sessionStore = sessions()
            val session = sessionStore?.checkActor(account, credential, request.authEpoch)
            if (session != null) {
                response.onNext(
                    Actor
                        .newBuilder()
                        .setAccountId(session.account.id.toString())
                        .setCredentialId(session.credentialId.toString())
                        .setRole(session.account.role.name)
                        .setAuthEpoch(session.account.authEpoch)
                        .setExpiresAt(
                            Timestamp
                                .newBuilder()
                                .setSeconds(session.expiresAt.epochSecond)
                                .setNanos(session.expiresAt.nano)
                                .build(),
                        ).build(),
                )
                response.onCompleted()
                return
            }
            val connectorStore = connectors() ?: throw Status.UNAVAILABLE.asRuntimeException()
            val connector =
                connectorStore.findByCredential(credential, account, request.authEpoch)
                    ?: throw Status.PERMISSION_DENIED.asRuntimeException()
            if (!connectorStore.live(connector)) throw Status.PERMISSION_DENIED.asRuntimeException()
            val role = connectorStore.accountRole(connector.accountId) ?: "VIEWER"
            response.onNext(
                Actor
                    .newBuilder()
                    .setAccountId(connector.accountId.toString())
                    .setCredentialId(connector.id.toString())
                    .setRole(role)
                    .setAuthEpoch(connector.authEpoch)
                    .setClientKind(connector.clientKind)
                    .setExpiresAt(
                        Timestamp
                            .newBuilder()
                            .setSeconds(connector.expiresAt.epochSecond)
                            .setNanos(connector.expiresAt.nano)
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
