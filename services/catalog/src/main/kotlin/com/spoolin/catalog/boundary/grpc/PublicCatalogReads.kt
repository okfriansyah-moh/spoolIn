package com.spoolin.catalog.boundary.grpc

import com.spoolin.catalog.features.person.read.PersonSearch
import com.spoolin.catalog.features.provenance.ProvenanceReads
import com.spoolin.catalog.features.search.PeopleCursor
import com.spoolin.catalog.features.search.TitleOrdering
import com.spoolin.catalog.features.search.TitleSearchSql
import com.spoolin.catalog.features.taxonomy.read.TaxonomyReads
import com.spoolin.support.policy.ReadPolicy
import io.grpc.Status
import io.grpc.stub.StreamObserver
import spoolin.v1.Spoolin
import spoolin.v1.Spoolin.CreditPage
import spoolin.v1.Spoolin.Genres
import spoolin.v1.Spoolin.Id
import spoolin.v1.Spoolin.PageRequest
import spoolin.v1.Spoolin.Person
import spoolin.v1.Spoolin.PersonPage
import spoolin.v1.Spoolin.Roles
import spoolin.v1.Spoolin.Search
import spoolin.v1.Spoolin.Title
import spoolin.v1.Spoolin.TitlePage

internal class PublicCatalogReads(
    private val titleSearch: TitleSearchSql,
    private val personSearch: PersonSearch,
    private val taxonomy: TaxonomyReads,
    private val provenance: ProvenanceReads,
    private val context: PublicReadContext,
) {
    private val projection = PublicReadProjection(context)

    fun searchTitles(
        request: Search,
        response: StreamObserver<TitlePage>,
    ) {
        try {
            val (page, scopeHash) = findTitles(request)
            val ids = page.rows.map { it.id }
            val genres = titleSearch.findGenresForTitles(ids)
            val sources = provenance.forSubjects(ids, "TITLE")
            val builder = TitlePage.newBuilder()
            page.rows.forEach { row ->
                builder.addNodes(projection.titleProto(row, genres[row.id].orEmpty(), sources[row.id].orEmpty()))
            }
            page.keyset?.let { ks ->
                builder.endCursor = context.titleCursor.encode(ks, scopeHash, context.clock.instant())
            }
            builder.hasNextPage = page.hasNextPage
            response.onNext(builder.build())
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        } catch (e: IllegalArgumentException) {
            response.onError(Status.INVALID_ARGUMENT.withDescription(e.message).asRuntimeException())
        }
    }

    private fun findTitles(request: Search): Pair<TitleSearchSql.TitlePage, String> {
        val text = context.searchText(request.text.takeIf { request.hasText() })
        val kind = request.kind.takeIf { it.isNotEmpty() }
        val genreIds = request.genreIdsList.map { context.parseId(it) }
        val year = if (request.hasYear()) request.year else null
        val month = if (request.hasMonth()) request.month else null
        val first = context.pageSize(request.first)
        val sort = TitleOrdering.parse(request.sort.takeIf { request.hasSort() })
        val scopeHash =
            ReadPolicy(context.policy()).cursorScope(
                "public-titles",
                first,
                mapOf(
                    "text" to text,
                    "kind" to kind,
                    "genres" to request.genreIdsList.distinct().sorted(),
                    "year" to year,
                    "month" to month,
                ) + if (sort == TitleOrdering.RELEASE_DESC) emptyMap() else mapOf("sort" to sort.name),
            )
        val afterCursor = context.titleAfter(request.after, scopeHash)
        val page = titleSearch.search(text, kind, genreIds, year, month, first, afterCursor, sort)
        return page to scopeHash
    }

    fun getTitle(
        request: Id,
        response: StreamObserver<Title>,
    ) {
        try {
            val id = context.parseId(request.id)
            val row = titleSearch.findById(id)
            // hidden/deleted/absent: return default (empty) Title — not a substitute
            if (row == null) {
                response.onNext(Title.getDefaultInstance())
                response.onCompleted()
                return
            }
            val genreIds = titleSearch.findGenreIds(id)
            val provenanceRows = provenance.forSubject(id, "TITLE")
            response.onNext(projection.titleProto(row, genreIds, provenanceRows))
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        }
    }

    fun searchPeople(
        request: Search,
        response: StreamObserver<PersonPage>,
    ) {
        try {
            val text = context.searchText(request.text.takeIf { request.hasText() })
            val first = context.pageSize(request.first)
            val scopeHash = context.peopleScope(request, text, first)
            val afterCursor = context.peopleAfter(request.after, scopeHash)
            val page = personSearch.searchPeople(text, first, afterCursor)
            val builder = PersonPage.newBuilder()
            page.rows.forEach { builder.addNodes(projection.personProto(it)) }
            page.keyset?.let { ks ->
                builder.endCursor =
                    context.peopleCursor.encode(
                        PeopleCursor.Keyset(ks.name, ks.id),
                        scopeHash,
                        context.clock.instant(),
                    )
            }
            builder.hasNextPage = page.hasNextPage
            response.onNext(builder.build())
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        } catch (e: IllegalArgumentException) {
            response.onError(Status.INVALID_ARGUMENT.withDescription(e.message).asRuntimeException())
        }
    }

    fun getPerson(
        request: Id,
        response: StreamObserver<Person>,
    ) {
        try {
            val id = context.parseId(request.id)
            val row = personSearch.findPerson(id)
            if (row == null) {
                response.onNext(Person.getDefaultInstance())
                response.onCompleted()
                return
            }
            response.onNext(projection.personProto(row))
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        }
    }

    fun titleCredits(
        request: PageRequest,
        response: StreamObserver<CreditPage>,
    ) {
        try {
            val titleId = context.parseId(request.targetId)
            val first = context.pageSize(request.first)
            val scope = context.creditScope(titleId, "TITLE", first)
            val afterKeyset =
                request.after.takeIf { it.isNotEmpty() }?.let { raw ->
                    context.decodeCreditCursor(raw, scope)
                        ?: throw Status.INVALID_ARGUMENT.withDescription("INVALID_CURSOR").asRuntimeException()
                }
            val page = personSearch.titleCredits(titleId, first, afterKeyset)
            response.onNext(projection.creditPageProto(page, scope))
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        }
    }

    fun personCredits(
        request: PageRequest,
        response: StreamObserver<CreditPage>,
    ) {
        try {
            val personId = context.parseId(request.targetId)
            val first = context.pageSize(request.first)
            val scope = context.creditScope(personId, "PERSON", first)
            val afterKeyset =
                request.after.takeIf { it.isNotEmpty() }?.let { raw ->
                    context.decodeCreditCursor(raw, scope)
                        ?: throw Status.INVALID_ARGUMENT.withDescription("INVALID_CURSOR").asRuntimeException()
                }
            val page = personSearch.personCredits(personId, first, afterKeyset)
            response.onNext(projection.creditPageProto(page, scope))
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        }
    }

    fun catalogFilterOptions(response: StreamObserver<Spoolin.CatalogFilterOptions>) {
        val options = taxonomy.catalogFilterOptions()
        val result = Spoolin.CatalogFilterOptions.newBuilder()
        options.genres.forEach { genre ->
            result.addGenres(
                Spoolin.Genre
                    .newBuilder()
                    .setId(genre.id.toString())
                    .setLabel(genre.label),
            )
        }
        options.releaseYears.forEach { date ->
            result.addReleaseYears(
                Spoolin.CatalogReleaseYear
                    .newBuilder()
                    .setYear(date.year)
                    .addAllMonths(date.months),
            )
        }
        response.onNext(result.build())
        response.onCompleted()
    }

    fun listGenres(response: StreamObserver<Genres>) {
        try {
            val builder = Genres.newBuilder()
            taxonomy.listGenres().forEach { g ->
                builder.addNodes(
                    Spoolin.Genre
                        .newBuilder()
                        .setId(g.id.toString())
                        .setLabel(g.label)
                        .build(),
                )
            }
            response.onNext(builder.build())
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        }
    }

    fun listRoles(response: StreamObserver<Roles>) {
        try {
            val builder = Roles.newBuilder()
            taxonomy.listRoles().forEach { r ->
                builder.addNodes(
                    Spoolin.CreditRole
                        .newBuilder()
                        .setId(r.id.toString())
                        .setCategory(r.category)
                        .setLabel(r.label)
                        .setAllowsCharacters(r.allowsCharacters)
                        .build(),
                )
            }
            response.onNext(builder.build())
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        }
    }
}
