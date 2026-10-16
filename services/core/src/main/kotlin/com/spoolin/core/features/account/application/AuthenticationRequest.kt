package com.spoolin.core.features.account.application

import com.fasterxml.jackson.annotation.JsonIgnoreType
import java.util.UUID

@JsonIgnoreType
class AuthenticationRequest(
    val sourceAddress: String?,
    val credentialId: UUID?,
) {
    override fun toString(): String = "AuthenticationRequest(content=redacted)"

    companion object {
        val unavailable = AuthenticationRequest(null, null)
    }
}
