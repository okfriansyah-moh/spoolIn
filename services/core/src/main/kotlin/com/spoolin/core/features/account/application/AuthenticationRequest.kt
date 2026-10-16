package com.spoolin.core.features.account.application

import com.fasterxml.jackson.annotation.JsonIgnoreType
import java.util.UUID

@JsonIgnoreType
class AuthenticationRequest(
    val sourceAddress: String?,
    val credentialId: UUID?,
    private val deadline: Long? = null,
) {
    fun requireActive() {
        if (deadline != null && System.nanoTime() - deadline >= 0) throw AuthenticationDeadlineExceeded()
    }

    override fun toString(): String = "AuthenticationRequest(content=redacted)"

    companion object {
        val unavailable = AuthenticationRequest(null, null)
    }
}

class AuthenticationDeadlineExceeded : IllegalStateException("Authentication deadline exceeded")
