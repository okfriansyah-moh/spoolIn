package com.spoolin.core.boundary.http

import com.spoolin.core.features.session.AnonymousCsrf
import com.spoolin.core.features.session.SessionCredential
import com.spoolin.core.features.session.SessionCsrf
import com.spoolin.core.features.session.SessionStore
import jakarta.servlet.http.HttpServletRequest

class BrowserIdentity(
    private val sessions: SessionStore,
    private val anonymous: AnonymousCsrf,
    private val csrf: SessionCsrf,
) {
    fun context(
        request: HttpServletRequest,
        mutation: Boolean,
        logoutOnly: Boolean,
    ): BrowserContext {
        val cookies = request.cookies.orEmpty().groupBy { it.name }
        if (cookies.values.any { it.size != 1 }) throw BrowserRejected(BrowserBoundary.FORBIDDEN)

        fun cookie(name: String): String? = cookies[name]?.single()?.value
        val credential = SessionCredential.parse(cookie("__Host-spoolin_session"))
        val principal = credential?.let(sessions::find)
        val submission = anonymous.verify(cookie("__Host-spoolin_registration"), cookie("__Host-spoolin_csrf"))
        val supplied = request.getHeaders("X-CSRF").toList().singleOrNull()
        val context = BrowserContext(principal, submission, credential, supplied)
        if (mutation && !permitted(context, logoutOnly)) throw BrowserRejected(BrowserBoundary.FORBIDDEN)
        val current = submission ?: anonymous.issue()
        val result = BrowserContext(principal, current, credential, supplied)
        if (submission == null && !mutation) current.cookies().forEach(result::issue)
        return result
    }

    private fun permitted(
        context: BrowserContext,
        logoutOnly: Boolean,
    ): Boolean {
        val principal = context.principal
        val credential = context.credential
        return when {
            principal != null -> csrf.matches(context.csrf.orEmpty(), csrf.digest(principal.csrfToken))
            logoutOnly && credential != null -> sessions.logoutIdentity(credential, context.csrf.orEmpty()) != null
            else -> anonymousMatches(context)
        }
    }

    private fun anonymousMatches(context: BrowserContext): Boolean {
        val submission = context.anonymous ?: return false
        return csrf.matches(context.csrf.orEmpty(), csrf.digest(submission.csrfToken))
    }
}
