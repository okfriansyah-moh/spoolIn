package com.spoolin.catalog.features.lifecycle

import com.spoolin.support.receipts.queryBound
import io.grpc.Status
import io.grpc.stub.StreamObserver
import spoolin.v1.Spoolin.Id
import spoolin.v1.Spoolin.Impact
import java.util.Base64
import java.util.UUID
import javax.sql.DataSource

class DeletionImpact(private val dataSource: DataSource) {
    fun preview(request: Id, response: StreamObserver<Impact>) {
        try {
            val personId = parseUuid(request.id)
            val titles =
                dataSource.connection.use { connection ->
                    connection.queryBound(
                        "SELECT DISTINCT t.display_title FROM credit c JOIN title t ON t.id = c.title_id" +
                            " WHERE c.person_id = ? AND t.status IN ('ACTIVE','ARCHIVED') ORDER BY t.display_title LIMIT 20",
                        listOf(personId),
                    ) { it.getString(1) }
                }
            val token = Base64.getEncoder().encodeToString("$personId:${titles.size}".toByteArray())
            response.onNext(
                Impact.newBuilder()
                    .setTargetId(personId.toString())
                    .setVersion(0)
                    .setToken(token)
                    .setAffectedCreditCount(titles.size)
                    .addAllAffectedTitles(titles)
                    .build(),
            )
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        }
    }

    private fun parseUuid(value: String): UUID =
        runCatching { UUID.fromString(value).also { require(it.toString() == value) } }
            .getOrElse { throw Status.INVALID_ARGUMENT.asRuntimeException() }
}
