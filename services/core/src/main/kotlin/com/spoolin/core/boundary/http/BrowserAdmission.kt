package com.spoolin.core.boundary.http

import com.spoolin.core.boundary.graphql.PreparedGraphql
import com.spoolin.core.features.account.application.AuthenticationRequest
import com.spoolin.core.features.ratelimit.TrustedAdmission
import jakarta.servlet.http.HttpServletRequest

private val publicRoots =
    setOf(
        "session",
        "policy",
        "titles",
        "title",
        "people",
        "person",
        "titleCredits",
        "personCredits",
        "genres",
        "catalogFilterOptions",
        "creditRoles",
    )
private val identityMutations = setOf("register", "signIn", "signOut")

class BrowserAdmission(
    private val admission: TrustedAdmission,
    private val operations: com.spoolin.core.operations.receipts.OperationQueries? = null,
) {
    fun context(
        request: HttpServletRequest,
        parsed: PreparedGraphql,
        identities: BrowserIdentity,
        deadline: Long,
    ): BrowserContext {
        val onlyPublic = !parsed.mutation && parsed.roots.all { it in publicRoots } && "session" !in parsed.roots
        val context =
            if (onlyPublic) {
                BrowserContext(null, null, null, null, AuthenticationRequest(request.remoteAddr, null, deadline))
            } else {
                identities.context(
                    request,
                    parsed.mutation,
                    parsed.roots == listOf("signOut"),
                    "session" in parsed.roots,
                    deadline,
                )
            }
        if (parsed.mutation) {
            if (parsed.roots.any { it !in identityMutations }) admission.mutation(context.request)
        } else {
            reads(parsed, context)
        }
        return context
    }

    private fun reads(
        parsed: PreparedGraphql,
        context: BrowserContext,
    ) {
        admission.reads(
            context.request,
            parsed.roots.any { it in publicRoots },
            parsed.roots.any { it !in publicRoots && it != "operation" },
        )
        if ("operation" in parsed.roots) {
            val queries =
                operations ?: throw com.spoolin.core.features.ratelimit
                    .RateUnavailable()
            queries.prepare(parsed, context, admission)
        }
    }
}
