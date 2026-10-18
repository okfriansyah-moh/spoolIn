package com.spoolin.core.boundary.grpc

import com.google.protobuf.Timestamp
import com.spoolin.core.features.connector.ConnectorStore
import com.spoolin.core.features.session.SessionStore
import com.spoolin.core.identity.ActorAuthority
import io.grpc.Status
import io.grpc.stub.StreamObserver
import spoolin.v1.IdentityServiceGrpc
import spoolin.v1.Spoolin.Actor
import spoolin.v1.Spoolin.CheckActorRequest
import java.time.Instant
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
            response.onNext(resolve(request))
            response.onCompleted()
        } catch (failure: io.grpc.StatusRuntimeException) {
            response.onError(failure)
        } catch (_: IllegalArgumentException) {
            response.onError(Status.PERMISSION_DENIED.asRuntimeException())
        } catch (_: Exception) {
            response.onError(Status.UNAVAILABLE.asRuntimeException())
        }
    }

    private fun resolve(request: CheckActorRequest): Actor {
        require(request.audience == "catalog" && request.authEpoch >= 0)
        val account = uuid(request.accountId)
        val credential = uuid(request.credentialId)
        val session = sessions()?.checkActor(account, credential, request.authEpoch)
        return session?.let(::sessionActor) ?: connectorActor(account, credential, request.authEpoch)
    }

    private fun sessionActor(session: ActorAuthority): Actor =
        Actor
            .newBuilder()
            .setAccountId(session.account.id.toString())
            .setCredentialId(session.credentialId.toString())
            .setRole(session.account.role.name)
            .setAuthEpoch(session.account.authEpoch)
            .setExpiresAt(timestamp(session.expiresAt))
            .build()

    private fun connectorActor(
        account: UUID,
        credential: UUID,
        epoch: Long,
    ): Actor {
        val store = availableConnectors()
        val connector =
            store.findByCredential(credential, account, epoch) ?: throw Status.PERMISSION_DENIED.asRuntimeException()
        if (!store.live(connector)) throw Status.PERMISSION_DENIED.asRuntimeException()
        val role = store.accountRole(connector.accountId) ?: "VIEWER"
        return Actor
            .newBuilder()
            .setAccountId(connector.accountId.toString())
            .setCredentialId(connector.id.toString())
            .setRole(role)
            .setAuthEpoch(connector.authEpoch)
            .setClientKind(connector.clientKind)
            .setExpiresAt(timestamp(connector.expiresAt))
            .build()
    }

    private fun availableConnectors(): ConnectorStore = connectors() ?: throw Status.UNAVAILABLE.asRuntimeException()

    private fun timestamp(value: Instant): Timestamp =
        Timestamp
            .newBuilder()
            .setSeconds(value.epochSecond)
            .setNanos(value.nano)
            .build()

    private fun uuid(value: String): UUID = UUID.fromString(value).also { require(it.toString() == value) }
}
