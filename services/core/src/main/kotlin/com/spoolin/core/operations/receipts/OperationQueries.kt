package com.spoolin.core.operations.receipts

import com.spoolin.core.boundary.graphql.PreparedGraphql
import com.spoolin.core.boundary.grpc.ActorIssuer
import com.spoolin.core.boundary.grpc.ActorTarget
import com.spoolin.core.boundary.http.BrowserContext
import com.spoolin.core.features.ratelimit.RateUnavailable
import com.spoolin.core.features.ratelimit.TrustedAdmission
import com.spoolin.core.features.session.SessionStore
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.receipts.OperationIntent
import com.spoolin.support.receipts.OperationOwner
import com.spoolin.support.receipts.ReceiptAuthority
import com.spoolin.support.receipts.ReceiptConflict
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.MediaServiceGrpc
import java.util.UUID

class OperationQueries(
    private val reads: OwnerReceiptReads,
    private val receipts: OwnerReceipts,
    private val sessions: SessionStore?,
    private val issuer: ActorIssuer?,
    private val catalog: CatalogServiceGrpc.CatalogServiceBlockingStub,
    private val media: MediaServiceGrpc.MediaServiceBlockingStub,
) {
    fun prepare(
        parsed: PreparedGraphql,
        context: BrowserContext,
        admission: TrustedAdmission,
    ) {
        val selected = operationSelections(parsed)
        if (selected.isEmpty()) return
        if (context.principal == null) admission.reads(context.request, true, false)
        val anonymous = anonymous(selected, context, admission)
        if (selected.any { it !in anonymous } && context.principal != null) {
            admission.reads(context.request, false, true)
        }
        remember(selected, anonymous, context)
    }

    private fun anonymous(
        selected: List<OperationSelection>,
        context: BrowserContext,
        admission: TrustedAdmission,
    ): Set<OperationSelection> {
        val scope = context.anonymous?.scope ?: return emptySet()
        val potential = selected.any(::core)
        if (potential) admission.reads(context.request, true, false)
        return selected.filter { core(it) && reads.anonymous(it.id, scope) }.toSet()
    }

    private fun remember(
        selected: List<OperationSelection>,
        anonymous: Set<OperationSelection>,
        context: BrowserContext,
    ) {
        selected.forEach { candidate ->
            context.request.requireActive()
            val value =
                if (candidate in anonymous) {
                    local(candidate.id, requireNotNull(context.anonymous).scope)
                } else {
                    lookup(candidate, context)
                }
            context.rememberOperation(candidate.id, candidate.owner, value)
        }
    }

    private fun lookup(
        selected: OperationSelection,
        context: BrowserContext,
    ): Map<String, Any?>? {
        val principal = context.principal ?: return logout(selected, context)
        return if (selected.owner == OperationOwner.CORE) {
            local(selected.id, "account:${principal.accountId}")
        } else {
            remote(selected, context, principal)
        }
    }

    private fun remote(
        selected: OperationSelection,
        context: BrowserContext,
        principal: com.spoolin.core.features.session.SessionPrincipal,
    ): Map<String, Any?>? {
        val actor = "account:${principal.accountId}"
        val signer = issuer ?: throw RateUnavailable()
        val catalogToken =
            signer.issue(
                principal,
                ActorTarget(
                    "catalog",
                    "/spoolin.v1.CatalogService/GetOperation",
                    selected.id,
                    context.correlation,
                ),
            )
        val mediaToken =
            signer.issue(
                principal,
                ActorTarget(
                    "media",
                    "/spoolin.v1.MediaService/GetOperation",
                    selected.id,
                    context.correlation,
                ),
            )
        val router =
            OperationRouter(
                ReceiptAuthority { actor },
                reads::find,
                catalog.withInterceptors(metadata(catalogToken)),
                media.withInterceptors(metadata(mediaToken)),
            )
        return router.find(selected.id, selected.owner)
    }

    private fun logout(
        selected: OperationSelection,
        context: BrowserContext,
    ): Map<String, Any?>? {
        val retained = if (core(selected)) logoutTarget(context) else null
        if (retained == null) return null
        val scope = "account:${retained.accountId}"
        val intent =
            OperationIntent(
                selected.id,
                scope,
                "SignOut",
                "signout-v1",
                listOf("session" to retained.sessionId.toString()),
            )
        val result =
            try {
                receipts.resolve(intent)
            } catch (
                _: ReceiptConflict,
            ) {
                null
            } catch (_: com.spoolin.support.receipts.ReceiptHidden) {
                null
            }
        return OperationRouter(ReceiptAuthority { scope }, { _, _ -> result }, catalog, media)
            .find(selected.id, OperationOwner.CORE)
    }

    private fun core(selected: OperationSelection): Boolean = selected.owner in setOf(null, OperationOwner.CORE)

    private fun logoutTarget(context: BrowserContext): com.spoolin.core.features.session.LogoutIdentity? =
        context.credential?.let { sessions?.logoutIdentity(it, context.csrf.orEmpty()) }

    private fun local(
        id: UUID,
        scope: String,
    ): Map<String, Any?>? {
        val router = OperationRouter(ReceiptAuthority { scope }, reads::find, catalog, media)
        return router.find(id, OperationOwner.CORE)
    }

    private fun metadata(token: String): io.grpc.ClientInterceptor {
        val metadata = Metadata().apply { put(ActorRpc.metadata, token) }
        return MetadataUtils.newAttachHeadersInterceptor(metadata)
    }
}
