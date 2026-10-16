package com.spoolin.core.identity

import java.time.Instant
import java.util.UUID

class ActorAuthority internal constructor(
    val account: AccountAuthority,
    val credentialId: UUID,
    val expiresAt: Instant,
) {
    override fun toString(): String = "ActorAuthority(redacted)"
}
