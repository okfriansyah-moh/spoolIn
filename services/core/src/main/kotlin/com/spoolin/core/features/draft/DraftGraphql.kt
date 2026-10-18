package com.spoolin.core.features.draft

import com.spoolin.core.boundary.http.BrowserContext
import com.spoolin.core.features.account.application.CredentialRejected
import graphql.schema.DataFetchingEnvironment
import graphql.schema.idl.RuntimeWiring
import java.util.UUID

/**
 * GraphQL resolvers for draft operations:
 *  - Query.drafts – returns up to 20 eligible decrypted drafts for the authenticated owner.
 *  - Mutation.saveDraft – saves or updates a draft and returns a durable [MutationResult].
 *  - Mutation.discardDraft – deletes a draft and returns [DraftDiscardResult].
 *
 * F-PERMISSIONS: all operations are authenticated-owner only; no secrets or internal state
 * escape the declared projection.
 */
class DraftGraphql(
    private val drafts: ProtectedDrafts,
    private val save: SaveDraft,
    private val discard: DiscardDraft,
) {
    fun bind(wiring: RuntimeWiring.Builder) {
        wiring.type("Query") { q ->
            q.dataFetcher("drafts") { env -> drafts(context(env)) }
        }
        wiring.type("Mutation") { m ->
            m
                .dataFetcher("saveDraft") { env -> saveDraft(env) }
                .dataFetcher("discardDraft") { env -> discardDraft(env) }
        }
    }

    // ── Query ──────────────────────────────────────────────────────────────

    private fun drafts(context: BrowserContext): List<Map<String, Any?>> {
        val principal = context.principal ?: throw CredentialRejected()
        return drafts.list(principal.accountId).map { draftGraphqlProjection(it) }
    }

    // ── Mutation ───────────────────────────────────────────────────────────

    private fun saveDraft(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = context(env)
        val principal = context.principal ?: throw CredentialRejected()
        val operationId = uuid(env, "operationId")
        val draftId = uuid(env, "id")
        val expectedVersion = env.getArgument<Long>("expectedVersion")
        val omittedOrValid =
            env.getField().arguments.none { it.name == "expectedVersion" && it.value != null }
        require(expectedVersion != null || omittedOrValid) {
            "Explicit null for expectedVersion is not permitted; omit the argument for create"
        }
        val rawInput = requireNotNull(env.getArgument<Map<String, Any?>>("input"))
        val input = parseDraftGraphqlInput(rawInput)
        val outcome =
            save.save(
                DraftSaveCommand(
                    operationId = operationId,
                    accountId = principal.accountId,
                    draftId = draftId,
                    expectedVersion = expectedVersion,
                    input = input,
                    correlationId = context.correlation,
                ),
            )
        val projection = saveDraftMutationProjection(outcome)
        context.rememberOperation(
            operationId,
            com.spoolin.support.receipts.OperationOwner.CORE,
            projection,
        )
        return projection
    }

    private fun discardDraft(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = context(env)
        val principal = context.principal ?: throw CredentialRejected()
        val operationId = uuid(env, "operationId")
        val draftId = uuid(env, "id")
        val expectedVersion =
            requireNotNull(env.getArgument<Long>("expectedVersion")) {
                "expectedVersion is required for discardDraft"
            }
        val outcome =
            discard.discard(
                operationId = operationId,
                accountId = principal.accountId,
                draftId = draftId,
                expectedVersion = expectedVersion,
                correlationId = context.correlation,
            )
        val projection = discardDraftMutationProjection(outcome)
        context.rememberOperation(
            operationId,
            com.spoolin.support.receipts.OperationOwner.CORE,
            projection,
        )
        return projection
    }

    private fun context(env: DataFetchingEnvironment): BrowserContext = requireNotNull(env.graphQlContext.get("browser"))

    private fun uuid(
        env: DataFetchingEnvironment,
        name: String,
    ): UUID = UUID.fromString(requireNotNull(env.getArgument<String>(name)))
}
