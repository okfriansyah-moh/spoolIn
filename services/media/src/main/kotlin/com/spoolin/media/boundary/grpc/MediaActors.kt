package com.spoolin.media.boundary.grpc

import com.spoolin.support.identity.ActorInterceptor
import com.spoolin.support.identity.ActorTrust
import com.spoolin.support.identity.ActorVerifier
import org.springframework.core.env.Environment
import java.nio.file.Path

object MediaActors {
    fun interceptor(environment: Environment): ActorInterceptor {
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
        return ActorInterceptor("media", methods, verifier, {})
    }
}
