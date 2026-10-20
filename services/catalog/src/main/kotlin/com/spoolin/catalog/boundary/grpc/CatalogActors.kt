package com.spoolin.catalog.boundary.grpc

import com.spoolin.support.identity.ActorClaims
import com.spoolin.support.identity.ActorInterceptor
import com.spoolin.support.identity.ActorRejected
import com.spoolin.support.identity.ActorTrust
import com.spoolin.support.identity.ActorVerifier
import com.spoolin.support.identity.Permissions
import org.springframework.core.env.Environment
import org.springframework.grpc.client.GrpcChannelFactory
import spoolin.v1.IdentityServiceGrpc
import spoolin.v1.Spoolin.CheckActorRequest
import java.nio.file.Path
import java.time.Clock
import java.util.concurrent.TimeUnit

class CatalogActors(
    private val identity: IdentityServiceGrpc.IdentityServiceBlockingStub,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun check(claims: ActorClaims) {
        permission(claims)

        val current =
            identity.withDeadlineAfter(1, TimeUnit.SECONDS).checkActor(
                CheckActorRequest
                    .newBuilder()
                    .setAccountId(claims.accountId.toString())
                    .setCredentialId(claims.credentialId.toString())
                    .setAuthEpoch(claims.authEpoch)
                    .setAudience("catalog")
                    .build(),
            )
        val sameIdentity =
            current.accountId == claims.accountId.toString() &&
                current.credentialId == claims.credentialId.toString() && current.authEpoch == claims.authEpoch
        val client = if (current.hasClientKind()) current.clientKind else null
        val sameGrant = current.role == claims.role && client == claims.clientKind
        val live = current.expiresAt.seconds > clock.instant().epochSecond
        if (!sameIdentity || !sameGrant || !live) throw ActorRejected()
    }

    private fun permission(claims: ActorClaims) {
        val receipt = claims.method == "/spoolin.v1.CatalogService/GetOperation"
        if (!receipt && !Permissions.management(claims.role)) throw ActorRejected()
    }

    companion object {
        fun interceptor(
            environment: Environment,
            channels: GrpcChannelFactory,
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
            val live = CatalogActors(IdentityServiceGrpc.newBlockingStub(channels.createChannel("core")))
            val protected =
                ActorInterceptor(
                    "catalog",
                    protectedMethods + "spoolin.v1.CatalogService/ResolveArtworkAccess",
                    verifier,
                    live::check,
                )
            return object : io.grpc.ServerInterceptor {
                override fun <ReqT : Any, RespT : Any> interceptCall(
                    call: io.grpc.ServerCall<ReqT, RespT>,
                    headers: io.grpc.Metadata,
                    next: io.grpc.ServerCallHandler<ReqT, RespT>,
                ): io.grpc.ServerCall.Listener<ReqT> {
                    if (call.methodDescriptor.fullMethodName == "spoolin.v1.CatalogService/ResolveArtworkAccess" &&
                        headers.get(com.spoolin.support.identity.ActorRpc.metadata) == null
                    ) {
                        return next.startCall(call, headers)
                    }
                    return protected.interceptCall(call, headers, next)
                }
            }
        }

        val protectedMethods: Set<String> =
            setOf(
                "CreateRole",
                "ManagedTitles",
                "ManagedPeople",
                "ManagedTitle",
                "ManagedPerson",
                "ManagedCredits",
                "Audit",
                "WriteMovie",
                "WritePerson",
                "WriteCredit",
                "RemoveCredit",
                "MovieLifecycle",
                "PersonLifecycle",
                "DeletionImpact",
                "BeginArtwork",
                "CancelArtwork",
                "GetOperation",
                "ProposeMovie",
                "PrepareMoviePoster",
                "GetMovieProposal",
                "ReadMoviePoster",
                "ApproveAndCreate",
                "RejectProposal",
            ).map { "spoolin.v1.CatalogService/$it" }.toSet()
    }
}
