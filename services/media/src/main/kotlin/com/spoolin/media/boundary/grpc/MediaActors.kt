package com.spoolin.media.boundary.grpc

import com.spoolin.support.identity.ActorInterceptor
import com.spoolin.support.identity.ActorTrust
import com.spoolin.support.identity.ActorVerifier
import org.springframework.core.env.Environment
import java.nio.file.Path
import java.time.Clock

object MediaActors {
    fun interceptor(
        environment: Environment,
        clock: Clock = Clock.systemUTC(),
    ): io.grpc.ServerInterceptor {
        val enabled = environment.getProperty("spoolin.actor.enabled", Boolean::class.java, false)
        val verifier =
            if (enabled) {
                ActorVerifier(
                    ActorTrust.verifiers(
                        Path.of(environment.getRequiredProperty("ACTOR_VERIFY_KEYS_FILE")),
                    ),
                )
            } else {
                null
            }
        val methods = setOf("Upload", "GetOperation", "GetAsset").map { "spoolin.v1.MediaService/$it" }.toSet()
        val actorCheck = ActorInterceptor("media", methods, verifier, { claims ->
            if (claims.method.endsWith("/Upload") &&
                !com.spoolin.support.identity.Permissions
                    .management(claims.role)
            ) {
                throw com.spoolin.support.identity
                    .ActorRejected()
            }
        }, clock)
        return object : io.grpc.ServerInterceptor {
            override fun <ReqT : Any, RespT : Any> interceptCall(
                call: io.grpc.ServerCall<ReqT, RespT>,
                headers: io.grpc.Metadata,
                next: io.grpc.ServerCallHandler<ReqT, RespT>,
            ): io.grpc.ServerCall.Listener<ReqT> {
                val reconciliation = call.methodDescriptor.fullMethodName in setOf(
                    "spoolin.v1.MediaService/GetAsset", "spoolin.v1.MediaService/GetOperation",
                )
                // Business handlers additionally require the persisted local/Catalog tuple.
                return if (MediaPeer.current() == "catalog" && reconciliation) {
                    next.startCall(call, headers)
                } else {
                    actorCheck.interceptCall(call, headers, next)
                }
            }
        }
    }
}
