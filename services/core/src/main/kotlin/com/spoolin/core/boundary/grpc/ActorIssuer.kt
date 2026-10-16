package com.spoolin.core.boundary.grpc

import com.spoolin.core.features.session.SessionPrincipal
import com.spoolin.support.identity.ActorClaims
import com.spoolin.support.identity.ActorSigner
import com.spoolin.support.identity.ActorTrust
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import java.nio.file.Path
import java.security.SecureRandom
import java.time.Clock
import java.util.Base64
import java.util.UUID

private const val ACTOR_LIFETIME = 30L
private const val ACTOR_NONCE_BYTES = 16

data class ActorTarget(
    val audience: String,
    val method: String,
    val operation: UUID?,
    val correlation: UUID,
)

class ActorIssuer(
    private val signer: ActorSigner,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val random = SecureRandom()

    fun issue(
        principal: SessionPrincipal,
        target: ActorTarget,
    ): String {
        require(target.audience in setOf("catalog", "media"))
        val now = clock.instant().epochSecond
        val expires = minOf(now + ACTOR_LIFETIME, principal.expiresAt.epochSecond)
        require(expires > now)
        val nonce = ByteArray(ACTOR_NONCE_BYTES).also(random::nextBytes)
        return signer.sign(
            ActorClaims(
                target.audience,
                target.method,
                principal.accountId,
                principal.id,
                null,
                principal.role.name,
                principal.authEpoch,
                now,
                expires,
                Base64.getUrlEncoder().withoutPadding().encodeToString(nonce),
                target.correlation,
                target.operation,
            ),
        )
    }
}

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = ["spoolin.actor.enabled"], havingValue = "true")
class ActorConfiguration {
    @Bean
    fun actorIssuer(environment: Environment): ActorIssuer =
        ActorIssuer(
            ActorSigner(
                environment.getRequiredProperty("ACTOR_SIGNING_KEY_ID"),
                ActorTrust.signer(Path.of(environment.getRequiredProperty("ACTOR_SIGNING_PRIVATE_KEY_FILE"))),
            ),
        )
}
