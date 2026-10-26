@file:Suppress("TooManyFunctions")

package com.spoolin.core.features.catalogview.read

import com.spoolin.core.features.catalogview.CatalogGraphqlArguments
import com.spoolin.support.policy.PublicPolicy
import com.spoolin.support.policy.ReadPolicy
import graphql.schema.DataFetchingEnvironment
import graphql.schema.idl.RuntimeWiring
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.Spoolin
import java.util.concurrent.TimeUnit

private const val CATALOG_DEADLINE_SECONDS = 3L

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
    private val reads = ReadPolicy(policy)

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
                .dataFetcher("catalogFilterOptions") { _ -> catalogFilterOptions() }
                .dataFetcher("creditRoles") { _ -> creditRoles() }
        }
        CatalogMetadataProjection {
            stub().listGenres(
                com.google.protobuf.Empty
                    .getDefaultInstance(),
            )
        }.bind(wiring)
        // Resolver for Title.credits (nested, resolved lazily per title)
        wiring.type("Title") { t ->
            t.dataFetcher("credits") { env -> titleCreditsField(env) }
        }
    }

    // ── titles ────────────────────────────────────────────────────────────────

    private fun titles(env: DataFetchingEnvironment): Map<String, Any?> {
        val filter = requireNotNull(env.getArgument<Map<String, Any?>>("filter"))
        val first = reads.pageSize(env.getArgument<Int>("first"))
        val after = env.getArgument<String>("after")
        val request = buildTitleSearchRequest(filter, first, after, ::validateSearchText, reads::search)
        val result = stub().searchTitles(request)
        return CatalogReadProjection.titlePage(result, management = false)
    }

    // ── title ─────────────────────────────────────────────────────────────────

    private fun title(env: DataFetchingEnvironment): Map<String, Any?>? {
        val id = requireNotNull(env.getArgument<String>("id"))
        CatalogGraphqlArguments.validateId(id)
        val result =
            stub().getTitle(
                Spoolin.Id
                    .newBuilder()
                    .setId(id)
                    .build(),
            )
        // Empty title (no id) = archived/deleted — return null
        if (result.id.isEmpty()) return null
        return CatalogReadProjection.title(result, management = false)
    }

    // ── people ────────────────────────────────────────────────────────────────

    private fun people(env: DataFetchingEnvironment): Map<String, Any?> {
        val text = reads.search(env.getArgument<String>("text"))
        val first = reads.pageSize(env.getArgument<Int>("first"))
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
        return CatalogReadProjection.personPage(result)
    }

    // ── person ────────────────────────────────────────────────────────────────

    private fun person(env: DataFetchingEnvironment): Map<String, Any?>? {
        val id = requireNotNull(env.getArgument<String>("id"))
        CatalogGraphqlArguments.validateId(id)
        val result =
            stub().getPerson(
                Spoolin.Id
                    .newBuilder()
                    .setId(id)
                    .build(),
            )
        if (result.id.isEmpty()) return null
        return CatalogReadProjection.person(result)
    }

    // ── titleCredits ─────────────────────────────────────────────────────────

    private fun titleCredits(env: DataFetchingEnvironment): Map<String, Any?> {
        val id = requireNotNull(env.getArgument<String>("id"))
        CatalogGraphqlArguments.validateId(id)
        val first = reads.pageSize(env.getArgument<Int>("first"))
        val after = env.getArgument<String>("after")

        val request =
            Spoolin.PageRequest
                .newBuilder()
                .setTargetId(id)
                .setFirst(first)
                .also { b -> after?.let { b.after = it } }
                .build()

        return CatalogReadProjection.creditPage(stub().titleCredits(request))
    }

    // ── personCredits ─────────────────────────────────────────────────────────

    private fun personCredits(env: DataFetchingEnvironment): Map<String, Any?> {
        val id = requireNotNull(env.getArgument<String>("id"))
        CatalogGraphqlArguments.validateId(id)
        val first = reads.pageSize(env.getArgument<Int>("first"))
        val after = env.getArgument<String>("after")

        val request =
            Spoolin.PageRequest
                .newBuilder()
                .setTargetId(id)
                .setFirst(first)
                .also { b -> after?.let { b.after = it } }
                .build()

        return CatalogReadProjection.creditPage(stub().personCredits(request))
    }

    // ── title.credits (inline field resolver) ─────────────────────────────────

    private fun titleCreditsField(env: DataFetchingEnvironment): Map<String, Any?> {
        val source = env.getSource<Map<String, Any?>>()
        val id = source?.get("id") as? String ?: return emptyCreditPage()
        val first = reads.pageSize(env.getArgument<Int>("first"))
        val after = env.getArgument<String>("after")

        val request =
            Spoolin.PageRequest
                .newBuilder()
                .setTargetId(id)
                .setFirst(first)
                .also { b -> after?.let { b.after = it } }
                .build()

        return CatalogReadProjection.creditPage(stub().titleCredits(request))
    }

    // ── genres ────────────────────────────────────────────────────────────────

    private fun catalogFilterOptions(): Map<String, Any?> {
        val result =
            stub().getCatalogFilterOptions(
                com.google.protobuf.Empty
                    .getDefaultInstance(),
            )
        return mapOf(
            "genres" to result.genresList.map { mapOf("id" to it.id, "label" to it.label) },
            "releaseYears" to result.releaseYearsList.map { mapOf("year" to it.year, "months" to it.monthsList) },
        )
    }

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

    private fun emptyCreditPage(): Map<String, Any?> =
        mapOf(
            "nodes" to emptyList<Any>(),
            "pageInfo" to mapOf("endCursor" to null, "hasNextPage" to false),
        )

    // ── validation ────────────────────────────────────────────────────────────

    private fun validateSearchText(text: String) {
        reads.search(text)
    }

    private fun stub() = catalog.withDeadlineAfter(CATALOG_DEADLINE_SECONDS, TimeUnit.SECONDS)
}
