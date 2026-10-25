package com.spoolin.support.observability

import com.google.protobuf.ByteString
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.Descriptors
import com.google.protobuf.DynamicMessage
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.Status
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LogicalWireClassificationRulesTest {
    private fun message(
        name: String,
        fields: Map<String, Any> = emptyMap(),
    ): DynamicMessage {
        val descriptor = schema.findMessageTypeByName(name)
        return DynamicMessage
            .newBuilder(descriptor)
            .also { builder ->
                fields.forEach { (key, value) -> builder.setField(descriptor.findFieldByName(key), value) }
            }.build()
    }

    private fun result(
        name: String,
        fields: Map<String, Any> = emptyMap(),
        lookup: Boolean = false,
    ): LogicalResult =
        LogicalClassification.message(
            MetricOperation("read", "owner", "spoolin.v1.$name", false, lookup),
            message(name, fields),
        )

    @Test
    fun `only present expected identities make object replies successful`() {
        val identities =
            mapOf(
                "Title" to "id",
                "Person" to "id",
                "Asset" to "id",
                "Attachment" to "hold_id",
                "ArtworkGrant" to "asset_id",
                "ProposalRevision" to "proposal_id",
                "PolicySnapshot" to "revision",
            )
        identities.forEach { (name, field) ->
            assertEquals(LogicalResult.success(), result(name, mapOf(field to "source-owned-id")))
            assertEquals(LogicalResult.unknown(), result(name))
            assertEquals(LogicalResult.unknown(), result(name, mapOf(field to " ")))
        }
        assertEquals(LogicalResult.unknown(), result("Other"))
        assertEquals(
            LogicalResult.unknown(),
            LogicalClassification.message(
                MetricOperation("read", "owner", "spoolin.v1.Title", false),
                message("Person"),
            ),
        )
        assertEquals(
            LogicalResult.unknown(),
            LogicalClassification.message(MetricOperation("read", "owner", "spoolin.v1.Title", false), "not protobuf"),
        )
    }

    @Test
    fun `collections empty bytes policy and safe problems retain their distinct outcomes`() {
        for (name in listOf("TitlePage", "PersonPage", "CreditPage", "Genres", "Roles", "AuditPage", "Impact")) {
            assertEquals(LogicalResult.success(), result(name))
        }
        assertEquals(LogicalResult.unknown(), result("MovieContext"))
        assertEquals(
            LogicalResult.success(),
            result(
                "MovieContext",
                mapOf("policy" to message("PolicySnapshot", mapOf("revision" to "p1"))),
            ),
        )
        assertEquals(LogicalResult.unknown(), result("AssetReadChunk"))
        assertEquals(
            LogicalResult.success(),
            result("AssetReadChunk", mapOf("data" to ByteString.copyFromUtf8("bytes"))),
        )
        assertEquals(
            MetricCode.CONFLICT,
            result(
                "Title",
                mapOf("problem" to message("Problem", mapOf("code" to "CONFLICT"))),
            ).code,
        )
        assertEquals(LogicalResult.unknown(), result("Title", mapOf("problem" to message("Problem"))))
    }

    @Test
    fun `operation lookup success is independent from a persisted failed command`() {
        for (state in listOf("SUCCEEDED", "FAILED", "PENDING", "EXPIRED")) {
            assertEquals(
                LogicalResult.success(),
                result("Outcome", mapOf("operation_id" to "id", "state" to state), true),
            )
        }
        assertEquals(LogicalResult.unknown(), result("Outcome", mapOf("state" to "SUCCEEDED"), true))
        assertEquals(
            LogicalResult.unknown(),
            result("Outcome", mapOf("operation_id" to "id", "state" to "UNKNOWN"), true),
        )
        assertEquals(LogicalResult.success(), result("Outcome", mapOf("state" to "SUCCEEDED")))
        assertEquals(MetricCode.PENDING, result("Outcome", mapOf("state" to "PENDING")).code)
        val failed =
            message(
                "Outcome",
                mapOf(
                    "state" to "FAILED",
                    "problem" to message("Problem", mapOf("code" to "FIELD_INVALID")),
                ),
            )
        assertEquals(MetricCode.FIELD_INVALID, result("ArtworkCancelOutcome", mapOf("operation" to failed)).code)
        assertEquals(LogicalResult.unknown(), result("ArtworkCancelOutcome"))
    }

    @Test
    fun `logical metric labels and duration require maintained operation names and boundaries`() {
        val registry = SimpleMeterRegistry()
        try {
            val metrics = LogicalMetrics(registry, "catalog")
            assertTrue(metrics.complete("SearchTitles", "owner", LogicalResult.success(), System.nanoTime()))
            assertTrue(
                metrics.complete(
                    "WriteMovie",
                    "owner",
                    LogicalResult.fromStatus(Status.UNAVAILABLE, true),
                    Long.MAX_VALUE,
                ),
            )
            assertFalse(metrics.complete("private-request-text", "owner", LogicalResult.success(), 0))
            assertFalse(metrics.complete("WriteMovie", "ingress", LogicalResult.success(), 0))
            assertEquals(
                1.0,
                registry
                    .get(
                        "spoolin.operation.results",
                    ).tags("operation", "SearchTitles", "outcome", "SUCCESS")
                    .counter()
                    .count(),
            )
            assertEquals(
                1L,
                registry
                    .get("spoolin.operation.duration")
                    .tag("operation", "WriteMovie")
                    .timer()
                    .count(),
            )
            assertEquals(
                0.0,
                registry
                    .get(
                        "spoolin.operation.duration",
                    ).tag("operation", "WriteMovie")
                    .timer()
                    .totalTime(java.util.concurrent.TimeUnit.SECONDS),
            )
        } finally {
            registry.close()
        }
    }

    @Test
    fun `RPC observations record exactly one outcome for success cancellation and uncertain completion`() {
        for (mode in listOf("success", "unavailable", "cancel", "incomplete", "start-failed", "close-failed")) {
            val registry = SimpleMeterRegistry()
            try {
                val probe = RpcBoundary("spoolin.v1.CatalogService/SearchTitles", mode == "close-failed")
                val interceptor = LogicalRpcMetricsInterceptor(LogicalMetrics(registry, "catalog"))
                val handler =
                    ServerCallHandler<Any, Any> { call, _ ->
                        if (mode == "start-failed") error("handler failed")
                        if (mode in setOf("success", "close-failed")) {
                            call.sendMessage(message("TitlePage"))
                            call.close(Status.OK, Metadata())
                        } else if (mode == "unavailable") {
                            call.close(Status.UNAVAILABLE, Metadata())
                        }
                        object : ServerCall.Listener<Any>() {}
                    }
                if (mode.endsWith("failed")) {
                    assertThrows(
                        IllegalStateException::class.java,
                    ) { interceptor.interceptCall(probe, Metadata(), handler) }
                } else {
                    val listener = interceptor.interceptCall(probe, Metadata(), handler)
                    if (mode == "cancel") listener.onCancel()
                    listener.onComplete()
                    listener.onCancel()
                }
                val expected =
                    when (mode) {
                        "success" -> "OK"
                        "unavailable" -> "UNAVAILABLE"
                        "cancel" -> "CANCELLED"
                        else -> "UNKNOWN"
                    }
                assertEquals(
                    1.0,
                    registry
                        .get("spoolin.operation.results")
                        .tag("code", expected)
                        .counter()
                        .count(),
                )
                assertEquals(1L, registry.get("spoolin.operation.duration").timer().count())
            } finally {
                registry.close()
            }
        }
    }

    @Test
    fun `unknown RPC names bypass measurements and conflicting streamed replies preserve the worst result`() {
        val registry = SimpleMeterRegistry()
        try {
            val interceptor = LogicalRpcMetricsInterceptor(LogicalMetrics(registry, "catalog"))
            interceptor.interceptCall(
                RpcBoundary("untrusted/PrivateName"),
                Metadata(),
                ServerCallHandler<Any, Any> {
                    _,
                    _,
                    ->
                    object : ServerCall.Listener<Any>() {}
                },
            )
            assertTrue(registry.meters.isEmpty())
            interceptor.interceptCall(
                RpcBoundary("spoolin.v1.CatalogService/WriteMovie"),
                Metadata(),
                ServerCallHandler<Any, Any> { call, _ ->
                    call.sendMessage(message("Outcome", mapOf("state" to "SUCCEEDED")))
                    call.sendMessage(message("Outcome", mapOf("state" to "PENDING")))
                    call.close(Status.OK, Metadata())
                    object : ServerCall.Listener<Any>() {}
                },
            )
            assertEquals(
                1.0,
                registry
                    .get("spoolin.operation.results")
                    .tag("code", "PENDING")
                    .counter()
                    .count(),
            )
        } finally {
            registry.close()
        }
    }

    private class RpcBoundary(
        private val method: String,
        private val failClose: Boolean = false,
    ) : ServerCall<Any, Any>() {
        override fun request(messages: Int) {}

        override fun sendHeaders(headers: Metadata) {}

        override fun sendMessage(message: Any) {}

        override fun close(
            status: Status,
            trailers: Metadata,
        ) {
            if (failClose) error("transport close failed")
        }

        override fun isCancelled(): Boolean = false

        override fun getMethodDescriptor(): MethodDescriptor<Any, Any> {
            val marshaller =
                object : MethodDescriptor.Marshaller<Any> {
                    override fun stream(value: Any): java.io.InputStream = java.io.ByteArrayInputStream(byteArrayOf())

                    override fun parse(stream: java.io.InputStream): Any = "unused transport bytes"
                }
            return MethodDescriptor
                .newBuilder<Any, Any>()
                .setType(
                    MethodDescriptor.MethodType.UNARY,
                ).setFullMethodName(method)
                .setRequestMarshaller(marshaller)
                .setResponseMarshaller(marshaller)
                .build()
        }
    }

    companion object {
        private fun descriptor(
            name: String,
            fields: List<Pair<String, String>>,
        ): DescriptorProtos.DescriptorProto {
            val builder = DescriptorProtos.DescriptorProto.newBuilder().setName(name)
            fields.forEachIndexed { index, (field, type) ->
                val value =
                    DescriptorProtos.FieldDescriptorProto
                        .newBuilder()
                        .setName(field)
                        .setNumber(index + 1)
                when (type) {
                    "string" -> value.type = DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING
                    "bytes" -> value.type = DescriptorProtos.FieldDescriptorProto.Type.TYPE_BYTES
                    else -> {
                        value.type = DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE
                        value.typeName =
                            ".spoolin.v1.$type"
                    }
                }
                builder.addField(value)
            }
            return builder.build()
        }

        private val schema: Descriptors.FileDescriptor =
            run {
                val file =
                    DescriptorProtos.FileDescriptorProto
                        .newBuilder()
                        .setName(
                            "unit-observable-replies.proto",
                        ).setPackage("spoolin.v1")
                        .setSyntax("proto3")
                val definitions =
                    mapOf(
                        "Problem" to listOf("code" to "string"),
                        "Outcome" to listOf("operation_id" to "string", "state" to "string", "problem" to "Problem"),
                        "PolicySnapshot" to listOf("revision" to "string"),
                        "MovieContext" to listOf("policy" to "PolicySnapshot"),
                        "AssetReadChunk" to listOf("data" to "bytes"),
                        "ArtworkCancelOutcome" to listOf("operation" to "Outcome"),
                        "Title" to listOf("id" to "string", "problem" to "Problem"),
                        "Person" to listOf("id" to "string"),
                        "Asset" to listOf("id" to "string"),
                        "Attachment" to listOf("hold_id" to "string"),
                        "ArtworkGrant" to listOf("asset_id" to "string"),
                        "ProposalRevision" to listOf("proposal_id" to "string"),
                    )
                definitions.forEach { (name, fields) -> file.addMessageType(descriptor(name, fields)) }
                listOf(
                    "TitlePage",
                    "PersonPage",
                    "CreditPage",
                    "Genres",
                    "Roles",
                    "AuditPage",
                    "Impact",
                    "Other",
                ).forEach {
                    file.addMessageType(descriptor(it, emptyList()))
                }
                Descriptors.FileDescriptor.buildFrom(file.build(), emptyArray())
            }
    }
}
