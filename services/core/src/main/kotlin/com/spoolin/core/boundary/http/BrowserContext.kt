package com.spoolin.core.boundary.http

import com.spoolin.core.features.account.application.AuthenticationRequest
import com.spoolin.core.features.session.AnonymousSubmission
import com.spoolin.core.features.session.SessionCredential
import com.spoolin.core.features.session.SessionPrincipal
import com.spoolin.support.receipts.OperationOwner
import java.util.UUID

class BrowserContext(
    val principal: SessionPrincipal?,
    val anonymous: AnonymousSubmission?,
    val credential: SessionCredential?,
    val csrf: String?,
    val request: AuthenticationRequest = AuthenticationRequest.unavailable,
) {
    val correlation: UUID = UUID.randomUUID()
    private val operations = mutableMapOf<Pair<UUID, OperationOwner?>, Map<String, Any?>?>()

    fun rememberOperation(
        id: UUID,
        owner: OperationOwner?,
        value: Map<String, Any?>?,
    ) {
        operations[id to owner] = value
    }

    fun operation(
        id: UUID,
        owner: OperationOwner?,
    ): Map<String, Any?>? = operations[id to owner]

    private val issuedCookies = mutableListOf<String>()

    fun issue(cookie: String) {
        issuedCookies.add(cookie)
    }

    fun cookies(): List<String> = issuedCookies.toList()

    override fun toString(): String = "BrowserContext(content=redacted)"
}
