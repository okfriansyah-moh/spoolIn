package com.spoolin.core.features.session

import com.spoolin.core.features.account.persistence.AccountRole
import java.time.Instant
import java.util.UUID

class SessionPrincipal(
    val id: UUID,
    val accountId: UUID,
    val role: AccountRole,
    val expiresAt: Instant,
    val csrfToken: String,
)

class IssuedSession(
    val principal: SessionPrincipal,
    internal val credential: SessionCredential,
) {
    fun cookie(): String = credential.cookie()

    override fun toString(): String = "IssuedSession(content=redacted)"
}
