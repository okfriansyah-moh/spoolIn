package com.spoolin.catalog.features.lifecycle

import com.google.protobuf.Timestamp
import com.spoolin.support.receipts.executeBound
import io.grpc.Status
import io.grpc.stub.StreamObserver
import spoolin.v1.Spoolin.Id
import spoolin.v1.Spoolin.Impact
import java.sql.Connection
import java.util.UUID
import javax.sql.DataSource

class DeletionImpact(
    private val dataSource: DataSource,
    private val tokens: ImpactToken? = null,
) {
    fun preview(
        request: Id,
        response: StreamObserver<Impact>,
    ) {
        try {
            LifecycleAuthorization.management()
            val snapshot = readSnapshot(parseId(request.id))
            response.onNext(project(snapshot))
            response.onCompleted()
        } catch (
            failure: io.grpc.StatusRuntimeException,
        ) {
            response.onError(failure)
        } catch (
            _: IllegalArgumentException,
        ) {
            response.onError(Status.INVALID_ARGUMENT.asRuntimeException())
        }
    }

    private fun readSnapshot(person: UUID): ImpactSnapshot =
        dataSource.connection.use { connection ->
            connection.transactionIsolation = Connection.TRANSACTION_REPEATABLE_READ
            connection.isReadOnly = true
            connection.autoCommit = false
            readTransaction(connection, person)
        }

    private fun readTransaction(connection: Connection, person: UUID): ImpactSnapshot {
        var committed = false
        return try {
            connection.executeBound("SET LOCAL statement_timeout='3000ms'")
            val result = ImpactSnapshot.read(connection, person)
            connection.commit()
            committed = true
            result
        } finally {
            if (!committed) connection.rollback()
        }
    }

    fun verify(
        connection: Connection,
        person: UUID,
        token: String?,
    ): ImpactSnapshot {
        val signer = signer()
        val current = ImpactSnapshot.read(connection, person)
        project(current)
        if (!signer.matches(token, current)) throw Status.ABORTED.withDescription("IMPACT_CHANGED").asRuntimeException()
        return current
    }

    fun project(snapshot: ImpactSnapshot): Impact {
        val signer = signer()
        val signed = signer.issue(snapshot)
        val result =
            Impact
                .newBuilder()
                .setTargetId(snapshot.person.toString())
                .setVersion(snapshot.version)
                .setToken(signed.token)
                .setAffectedCreditCount(ImpactSnapshot.exactCount(snapshot.creditCount))
                .addAllAffectedTitles(snapshot.affectedTitles)
                .setExpiresAt(
                    Timestamp.newBuilder().setSeconds(signed.expires.epochSecond).setNanos(signed.expires.nano),
                )
                .build()
        val json =
            tools.jackson.databind.ObjectMapper().writeValueAsBytes(
                mapOf(
                    "data" to
                        mapOf(
                            "deletionImpact" to
                                mapOf(
                                    "targetId" to result.targetId,
                                    "token" to result.token,
                                    "affectedCreditCount" to result.affectedCreditCount,
                                    "affectedTitles" to result.affectedTitlesList,
                                ),
                        ),
                ),
            )
        if (result.serializedSize > ImpactSnapshot.RESPONSE_BYTES || json.size > ImpactSnapshot.RESPONSE_BYTES) {
            throw Status.RESOURCE_EXHAUSTED.withDescription("IMPACT_RESPONSE_CAPACITY_EXCEEDED").asRuntimeException()
        }
        return result
    }

    private fun signer(): ImpactToken = tokens ?: throw Status.UNAVAILABLE
        .withDescription("IMPACT_SIGNER_UNAVAILABLE").asRuntimeException()

    private fun parseId(value: String): UUID = UUID.fromString(value).also { require(it.toString() == value) }
}
