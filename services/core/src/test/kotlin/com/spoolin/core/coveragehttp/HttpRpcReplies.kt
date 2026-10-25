package com.spoolin.core.coveragehttp

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.Status

/** External RPC transport double; production generated stubs and request builders remain real. */
internal class HttpRpcReplies(
    vararg replies: Any,
) : Channel() {
    private val replies = ArrayDeque(replies.toList())
    val requests = mutableListOf<Any?>()

    override fun authority(): String = "unit-downstream"

    override fun <ReqT : Any, RespT : Any> newCall(
        method: MethodDescriptor<ReqT, RespT>,
        options: CallOptions,
    ): ClientCall<ReqT, RespT> =
        object : ClientCall<ReqT, RespT>() {
            lateinit var listener: Listener<RespT>

            override fun start(
                listener: Listener<RespT>,
                headers: Metadata,
            ) {
                this.listener = listener
            }

            override fun request(count: Int) = Unit

            override fun cancel(
                message: String?,
                cause: Throwable?,
            ) = Unit

            override fun sendMessage(message: ReqT) {
                requests.add(message)
            }

            override fun halfClose() {
                val result = replies.removeFirstOrNull() ?: error("No scripted downstream response for ${method.fullMethodName}")
                if (result is Status) {
                    listener.onClose(result, Metadata())
                } else {
                    @Suppress("UNCHECKED_CAST")
                    if (result is List<*>) {
                        result.forEach { listener.onMessage(it as RespT) }
                    } else {
                        listener.onMessage(result as RespT)
                    }
                    listener.onClose(Status.OK, Metadata())
                }
            }
        }
}
