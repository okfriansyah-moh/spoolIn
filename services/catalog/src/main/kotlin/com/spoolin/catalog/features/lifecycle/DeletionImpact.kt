package com.spoolin.catalog.features.lifecycle

import com.google.protobuf.Timestamp
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.identity.Permissions
import com.spoolin.support.receipts.executeBound
import io.grpc.Status
import io.grpc.stub.StreamObserver
import spoolin.v1.Spoolin.Id
import spoolin.v1.Spoolin.Impact
import java.sql.Connection
import java.util.UUID
import javax.sql.DataSource

class DeletionImpact(private val dataSource: DataSource, private val tokens: ImpactToken? = null) {
    fun preview(request: Id, response: StreamObserver<Impact>) {
        try {
            val claims = ActorRpc.current() ?: throw Status.PERMISSION_DENIED.asRuntimeException()
            if (!Permissions.management(claims.role)) throw Status.PERMISSION_DENIED.asRuntimeException()
            val person = parseId(request.id)
            val snapshot = dataSource.connection.use { connection ->
                connection.transactionIsolation = Connection.TRANSACTION_REPEATABLE_READ
                connection.isReadOnly = true; connection.autoCommit = false
                try {
                    connection.executeBound("SET LOCAL statement_timeout='3000ms'")
                    val result = ImpactSnapshot.read(connection, person)
                    connection.commit(); result
                } catch (failure: Exception) { connection.rollback(); throw failure }
            }
            response.onNext(project(snapshot)); response.onCompleted()
        } catch (failure: io.grpc.StatusRuntimeException) { response.onError(failure) }
        catch (_: IllegalArgumentException) { response.onError(Status.INVALID_ARGUMENT.asRuntimeException()) }
    }

    fun verify(connection: Connection, person: UUID, token: String?): ImpactSnapshot {
        val signer = tokens ?: throw Status.UNAVAILABLE.withDescription("IMPACT_SIGNER_UNAVAILABLE").asRuntimeException()
        val current = ImpactSnapshot.read(connection, person)
        if (!signer.matches(token, current)) throw Status.ABORTED.withDescription("IMPACT_CHANGED").asRuntimeException()
        return current
    }

    fun project(snapshot: ImpactSnapshot): Impact {
        val signer = tokens ?: throw Status.UNAVAILABLE.withDescription("IMPACT_SIGNER_UNAVAILABLE").asRuntimeException()
        val signed = signer.issue(snapshot)
        val titles = snapshot.credits.distinctBy { it.title }.sortedWith(compareBy({ it.titleName }, { it.title.toString() }))
        return Impact.newBuilder().setTargetId(snapshot.person.toString()).setVersion(snapshot.version)
            .setToken(signed.token).setAffectedCreditCount(snapshot.credits.size).addAllAffectedTitles(titles.map { it.titleName })
            .setExpiresAt(Timestamp.newBuilder().setSeconds(signed.expires.epochSecond).setNanos(signed.expires.nano)).build()
    }

    private fun parseId(value: String): UUID = UUID.fromString(value).also { require(it.toString() == value) }
}
