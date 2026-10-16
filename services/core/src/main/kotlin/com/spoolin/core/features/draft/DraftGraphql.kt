package com.spoolin.core.features.draft

import com.spoolin.core.boundary.http.BrowserContext
import com.spoolin.core.features.account.application.CredentialRejected
import com.spoolin.support.receipts.ReceiptOutcome
import com.spoolin.support.receipts.ReceiptState
import graphql.schema.DataFetchingEnvironment
import graphql.schema.idl.RuntimeWiring
import tools.jackson.databind.ObjectMapper
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
        return drafts.list(principal.accountId).map { draftProjection(it) }
    }

    // ── Mutation ───────────────────────────────────────────────────────────

    private fun saveDraft(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = context(env)
        val principal = context.principal ?: throw CredentialRejected()
        val operationId = uuid(env, "operationId")
        val draftId = uuid(env, "id")
        val expectedVersion = env.getArgument<Long>("expectedVersion")
        require(expectedVersion != null || env.getField().arguments.none { it.name == "expectedVersion" && it.value != null }) {
            "Explicit null for expectedVersion is not permitted; omit the argument for create"
        }
        val rawInput = requireNotNull(env.getArgument<Map<String, Any?>>("input"))
        val input = parseInput(rawInput)
        val outcome =
            save.save(
                operationId = operationId,
                accountId = principal.accountId,
                draftId = draftId,
                expectedVersion = expectedVersion,
                input = input,
                correlationId = context.correlation,
            )
        context.rememberOperation(operationId, com.spoolin.support.receipts.OperationOwner.CORE, saveMutationProjection(outcome))
        return saveMutationProjection(outcome)
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
        context.rememberOperation(operationId, com.spoolin.support.receipts.OperationOwner.CORE, discardMutationProjection(outcome))
        return discardMutationProjection(outcome)
    }

    // ── projections ────────────────────────────────────────────────────────

    private fun draftProjection(draft: DraftProjection): Map<String, Any?> =
        mapOf(
            "id" to draft.id.toString(),
            "targetKind" to draft.targetKind,
            "targetId" to draft.targetId?.toString(),
            "baseVersion" to draft.baseVersion?.toString(),
            "values" to draft.values,
            "operationId" to draft.operationId?.toString(),
            "version" to draft.version.toString(),
            "expiresAt" to draft.expiresAt.toString(),
        )

    private fun saveMutationProjection(outcome: ReceiptOutcome): Map<String, Any?> =
        mapOf(
            "operation" to
                mapOf(
                    "id" to outcome.id.toString(),
                    "state" to outcome.state.name,
                    "resultId" to outcome.result?.resultId?.toString(),
                    "version" to outcome.result?.version?.toString(),
                    "resultExpired" to outcome.resultExpired,
                    "problem" to null,
                ),
            "problem" to problemProjection(outcome),
        )

    private fun discardMutationProjection(outcome: ReceiptOutcome): Map<String, Any?> =
        mapOf(
            "operation" to
                mapOf(
                    "id" to outcome.id.toString(),
                    "state" to outcome.state.name,
                    "resultId" to outcome.result?.resultId?.toString(),
                    "resultExpired" to outcome.resultExpired,
                    "problem" to null,
                ),
            "discarded" to (outcome.result?.discarded ?: false),
            "problem" to problemProjection(outcome),
        )

    private fun problemProjection(outcome: ReceiptOutcome): Map<String, Any?>? {
        val code = outcome.errorCode ?: return null
        return mapOf(
            "code" to code,
            "message" to "The draft operation could not be completed.",
            "fields" to emptyList<Any>(),
            "retryAfterMs" to null,
        )
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private fun parseInput(raw: Map<String, Any?>): DraftInput =
        DraftInput(
            targetKind = requireNotNull(raw["targetKind"] as? String) { "targetKind is required" },
            targetId = (raw["targetId"] as? String)?.let { UUID.fromString(it) },
            baseVersion = raw["baseVersion"] as? Long,
            values =
                requireNotNull(
                    when (val v = raw["values"]) {
                        is String -> v
        is Map<*, *> -> ObjectMapper().writeValueAsString(v)
                        else -> null
                    },
                ) { "values is required" },
            operationId = (raw["operationId"] as? String)?.let { UUID.fromString(it) },
            policyRevision = requireNotNull(raw["policyRevision"] as? String) { "policyRevision is required" },
        )

    private fun context(env: DataFetchingEnvironment): BrowserContext =
        requireNotNull(env.graphQlContext.get("browser"))

    private fun uuid(
        env: DataFetchingEnvironment,
        name: String,
    ): UUID = UUID.fromString(requireNotNull(env.getArgument<String>(name)))
}
