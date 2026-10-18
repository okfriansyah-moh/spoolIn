@file:Suppress("TooManyFunctions")

package com.spoolin.core.features.catalogview.read

import com.spoolin.support.policy.InputViolation
import com.spoolin.support.policy.PublicPolicy
import graphql.schema.DataFetchingEnvironment
import graphql.schema.idl.RuntimeWiring
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.Spoolin
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val CATALOG_DEADLINE_SECONDS = 3L
private const val SEARCH_MAX_CODE_POINTS = 200

/**
 * GraphQL resolvers for public catalog read operations.
 *
 * Proxies to Catalog gRPC service (SearchTitles, GetTitle, SearchPeople, GetPerson,
 * TitleCredits, PersonCredits, ListGenres, ListRoles).
 *
 * F-PERMISSIONS: public queries; no actor token required.
 * Account PII never escapes the declared projection.
 * Archived titles/people return null (not a same-name substitute).
 */
class CatalogViewGraphql(
    private val catalog: CatalogServiceGrpc.CatalogServiceBlockingStub,
    private val policy: PublicPolicy,
) {
    fun bind(wiring: RuntimeWiring.Builder) {
        wiring.type("Query") { q ->
            q
                .dataFetcher("titles") { env -> titles(env) }
                .dataFetcher("title") { env -> title(env) }
                .dataFetcher("people") { env -> people(env) }
                .dataFetcher("person") { env -> person(env) }
                .dataFetcher("titleCredits") { env -> titleCredits(env) }
                .dataFetcher("personCredits") { env -> personCredits(env) }
                .dataFetcher("genres") { _ -> genres() }
                .dataFetcher("creditRoles") { _ -> creditRoles() }
        }
        CatalogMetadataProjection { stub().listGenres(com.google.protobuf.Empty.getDefaultInstance()) }.bind(wiring)
        // Resolver for Title.credits (nested, resolved lazily per title)
        wiring.type("Title") { t ->
            t.dataFetcher("credits") { env -> titleCreditsField(env) }
        }
    }

    // ── titles ────────────────────────────────────────────────────────────────

    private fun titles(env: DataFetchingEnvironment): Map<String, Any?> {
        val filter = requireNotNull(env.getArgument<Map<String, Any?>>("filter"))
        val first = (env.getArgument<Int>("first") ?: DEFAULT_FIRST).coerceIn(1, MAX_PAGE)
        val after = env.getArgument<String>("after")
        val request = buildTitleSearchRequest(filter, first, after, ::validateSearchText)
        val result = stub().searchTitles(request)
        return titlePageMap(result)
    }

    // ── title ─────────────────────────────────────────────────────────────────

    private fun title(env: DataFetchingEnvironment): Map<String, Any?>? {
        val id = requireNotNull(env.getArgument<String>("id"))
        validateUuid(id)
        val result =
            stub().getTitle(
                Spoolin.Id
                    .newBuilder()
                    .setId(id)
                    .build(),
            )
        // Empty title (no id) = archived/deleted — return null
        if (result.id.isEmpty()) return null
        return titleMap(result)
    }

    // ── people ────────────────────────────────────────────────────────────────

    private fun people(env: DataFetchingEnvironment): Map<String, Any?> {
        val text = (env.getArgument<String>("text"))?.also { validateSearchText(it) }
        val first = (env.getArgument<Int>("first") ?: DEFAULT_FIRST).coerceIn(1, MAX_PAGE)
        val after = env.getArgument<String>("after")

        val request =
            Spoolin.Search
                .newBuilder()
                .setFirst(first)
                .also { b ->
                    text?.let { b.text = it }
                    after?.let { b.after = it }
                }.build()

        val result = stub().searchPeople(request)
        return personPageMap(result)
    }

    // ── person ────────────────────────────────────────────────────────────────

    private fun person(env: DataFetchingEnvironment): Map<String, Any?>? {
        val id = requireNotNull(env.getArgument<String>("id"))
        validateUuid(id)
        val result =
            stub().getPerson(
                Spoolin.Id
                    .newBuilder()
                    .setId(id)
                    .build(),
            )
        if (result.id.isEmpty()) return null
        return personMap(result)
    }

    // ── titleCredits ─────────────────────────────────────────────────────────

    private fun titleCredits(env: DataFetchingEnvironment): Map<String, Any?> {
        val id = requireNotNull(env.getArgument<String>("id"))
        validateUuid(id)
        val first = (env.getArgument<Int>("first") ?: DEFAULT_FIRST).coerceIn(1, MAX_PAGE)
        val after = env.getArgument<String>("after")

        val request =
            Spoolin.PageRequest
                .newBuilder()
                .setTargetId(id)
                .setFirst(first)
                .also { b -> after?.let { b.after = it } }
                .build()

        return creditPageMap(stub().titleCredits(request))
    }

    // ── personCredits ─────────────────────────────────────────────────────────

    private fun personCredits(env: DataFetchingEnvironment): Map<String, Any?> {
        val id = requireNotNull(env.getArgument<String>("id"))
        validateUuid(id)
        val first = (env.getArgument<Int>("first") ?: DEFAULT_FIRST).coerceIn(1, MAX_PAGE)
        val after = env.getArgument<String>("after")

        val request =
            Spoolin.PageRequest
                .newBuilder()
                .setTargetId(id)
                .setFirst(first)
                .also { b -> after?.let { b.after = it } }
                .build()

        return creditPageMap(stub().personCredits(request))
    }

    // ── title.credits (inline field resolver) ─────────────────────────────────

    private fun titleCreditsField(env: DataFetchingEnvironment): Map<String, Any?> {
        val source = env.getSource<Map<String, Any?>>()
        val id = source?.get("id") as? String ?: return emptyCreditPage()
        val first = (env.getArgument<Int>("first") ?: DEFAULT_FIRST).coerceIn(1, MAX_PAGE)
        val after = env.getArgument<String>("after")

        val request =
            Spoolin.PageRequest
                .newBuilder()
                .setTargetId(id)
                .setFirst(first)
                .also { b -> after?.let { b.after = it } }
                .build()

        return creditPageMap(stub().titleCredits(request))
    }

    // ── genres ────────────────────────────────────────────────────────────────

    private fun genres(): List<Map<String, Any?>> {
        val result =
            stub().listGenres(
                com.google.protobuf.Empty
                    .getDefaultInstance(),
            )
        return result.nodesList.map { g -> mapOf("id" to g.id, "label" to g.label) }
    }

    // ── creditRoles ───────────────────────────────────────────────────────────

    private fun creditRoles(): List<Map<String, Any?>> {
        val result =
            stub().listRoles(
                com.google.protobuf.Empty
                    .getDefaultInstance(),
            )
        return result.nodesList.map { r ->
            mapOf(
                "id" to r.id,
                "label" to r.label,
                "category" to r.category,
                "allowsCharacters" to r.allowsCharacters,
            )
        }
    }

    // ── proto → GraphQL projection maps ──────────────────────────────────────

    private fun titlePageMap(proto: Spoolin.TitlePage): Map<String, Any?> =
        mapOf(
            "nodes" to proto.nodesList.map { titleMap(it) },
            "pageInfo" to
                mapOf(
                    "endCursor" to proto.endCursor.takeIf { it.isNotEmpty() },
                    "hasNextPage" to proto.hasNextPage,
                ),
        )

    private fun titleMap(t: Spoolin.Title): Map<String, Any?> =
        mapOf(
            "id" to t.id,
            "kind" to t.kind,
            "state" to t.state,
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
            "_artworkAssetId" to t.assetId.takeIf { it.isNotBlank() },
            "_artworkManagement" to false,
            "version" to t.version.toString(),
            // credits is a lazy field resolved by titleCreditsField
        )

    private fun personPageMap(proto: Spoolin.PersonPage): Map<String, Any?> =
        mapOf(
            "nodes" to proto.nodesList.map { personMap(it) },
            "pageInfo" to
                mapOf(
                    "endCursor" to proto.endCursor.takeIf { it.isNotEmpty() },
                    "hasNextPage" to proto.hasNextPage,
                ),
        )

    private fun personMap(p: Spoolin.Person): Map<String, Any?> =
        mapOf(
            "id" to p.id,
            "name" to p.values.name,
            "biography" to p.values.biography.takeIf { it.isNotEmpty() },
            "state" to p.state,
            "version" to p.version.toString(),
        )

    private fun creditPageMap(proto: Spoolin.CreditPage): Map<String, Any?> =
        mapOf(
            "nodes" to proto.nodesList.map { creditMap(it) },
            "pageInfo" to
                mapOf(
                    "endCursor" to proto.endCursor.takeIf { it.isNotEmpty() },
                    "hasNextPage" to proto.hasNextPage,
                ),
        )

    private fun creditMap(c: Spoolin.Credit): Map<String, Any?> =
        mapOf(
            "id" to c.id,
            "personId" to c.personId,
            "personName" to c.personName,
            "titleId" to c.titleId,
            "titleName" to c.titleName,
            "role" to
                mapOf(
                    "id" to c.role.id,
                    "label" to c.role.label,
                    "category" to c.role.category,
                    "allowsCharacters" to c.role.allowsCharacters,
                ),
            "characters" to c.charactersList,
            "version" to c.version.toString(),
        )

    private fun partialDateMap(pd: Spoolin.PartialDate): Map<String, Any?> =
        mapOf(
            "year" to pd.year.takeIf { pd.hasYear() },
            "month" to pd.month.takeIf { pd.hasMonth() },
            "day" to pd.day.takeIf { pd.hasDay() },
            "precision" to
                when {
                    pd.hasDay() -> "DAY"
                    pd.hasMonth() -> "MONTH"
                    pd.hasYear() -> "YEAR"
                    else -> "UNKNOWN"
                },
        )

    private fun provenanceMap(p: Spoolin.Provenance): Map<String, Any?> =
        mapOf(
            "source" to p.source,
            "externalId" to p.externalId,
            "recordUrl" to p.recordUrl.takeIf { it.isNotEmpty() },
            "retrievedAt" to Instant.ofEpochSecond(p.retrievedAt.seconds, p.retrievedAt.nanos.toLong()).toString(),
            "reuseNote" to p.reuseNote,
            "fieldSources" to CatalogMetadataProjection.fieldSources(p),
            "locallyChanged" to p.locallyChangedList,
        )

    private fun emptyCreditPage(): Map<String, Any?> =
        mapOf(
            "nodes" to emptyList<Any>(),
            "pageInfo" to mapOf("endCursor" to null, "hasNextPage" to false),
        )

    // ── validation ────────────────────────────────────────────────────────────

    private fun validateSearchText(text: String) {
        val maxCodePoints =
            policy.requestLimits.getOrDefault("maxSearchCodePoints", SEARCH_MAX_CODE_POINTS) as? Int
                ?: SEARCH_MAX_CODE_POINTS
        if (text.codePointCount(0, text.length) > maxCodePoints) {
            throw InputViolation("text", "Search text exceeds the maximum length of $maxCodePoints code points.")
        }
    }

    private fun validateUuid(id: String) {
        runCatching { UUID.fromString(id).also { require(it.toString() == id) } }
            .onFailure { throw InputViolation("id", "Supply a lowercase, hyphenated UUID.") }
    }

    private fun stub() = catalog.withDeadlineAfter(CATALOG_DEADLINE_SECONDS, TimeUnit.SECONDS)

    companion object {
        private const val DEFAULT_FIRST = 24
        private const val MAX_PAGE = 100
    }
}
