package com.spoolin.support.observability

import io.grpc.ForwardingServerCall
import io.grpc.ForwardingServerCallListener
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import java.util.concurrent.atomic.AtomicBoolean

/** Measures only source-selected handlers, not identity checks, unknown services or arbitrary method labels. */
class LogicalRpcMetricsInterceptor(
    private val metrics: LogicalMetrics,
) : ServerInterceptor {
    override fun <ReqT : Any, RespT : Any> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        val operation =
            MetricOperations.rpc(metrics.service, call.methodDescriptor.fullMethodName)
                ?: return next.startCall(call, headers)
        val observed = Observation(operation, metrics)
        val wrapped =
            object : ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
                override fun sendMessage(message: RespT) {
                    super.sendMessage(message)
                    observed.message(message)
                }

                override fun close(
                    status: Status,
                    trailers: Metadata,
                ) {
                    var returned = false
                    try {
                        super.close(status, trailers)
                        returned = true
                    } finally {
                        observed.finish(if (returned) status else Status.UNKNOWN)
                    }
                }
            }
        var started = false
        try {
            val listener = next.startCall(wrapped, headers)
            started = true
            return object : ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(listener) {
                override fun onCancel() {
                    try {
                        super.onCancel()
                    } finally {
                        observed.finish(Status.CANCELLED)
                    }
                }

                override fun onComplete() {
                    try {
                        super.onComplete()
                    } finally {
                        observed.finish(Status.UNKNOWN)
                    }
                }
            }
        } finally {
            if (!started) observed.finish(Status.UNKNOWN)
        }
    }

    private class Observation(
        private val operation: MetricOperation,
        private val metrics: LogicalMetrics,
    ) {
        private val started = System.nanoTime()
        private val completed = AtomicBoolean()
        private var response: LogicalResult? = null

        @Synchronized
        fun message(value: Any) {
            val next = LogicalResult.message(operation, value)
            if (response == null || next.outcome.ordinal > requireNotNull(response).outcome.ordinal) response = next
        }

        @Synchronized
        fun finish(status: Status) {
            if (!completed.compareAndSet(false, true)) return
            val result =
                if (status.isOk) {
                    response ?: LogicalResult.unknown()
                } else {
                    LogicalResult.fromStatus(status, operation.mutation)
                }
            metrics.record(operation, result, started)
        }
    }
}
