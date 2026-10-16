package com.spoolin.core.features.session

import com.spoolin.core.features.account.persistence.AccountRole
import java.time.Instant
import java.util.UUID

class SessionAccount(
    val id: UUID,
    val role: AccountRole,
    val authEpoch: Long,
)

class SessionPrincipal(
    val id: UUID,
    private val account: SessionAccount,
    val expiresAt: Instant,
    val csrfToken: String,
) {
    val accountId: UUID get() = account.id
    val role: AccountRole get() = account.role
    val authEpoch: Long get() = account.authEpoch
}

class IssuedSession(
    val principal: SessionPrincipal,
    internal val credential: SessionCredential,
) {
    fun cookie(): String = credential.cookie()

    override fun toString(): String = "IssuedSession(content=redacted)"
}
