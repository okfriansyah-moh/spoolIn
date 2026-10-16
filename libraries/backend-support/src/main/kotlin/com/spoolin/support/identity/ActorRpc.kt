package com.spoolin.support.identity

import io.grpc.Context
import io.grpc.Contexts
import io.grpc.ForwardingServerCallListener
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import java.time.Clock

object ActorRpc {
    val metadata: Metadata.Key<String> = Metadata.Key.of("spoolin-actor", Metadata.ASCII_STRING_MARSHALLER)
    private val actor: Context.Key<ActorClaims> = Context.key("spoolin-verified-actor")

    fun current(): ActorClaims? = actor.get()

    internal fun context(claims: ActorClaims): Context = Context.current().withValue(actor, claims)
}

class ActorInterceptor(
    private val audience: String,
    private val protectedMethods: Set<String>,
    private val verifier: ActorVerifier?,
    private val beforeDispatch: (ActorClaims) -> Unit,
    private val clock: Clock = Clock.systemUTC(),
) : ServerInterceptor {
    override fun <ReqT : Any, RespT : Any> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        val method = call.methodDescriptor.fullMethodName
        if (method !in protectedMethods) return next.startCall(call, headers)
        return protectedCall(call, headers, next)
    }

    private fun <ReqT : Any, RespT : Any> protectedCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        val method = call.methodDescriptor.fullMethodName
        val claims =
            try {
                val token = requireNotNull(headers.getAll(ActorRpc.metadata)?.toList()?.singleOrNull())
                requireNotNull(verifier).verifyTarget(token, audience, "/$method", clock.instant())
            } catch (_: Exception) {
                call.close(Status.PERMISSION_DENIED.withDescription("Actor context is not permitted"), Metadata())
                return object : ServerCall.Listener<ReqT>() {}
            }
        val listener = Contexts.interceptCall(ActorRpc.context(claims), call, headers, next)
        return BoundActorListener(listener, call, claims, beforeDispatch)
    }
}

private class BoundActorListener<ReqT : Any>(
    delegate: ServerCall.Listener<ReqT>,
    private val call: ServerCall<ReqT, *>,
    private val claims: ActorClaims,
    private val check: (ActorClaims) -> Unit,
) : ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(delegate) {
    private var denied = false
    private var checked = false

    override fun onMessage(message: ReqT) {
        if (denied) return
        try {
            ActorMessages.check(message, claims)
            if (!checked) check(claims)
            checked = true
            super.onMessage(message)
        } catch (failure: io.grpc.StatusRuntimeException) {
            val code = failure.status.code
            val status =
                if (code in setOf(Status.Code.UNAVAILABLE, Status.Code.DEADLINE_EXCEEDED)) {
                    Status.UNAVAILABLE
                } else {
                    Status.PERMISSION_DENIED
                }
            deny(status)
        } catch (_: IllegalArgumentException) {
            deny(Status.PERMISSION_DENIED)
        }
    }

    private fun deny(status: Status) {
        denied = true
        call.close(status.withDescription("Actor verification could not complete"), Metadata())
    }

    override fun onHalfClose() {
        if (!denied) super.onHalfClose()
    }
}
