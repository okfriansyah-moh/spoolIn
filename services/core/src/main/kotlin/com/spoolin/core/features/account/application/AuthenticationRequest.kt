package com.spoolin.core.features.account.application

import com.fasterxml.jackson.annotation.JsonIgnoreType
import java.util.UUID

@JsonIgnoreType
class AuthenticationRequest(
    val sourceAddress: String?,
    val credentialId: UUID?,
    private val deadline: Long? = null,
) {
    private val admitted = mutableSetOf<Any>()

    @Synchronized
    fun <T : Any> charge(
        demands: List<T>,
        consume: (List<T>) -> Unit,
    ) {
        requireActive()
        val pending = demands.distinct().filterNot { it in admitted }
        if (pending.isEmpty()) return
        consume(pending)
        admitted.addAll(pending)
    }

    fun requireActive() {
        if (deadline != null && System.nanoTime() - deadline >= 0) throw AuthenticationDeadlineExceeded()
    }

    override fun toString(): String = "AuthenticationRequest(content=redacted)"

    companion object {
        val unavailable = AuthenticationRequest(null, null)
    }
}

class AuthenticationDeadlineExceeded : IllegalStateException("Authentication deadline exceeded")
