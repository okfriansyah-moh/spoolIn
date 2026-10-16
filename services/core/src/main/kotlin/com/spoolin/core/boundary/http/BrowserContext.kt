package com.spoolin.core.boundary.http

import com.spoolin.core.features.session.AnonymousSubmission
import com.spoolin.core.features.session.SessionCredential
import com.spoolin.core.features.session.SessionPrincipal
import java.util.UUID

class BrowserContext(
    val principal: SessionPrincipal?,
    val anonymous: AnonymousSubmission?,
    val credential: SessionCredential?,
    val csrf: String?,
    val correlation: UUID = UUID.randomUUID(),
) {
    private val issuedCookies = mutableListOf<String>()

    fun issue(cookie: String) {
        issuedCookies.add(cookie)
    }

    fun cookies(): List<String> = issuedCookies.toList()

    override fun toString(): String = "BrowserContext(content=redacted)"
}
