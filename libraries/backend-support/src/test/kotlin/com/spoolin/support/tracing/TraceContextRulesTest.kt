package com.spoolin.support.tracing

import com.spoolin.support.logging.DiagnosticEnvelope
import io.grpc.Attributes
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.Status
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TraceContextRulesTest {
    @Test
    fun `trusted traceparent continues through real SDK child span and callback scopes clear on executor reuse`() {
        Fixture().use { fixture ->
            val call = ServerProbe()
            var observed: Pair<String, String>? = null
            val listener =
                TraceServerInterceptor(fixture.tracing) { true }.interceptCall(
                    call,
                    headers(),
                    ServerCallHandler { wrapped, _ ->
                        object : ServerCall.Listener<String>() {
                            override fun onHalfClose() {
                                observed = TraceContexts.identifiers()
                                wrapped.close(Status.OK, Metadata())
                            }
                        }
                    },
                )
            assertEquals(ZERO_TRACE, TraceContexts.identifiers().first)
            val executor = Executors.newSingleThreadExecutor()
            try {
                executor
                    .submit {
                        listener.onHalfClose()
                        listener.onComplete()
                    }.get(2, TimeUnit.SECONDS)
                assertEquals(ZERO_TRACE, executor.submit<String> { TraceContexts.identifiers().first }.get())
            } finally {
                executor.shutdownNow()
            }
            val span = fixture.exporter.finishedSpanItems.single()
            assertEquals(TRACE, observed?.first)
            assertNotEquals(PARENT, observed?.second)
            assertEquals(PARENT, span.parentSpanId)
            assertEquals(observed?.second, span.spanId)
            assertEquals(1, call.closes)
        }
    }

    @Test
    fun `untrusted duplicate malformed and zero traceparents produce fresh SDK roots`() {
        Fixture().use { fixture ->
            val invalid =
                listOf(
                    headers(),
                    headers().also { it.put(PARENT_KEY, VALID) },
                    Metadata().also { it.put(PARENT_KEY, "private-invalid\nvalue") },
                    Metadata().also { it.put(PARENT_KEY, "00-$ZERO_TRACE-$PARENT-01") },
                )
            invalid.forEachIndexed { index, input ->
                val call = ServerProbe()
                TraceServerInterceptor(fixture.tracing) { index != 0 }.interceptCall(
                    call,
                    input,
                    ServerCallHandler { wrapped, _ ->
                        wrapped.close(Status.OK, Metadata())
                        object : ServerCall.Listener<String>() {}
                    },
                )
            }
            assertEquals(4, fixture.exporter.finishedSpanItems.size)
            assertTrue(fixture.exporter.finishedSpanItems.all { it.traceId != TRACE && it.parentSpanId == ZERO_SPAN })
            assertEquals(ZERO_TRACE, TraceContexts.identifiers().first)
        }
    }

    @Test
    fun `client overwrites foreign metadata propagates only traceparent and scopes async reply`() {
        Fixture().use { fixture ->
            val root = fixture.tracing.browser()
            val channel = ClientProbe()
            var responseTrace = ZERO_TRACE
            root.scoped {
                val client = TraceClientInterceptor(fixture.tracing).interceptCall(METHOD, CallOptions.DEFAULT, channel)
                client.start(
                    object : ClientCall.Listener<String>() {
                        override fun onMessage(message: String) {
                            responseTrace = TraceContexts.identifiers().first
                        }
                    },
                    headers(),
                )
                client.sendMessage("private-payload")
            }
            val outbound = requireNotNull(channel.headers)
            assertEquals(null, outbound.get(Metadata.Key.of("baggage", Metadata.ASCII_STRING_MARSHALLER)))
            assertEquals(null, outbound.get(Metadata.Key.of("tracestate", Metadata.ASCII_STRING_MARSHALLER)))
            val propagated = requireNotNull(outbound.get(PARENT_KEY))
            val executor = Executors.newSingleThreadExecutor()
            try {
                executor
                    .submit { channel.respond() }
                    .get(2, TimeUnit.SECONDS)
                assertEquals(ZERO_TRACE, executor.submit<String> { TraceContexts.identifiers().first }.get())
            } finally {
                executor.shutdownNow()
            }
            root.finish(true)
            val spans = fixture.exporter.finishedSpanItems
            assertEquals(2, spans.size)
            val browser = spans.single { it.name == "browser.ingress" }
            val child = spans.single { it.name == "grpc.client" }
            assertEquals(browser.spanId, child.parentSpanId)
            assertEquals(browser.traceId, child.traceId)
            assertEquals(browser.traceId, responseTrace)
            assertTrue(propagated.contains(child.spanId))
            assertFalse(spans.any { it.attributes.toString().contains("private") })
        }
    }

    @Test
    fun `cancellation and wrapped tasks close scopes without cross request trace`() {
        Fixture().use { fixture ->
            val root = fixture.tracing.browser()
            val task =
                root.scoped {
                    TraceContexts.wrap(Runnable { assertNotEquals(ZERO_TRACE, TraceContexts.identifiers().first) })
                }
            task.run()
            assertEquals(ZERO_TRACE, TraceContexts.identifiers().first)
            val listener =
                TraceServerInterceptor(fixture.tracing) { true }.interceptCall(
                    ServerProbe(),
                    headers(),
                    ServerCallHandler { _, _ -> object : ServerCall.Listener<String>() {} },
                )
            listener.onCancel()
            listener.onComplete()
            root.finish(false)
            root.finish(true)
            assertEquals(2, fixture.exporter.finishedSpanItems.size)
            assertTrue(fixture.exporter.finishedSpanItems.all { it.status.statusCode.name == "ERROR" })
            assertEquals(ZERO_TRACE, TraceContexts.identifiers().first)
        }
    }

    @Test
    fun `failed server callback ends span and encoder reads SDK context without arbitrary MDC`() {
        Fixture().use { fixture ->
            var ids: Pair<String, String>? = null
            val listener =
                TraceServerInterceptor(fixture.tracing) { true }.interceptCall(
                    ServerProbe(),
                    headers(),
                    ServerCallHandler { _, _ ->
                        object : ServerCall.Listener<String>() {
                            override fun onHalfClose() {
                                ids = TraceContexts.identifiers()
                                val bytes =
                                    DiagnosticEnvelope("core", "test-version", "test").encode(
                                        com.spoolin.support.logging.DiagnosticLevel.INFO,
                                        1,
                                        "private-input",
                                    )
                                val json = JsonMapper.builder().build().readTree(bytes)
                                assertEquals(ids?.first, json["trace_id"].asText())
                                assertEquals(ids?.second, json["span_id"].asText())
                                error("private-failure")
                            }
                        }
                    },
                )
            assertThrows(IllegalStateException::class.java) { listener.onHalfClose() }
            listener.onCancel()
            assertEquals(1, fixture.exporter.finishedSpanItems.size)
            assertEquals(
                "ERROR",
                fixture.exporter.finishedSpanItems
                    .single()
                    .status.statusCode.name,
            )
            assertEquals(ZERO_TRACE, TraceContexts.identifiers().first)
        }
    }

    private class ClientProbe : Channel() {
        var headers: Metadata? = null
        private var response: (() -> Unit)? = null

        override fun authority(): String = "private-peer"

        fun respond() {
            requireNotNull(response).invoke()
        }

        override fun <Q : Any, S : Any> newCall(
            method: MethodDescriptor<Q, S>,
            options: CallOptions,
        ): ClientCall<Q, S> =
            object : ClientCall<Q, S>() {
                override fun start(
                    listener: Listener<S>,
                    metadata: Metadata,
                ) {
                    headers = metadata
                    response = {
                        val value =
                            method.responseMarshaller.parse(
                                ByteArrayInputStream("private-response".toByteArray()),
                            )
                        listener.onMessage(value)
                        listener.onClose(Status.OK, Metadata())
                    }
                }

                override fun request(messages: Int) = Unit

                override fun cancel(
                    message: String?,
                    cause: Throwable?,
                ) = Unit

                override fun halfClose() = Unit

                override fun sendMessage(message: Q) = Unit
            }
    }

    private class Fixture : AutoCloseable {
        val exporter = InMemorySpanExporter.create()
        private val provider =
            SdkTracerProvider
                .builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build()
        val tracing = OwnerTracing.testing(provider)

        override fun close() {
            tracing.close()
        }
    }

    private class ServerProbe : ServerCall<String, String>() {
        var closes = 0

        override fun request(messages: Int) = Unit

        override fun sendHeaders(headers: Metadata) = Unit

        override fun sendMessage(message: String) = Unit

        override fun close(
            status: Status,
            trailers: Metadata,
        ) {
            closes++
        }

        override fun isCancelled(): Boolean = false

        override fun getMethodDescriptor(): MethodDescriptor<String, String> = METHOD

        override fun getAttributes(): Attributes = Attributes.EMPTY
    }

    companion object {
        private const val TRACE = "11111111111111111111111111111111"
        private const val PARENT = "2222222222222222"
        private const val ZERO_TRACE = "00000000000000000000000000000000"
        private const val ZERO_SPAN = "0000000000000000"
        private const val VALID = "00-$TRACE-$PARENT-01"
        private val PARENT_KEY = Metadata.Key.of("traceparent", Metadata.ASCII_STRING_MARSHALLER)
        private val marshaller =
            object : MethodDescriptor.Marshaller<String> {
                override fun stream(value: String): InputStream = ByteArrayInputStream(value.toByteArray())

                override fun parse(stream: InputStream): String = stream.readBytes().toString(Charsets.UTF_8)
            }
        private val METHOD =
            MethodDescriptor
                .newBuilder<String, String>()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName("private.Service/Read")
                .setRequestMarshaller(marshaller)
                .setResponseMarshaller(marshaller)
                .build()

        private fun headers(): Metadata =
            Metadata().also {
                it.put(PARENT_KEY, VALID)
                it.put(Metadata.Key.of("baggage", Metadata.ASCII_STRING_MARSHALLER), "private-baggage")
                it.put(Metadata.Key.of("tracestate", Metadata.ASCII_STRING_MARSHALLER), "private=state")
            }
    }
}
