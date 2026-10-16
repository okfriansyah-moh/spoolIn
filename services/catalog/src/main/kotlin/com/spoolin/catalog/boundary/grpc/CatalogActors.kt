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
        val method = claims.method.substringAfterLast('/')
        val connector = method in setOf("ProposeMovie", "ApproveAndCreate", "RejectProposal")
        if (connector && claims.clientKind == null) throw ActorRejected()
    }

    companion object {
        fun interceptor(
            environment: Environment,
            channels: GrpcChannelFactory,
        ): ActorInterceptor {
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
            return ActorInterceptor("catalog", protectedMethods, verifier, live::check)
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
                "ApproveAndCreate",
                "RejectProposal",
            ).map { "spoolin.v1.CatalogService/$it" }.toSet()
    }
}
