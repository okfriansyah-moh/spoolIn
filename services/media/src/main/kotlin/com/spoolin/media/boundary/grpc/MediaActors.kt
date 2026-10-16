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
        val methods = setOf("Upload", "GetOperation", "GetAsset").map { "spoolin.v1.MediaService/$it" }.toSet()
        return ActorInterceptor("media", methods, verifier, { claims ->
            if (claims.method.endsWith("/Upload") &&
                !com.spoolin.support.identity.Permissions
                    .management(claims.role)
            ) {
                throw com.spoolin.support.identity
                    .ActorRejected()
            }
        }, clock)
    }
}
