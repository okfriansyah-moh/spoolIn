package com.spoolin.core.features.catalogview.movie

import com.spoolin.core.boundary.grpc.ActorIssuer
import com.spoolin.core.boundary.grpc.ActorTarget
import com.spoolin.core.boundary.http.BrowserContext
import com.spoolin.core.features.account.application.CredentialRejected
import com.spoolin.core.features.ratelimit.RateUnavailable
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.policy.InputViolation
import com.spoolin.support.receipts.ReceiptState
import graphql.schema.DataFetchingEnvironment
import graphql.schema.idl.RuntimeWiring
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.Spoolin
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val DEADLINE_SECONDS = 5L
private const val DEFAULT_FIRST = 24
private const val MAX_PAGE = 100

/**
 * GraphQL resolvers for Maintainer-only catalog mutations and managed reads.
 *
 * Mutations: createMovie, updateMovie.
 * Queries:   managedTitle, managedTitles, audit.
 *
 * F-PERMISSIONS: each operation requires an authenticated MAINTAINER session.
 * AC-127: actorRef is the account UUID only; no email or credential data exposed.
 * ATDD-251-T1-R23/R24: createMovie / updateMovie at their authoritative boundary.
 */
class ManagedCatalogGraphql(
    private val catalog: CatalogServiceGrpc.CatalogServiceBlockingStub,
    private val issuer: ActorIssuer?,
) {

    fun bind(wiring: RuntimeWiring.Builder) {
        wiring.type("Query") { q ->
            q.dataFetcher("managedTitle") { env -> managedTitle(env) }
             .dataFetcher("managedTitles") { env -> managedTitles(env) }
             .dataFetcher("audit") { env -> audit(env) }
        }
        wiring.type("Mutation") { m ->
            m.dataFetcher("createMovie") { env -> createMovie(env) }
             .dataFetcher("updateMovie") { env -> updateMovie(env) }
        }
    }

    // ── createMovie ───────────────────────────────────────────────────────────

    private fun createMovie(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = context(env)
        val principal = context.principal ?: throw CredentialRejected()
        val operationId = id(env)
        val policyRevision = requireNotNull(env.getArgument<String>("policyRevision"))
        val input = requireNotNull(env.getArgument<Map<String, Any?>>("input"))
        val values = movieValues(input)
        val request = Spoolin.MovieWrite.newBuilder()
            .setContext(grpcContext(operationId, context.correlation, policyRevision))
            .setValues(values)
            .build()
        val token = issueToken(principal, "/spoolin.v1.CatalogService/WriteMovie", operationId, context.correlation)
        val outcome = stub(token).writeMovie(request)
        val result = outcomeMap(outcome)
        context.rememberOperation(operationId, com.spoolin.support.receipts.OperationOwner.CATALOG, result)
        return mutationResult(outcome)
    }

    // ── updateMovie ───────────────────────────────────────────────────────────

    private fun updateMovie(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = context(env)
        val principal = context.principal ?: throw CredentialRejected()
        val operationId = id(env)
        val titleId = requireNotNull(env.getArgument<String>("id"))
        validateUuid(titleId)
        val expectedVersion = requireNotNull(env.getArgument<Long>("expectedVersion"))
        val policyRevision = requireNotNull(env.getArgument<String>("policyRevision"))
        val patch = requireNotNull(env.getArgument<Map<String, Any?>>("patch"))
        val (values, mask) = moviePatch(patch)
        val request = Spoolin.MovieWrite.newBuilder()
            .setContext(grpcContext(operationId, context.correlation, policyRevision))
            .setId(titleId)
            .setExpectedVersion(expectedVersion)
            .setValues(values)
            .setUpdateMask(mask)
            .build()
        val token = issueToken(principal, "/spoolin.v1.CatalogService/WriteMovie", operationId, context.correlation)
        val outcome = stub(token).writeMovie(request)
        val result = outcomeMap(outcome)
        context.rememberOperation(operationId, com.spoolin.support.receipts.OperationOwner.CATALOG, result)
        return mutationResult(outcome)
    }

    // ── managedTitle ─────────────────────────────────────────────────────────

    private fun managedTitle(env: DataFetchingEnvironment): Map<String, Any?>? {
        val context = context(env)
        val principal = context.principal ?: throw CredentialRejected()
        val titleId = requireNotNull(env.getArgument<String>("id"))
        validateUuid(titleId)
        val opId = UUID.randomUUID()
        val token = issueToken(principal, "/spoolin.v1.CatalogService/ManagedTitle", opId, context.correlation)
        val result = stub(token).managedTitle(Spoolin.Id.newBuilder().setId(titleId).build())
        if (result.id.isEmpty()) return null
        return titleMap(result)
    }

    // ── managedTitles ─────────────────────────────────────────────────────────

    private fun managedTitles(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = context(env)
        val principal = context.principal ?: throw CredentialRejected()
        val state = requireNotNull(env.getArgument<String>("state"))
        val first = (env.getArgument<Int>("first") ?: DEFAULT_FIRST).coerceIn(1, MAX_PAGE)
        val after = env.getArgument<String>("after")
        val opId = UUID.randomUUID()
        val token = issueToken(principal, "/spoolin.v1.CatalogService/ManagedTitles", opId, context.correlation)
        val request = Spoolin.PageRequest.newBuilder().setState(state).setFirst(first)
            .also { b -> after?.let { b.after = it } }.build()
        val result = stub(token).managedTitles(request)
        return titlePageMap(result)
    }

    // ── audit ─────────────────────────────────────────────────────────────────

    private fun audit(env: DataFetchingEnvironment): Map<String, Any?> {
        val context = context(env)
        val principal = context.principal ?: throw CredentialRejected()
        val subjectId = env.getArgument<String>("subjectId")?.also { validateUuid(it) }
        val first = (env.getArgument<Int>("first") ?: DEFAULT_FIRST).coerceIn(1, MAX_PAGE)
        val after = env.getArgument<String>("after")
        val opId = UUID.randomUUID()
        val token = issueToken(principal, "/spoolin.v1.CatalogService/Audit", opId, context.correlation)
        val request = Spoolin.PageRequest.newBuilder().setFirst(first)
            .also { b ->
                subjectId?.let { b.targetId = it }
                after?.let { b.after = it }
            }.build()
        val result = stub(token).audit(request)
        return auditPageMap(result)
    }

    // ── proto → GraphQL maps ──────────────────────────────────────────────────

    private fun titlePageMap(proto: Spoolin.TitlePage): Map<String, Any?> = mapOf(
        "nodes" to proto.nodesList.map { titleMap(it) },
        "pageInfo" to mapOf(
            "endCursor" to proto.endCursor.takeIf { it.isNotEmpty() },
            "hasNextPage" to proto.hasNextPage,
        ),
    )

    private fun titleMap(t: Spoolin.Title): Map<String, Any?> = mapOf(
        "id" to t.id, "kind" to t.kind, "state" to t.state,
        "title" to t.values.title,
        "originalTitle" to t.values.originalTitle.takeIf { it.isNotEmpty() },
        "synopsis" to t.values.synopsis,
        "synopsisLanguage" to t.values.synopsisLanguage,
        "originalLanguage" to t.values.originalLanguage.takeIf { it.isNotEmpty() },
        "countries" to t.values.countriesList,
        "genres" to t.values.genreIdsList.map { mapOf("id" to it) },
        "release" to partialDateMap(t.values.release),
        "runtimeMinutes" to t.values.runtimeMinutes.takeIf { it > 0 },
        "tvStatus" to t.tvStatus.takeIf { it.isNotEmpty() },
        "tvEnd" to partialDateMap(t.tvEnd),
        "releaseContext" to t.releaseContext.takeIf { it.isNotEmpty() },
        "provenance" to t.provenanceList.map { provenanceMap(it) },
        "artwork" to null,
        "version" to t.version.toString(),
    )

    private fun partialDateMap(pd: Spoolin.PartialDate): Map<String, Any?> = mapOf(
        "year" to pd.year.takeIf { pd.hasYear() },
        "month" to pd.month.takeIf { pd.hasMonth() },
        "day" to pd.day.takeIf { pd.hasDay() },
        "precision" to when { pd.hasDay() -> "DAY"; pd.hasMonth() -> "MONTH"; pd.hasYear() -> "YEAR"; else -> "UNKNOWN" },
    )

    private fun provenanceMap(p: Spoolin.Provenance): Map<String, Any?> = mapOf(
        "source" to p.source, "externalId" to p.externalId,
        "recordUrl" to p.recordUrl.takeIf { it.isNotEmpty() },
        "retrievedAt" to Instant.ofEpochSecond(p.retrievedAt.seconds, p.retrievedAt.nanos.toLong()).toString(),
        "reuseNote" to p.reuseNote, "fieldSources" to emptyMap<String, Any?>(),
        "locallyChanged" to p.locallyChangedList,
    )

    private fun auditPageMap(proto: Spoolin.AuditPage): Map<String, Any?> = mapOf(
        "nodes" to proto.nodesList.map { auditEventMap(it) },
        "pageInfo" to mapOf(
            "endCursor" to proto.endCursor.takeIf { it.isNotEmpty() },
            "hasNextPage" to proto.hasNextPage,
        ),
    )

    private fun auditEventMap(e: Spoolin.AuditEvent): Map<String, Any?> = mapOf(
        "id" to e.id, "actorRef" to e.actorRef, "action" to e.action, "outcome" to e.outcome,
        "subjectKind" to e.subjectKind,
        "subjectId" to e.subjectId.takeIf { e.hasSubjectId() }?.let { UUID.fromString(it) },
        "changes" to e.changesList.associate { it.path to mapOf("before" to it.beforeJson, "after" to it.afterJson) },
        "operationId" to e.operationId.takeIf { e.hasOperationId() }?.let { UUID.fromString(it) },
        "at" to Instant.ofEpochSecond(e.at.seconds, e.at.nanos.toLong()).toString(),
    )

    private fun mutationResult(outcome: Spoolin.Outcome): Map<String, Any?> = mapOf(
        "operation" to outcomeMap(outcome),
        "problem" to if (outcome.hasProblem()) problemMap(outcome.problem) else null,
    )

    private fun outcomeMap(outcome: Spoolin.Outcome): Map<String, Any?> = mapOf(
        "id" to outcome.operationId,
        "state" to outcome.state,
        "resultId" to if (outcome.hasResultId()) outcome.resultId else null,
        "version" to if (outcome.hasVersion()) outcome.version.toString() else null,
        "resultExpired" to outcome.resultExpired,
        "problem" to if (outcome.hasProblem()) problemMap(outcome.problem) else null,
    )

    private fun problemMap(p: Spoolin.Problem): Map<String, Any?> = mapOf(
        "code" to p.code,
        "message" to "The operation could not be completed.",
        "fields" to p.fieldsList.map { mapOf("path" to it.path, "code" to it.code, "message" to it.message) },
    )

    // ── input mappers ─────────────────────────────────────────────────────────

    private fun movieValues(input: Map<String, Any?>): Spoolin.MovieValues {
        val builder = Spoolin.MovieValues.newBuilder()
        (input["title"] as? String)?.let { builder.title = it }
        (input["originalTitle"] as? String)?.let { builder.originalTitle = it }
        (input["synopsis"] as? String)?.let { builder.synopsis = it }
        (input["synopsisLanguage"] as? String)?.let { builder.synopsisLanguage = it }
        (input["originalLanguage"] as? String)?.let { builder.originalLanguage = it }
        @Suppress("UNCHECKED_CAST")
        (input["countries"] as? List<String>)?.let { builder.addAllCountries(it) }
        @Suppress("UNCHECKED_CAST")
        (input["genreIds"] as? List<String>)?.let { builder.addAllGenreIds(it) }
        (input["release"] as? Map<*, *>)?.let { builder.setRelease(partialDateInput(it)) }
        (input["runtimeMinutes"] as? Int)?.let { builder.runtimeMinutes = it }
        return builder.build()
    }

    private fun moviePatch(patch: Map<String, Any?>): Pair<Spoolin.MovieValues, com.google.protobuf.FieldMask> {
        val paths = mutableListOf<String>()
        val builder = Spoolin.MovieValues.newBuilder()
        if (patch.containsKey("title")) {
            (patch["title"] as? String)?.let { builder.title = it }
            paths.add("values.title")
        }
        if (patch.containsKey("originalTitle")) {
            (patch["originalTitle"] as? String)?.let { builder.originalTitle = it }
            paths.add("values.original_title")
        }
        if (patch.containsKey("synopsis")) {
            (patch["synopsis"] as? String)?.let { builder.synopsis = it }
            paths.add("values.synopsis")
        }
        if (patch.containsKey("synopsisLanguage")) {
            (patch["synopsisLanguage"] as? String)?.let { builder.synopsisLanguage = it }
            paths.add("values.synopsis_language")
        }
        if (patch.containsKey("originalLanguage")) {
            (patch["originalLanguage"] as? String)?.let { builder.originalLanguage = it }
            paths.add("values.original_language")
        }
        if (patch.containsKey("countries")) {
            @Suppress("UNCHECKED_CAST")
            (patch["countries"] as? List<String>)?.let { builder.addAllCountries(it) }
            paths.add("values.countries")
        }
        if (patch.containsKey("genreIds")) {
            @Suppress("UNCHECKED_CAST")
            (patch["genreIds"] as? List<String>)?.let { builder.addAllGenreIds(it) }
            paths.add("values.genre_ids")
        }
        if (patch.containsKey("release")) {
            (patch["release"] as? Map<*, *>)?.let { builder.setRelease(partialDateInput(it)) }
            paths.add("values.release")
        }
        if (patch.containsKey("runtimeMinutes")) {
            (patch["runtimeMinutes"] as? Int)?.let { builder.runtimeMinutes = it }
            paths.add("values.runtime_minutes")
        }
        val mask = com.google.protobuf.FieldMask.newBuilder().addAllPaths(paths).build()
        return Pair(builder.build(), mask)
    }

    private fun partialDateInput(map: Map<*, *>): Spoolin.PartialDate {
        val b = Spoolin.PartialDate.newBuilder()
        (map["year"] as? Int)?.let { b.year = it }
        (map["month"] as? Int)?.let { b.month = it }
        (map["day"] as? Int)?.let { b.day = it }
        return b.build()
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun issueToken(
        principal: com.spoolin.core.features.session.SessionPrincipal,
        method: String,
        operationId: UUID,
        correlation: UUID,
    ): String {
        val signer = issuer ?: throw RateUnavailable()
        return signer.issue(principal, ActorTarget("catalog", method, operationId, correlation))
    }

    private fun stub(token: String): CatalogServiceGrpc.CatalogServiceBlockingStub {
        val meta = Metadata().apply { put(ActorRpc.metadata, token) }
        return catalog.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(meta))
            .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
    }

    private fun grpcContext(operationId: UUID, correlationId: UUID, policyRevision: String): Spoolin.Context =
        Spoolin.Context.newBuilder()
            .setOperationId(operationId.toString())
            .setCorrelationId(correlationId.toString())
            .setPolicyRevision(policyRevision)
            .build()

    private fun context(env: DataFetchingEnvironment): BrowserContext =
        requireNotNull(env.graphQlContext.get("browser"))

    private fun id(env: DataFetchingEnvironment): UUID =
        UUID.fromString(env.getArgument<String>("operationId"))

    private fun validateUuid(id: String) {
        runCatching { UUID.fromString(id).also { require(it.toString() == id) } }
            .onFailure { throw InputViolation("id", "Supply a lowercase, hyphenated UUID.") }
    }
}
