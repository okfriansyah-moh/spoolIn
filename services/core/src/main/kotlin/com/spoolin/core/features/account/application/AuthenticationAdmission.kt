package com.spoolin.core.features.account.application

enum class AuthenticationAction { REGISTER, SIGN_IN, SIGN_OUT }

class AuthenticationDenied : IllegalStateException("Authentication admission is unavailable")

fun interface AuthenticationAdmission {
    fun check(
        action: AuthenticationAction,
        identity: String,
    )

    companion object {
        val unavailable = AuthenticationAdmission { _, _ -> throw AuthenticationDenied() }
    }
}
