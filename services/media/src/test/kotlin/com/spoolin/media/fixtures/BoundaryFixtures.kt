package com.spoolin.media.fixtures

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.Status
import io.grpc.stub.StreamObserver
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.Spoolin.Attachment
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.ArrayDeque
import java.util.UUID
import javax.sql.DataSource

/** Queued JDBC results model external responses without implementing database predicates. */
class JdbcBoundary(
    var failure: Throwable? = null,
) {
    val queries = ArrayDeque<List<Map<String, Any?>>>()
    val updates = ArrayDeque<Int>()
    var commits = 0
    var rollbacks = 0
    val source: DataSource =
        proxy(DataSource::class.java) { method, _ ->
            when (method) {
                "getConnection" -> {
                    failure?.let { throw it }
                    connection
                }
                else -> null
            }
        }
    val connection: Connection =
        proxy(Connection::class.java) { method, _ ->
            when (method) {
                "prepareStatement" -> statement()
                "commit" -> {
                    commits++
                    null
                }
                "rollback" -> {
                    rollbacks++
                    null
                }
                else -> null
            }
        }

    fun rows(vararg rows: Map<String, Any?>): JdbcBoundary = apply { queries.add(rows.toList()) }

    private fun statement(): PreparedStatement =
        proxy(PreparedStatement::class.java) { method, _ ->
            when (method) {
                "executeUpdate" -> if (updates.isEmpty()) 1 else updates.removeFirst()
                "executeQuery" -> result(if (queries.isEmpty()) emptyList() else queries.removeFirst())
                else -> null
            }
        }

    private fun result(rows: List<Map<String, Any?>>): ResultSet {
        var position = -1
        var last: Any? = null
        return proxy(ResultSet::class.java) { method, args ->
            when (method) {
                "next" -> {
                    position++
                    position < rows.size
                }
                "wasNull" -> last == null
                "getObject", "getString", "getBytes", "getTimestamp", "getInt", "getLong", "getBoolean" -> {
                    last = rows[position][args!![0].toString()]
                    when (method) {
                        "getInt" -> (last as? Number)?.toInt() ?: 0
                        "getLong" -> (last as? Number)?.toLong() ?: 0L
                        "getBoolean" -> last ?: false
                        else -> last
                    }
                }
                else -> null
            }
        }
    }
}

private fun <T> proxy(
    type: Class<T>,
    invoke: (String, Array<out Any?>?) -> Any?,
): T =
    type.cast(
        Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, args ->
            when (method.name) {
                "toString" -> "Unit JDBC boundary"
                "hashCode" -> 1
                "equals" -> false
                else ->
                    invoke(method.name, args) ?: when (method.returnType) {
                        java.lang.Boolean.TYPE -> false
                        java.lang.Integer.TYPE -> 0
                        java.lang.Long.TYPE -> 0L
                        else -> null
                    }
            }
        },
    )

class CatalogBoundary(
    var attachment: Attachment = Attachment.getDefaultInstance(),
    var status: Status = Status.OK,
) {
    val client =
        CatalogServiceGrpc.newBlockingStub(
            object : Channel() {
                override fun authority(): String = "unit-catalog"

                override fun <ReqT : Any, RespT : Any> newCall(
                    method: MethodDescriptor<ReqT, RespT>,
                    options: CallOptions,
                ): ClientCall<ReqT, RespT> =
                    object : ClientCall<ReqT, RespT>() {
                        private lateinit var listener: Listener<RespT>

                        override fun start(
                            value: Listener<RespT>,
                            headers: Metadata,
                        ) {
                            listener = value
                        }

                        override fun request(count: Int) {}

                        override fun cancel(
                            message: String?,
                            cause: Throwable?,
                        ) {}

                        override fun sendMessage(message: ReqT) {}

                        override fun halfClose() {
                            if (status.isOk) {
                                listener.onMessage(
                                    method.responseMarshaller.parse(java.io.ByteArrayInputStream(attachment.toByteArray())),
                                )
                            }
                            listener.onClose(status, Metadata())
                        }
                    }
            },
        )
}

class Response<T> : StreamObserver<T> {
    val values = mutableListOf<T>()
    var failure: Throwable? = null
    var completed = false

    override fun onNext(value: T) {
        values.add(value)
    }

    override fun onError(error: Throwable) {
        failure = error
    }

    override fun onCompleted() {
        completed = true
    }

    val code: Status.Code? get() = failure?.let { Status.fromThrowable(it).code }
}

val assetId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
val actorId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")
val operationId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000003")
val holdId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000004")
val titleId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000005")
val instant: Instant = Instant.parse("2026-01-02T03:04:05Z")

fun assetRecord(state: String = "READY"): Map<String, Any?> =
    mapOf(
        "id" to assetId,
        "actor_id" to actorId,
        "upload_operation_id" to operationId,
        "state" to state,
        "storage_key" to "output/$assetId.png",
        "content_hash" to byteArrayOf(1, 2, 3),
        "format" to "PNG",
        "input_bytes" to 120L,
        "output_bytes" to 100L,
        "width" to 10,
        "height" to 20,
        "policy_revision" to "p1",
        "provenance" to "{}",
        "created_at" to Timestamp.from(instant),
        "updated_at" to Timestamp.from(instant),
        "processing_work_id" to operationId,
        "processing_staging_key" to "quarantine/$assetId",
        "processing_input_hash" to byteArrayOf(4),
        "processing_created_at" to Timestamp.from(instant),
    )
