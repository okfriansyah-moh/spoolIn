package com.spoolin.catalog.boundary.grpc

import com.google.protobuf.Timestamp
import com.spoolin.catalog.features.person.read.PersonSearch
import com.spoolin.catalog.features.provenance.ProvenanceReads
import com.spoolin.catalog.features.search.CatalogCursor
import com.spoolin.catalog.features.search.PeopleCursor
import com.spoolin.catalog.features.search.TitleSearchSql
import com.spoolin.catalog.features.taxonomy.read.TaxonomyReads
import com.spoolin.catalog.operations.receipts.OwnerReceiptReads
import com.spoolin.support.receipts.ReceiptAuthority
import com.spoolin.support.receipts.ReceiptOutcome
import io.grpc.Status
import io.grpc.stub.StreamObserver
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.Spoolin
import spoolin.v1.Spoolin.CreditPage
import spoolin.v1.Spoolin.Genres
import spoolin.v1.Spoolin.Id
import spoolin.v1.Spoolin.Outcome
import spoolin.v1.Spoolin.PageRequest
import spoolin.v1.Spoolin.PartialDate
import spoolin.v1.Spoolin.Person
import spoolin.v1.Spoolin.PersonPage
import spoolin.v1.Spoolin.PersonValues
import spoolin.v1.Spoolin.Provenance
import spoolin.v1.Spoolin.Roles
import spoolin.v1.Spoolin.Search
import spoolin.v1.Spoolin.Title
import spoolin.v1.Spoolin.TitlePage
import java.time.Clock
import java.util.UUID

private const val DEFAULT_FIRST = 24

/**
 * Catalog gRPC service — public read methods and operation receipt lookup.
 *
 * Public reads (no actor required, mTLS "core" identity only):
 *   SearchTitles, GetTitle, SearchPeople, GetPerson,
 *   PersonCredits, TitleCredits, ListGenres, ListRoles.
 * Protected: GetOperation (actor-scoped receipt lookup).
 */
class CatalogServiceRpc(
    private val titleSearch: TitleSearchSql,
    private val personSearch: PersonSearch,
    private val taxonomy: TaxonomyReads,
    private val provenance: ProvenanceReads,
    private val receiptReads: OwnerReceiptReads,
    private val receiptAuthority: ReceiptAuthority,
    private val titleCursor: CatalogCursor,
    private val peopleCursor: PeopleCursor,
    private val clock: Clock = Clock.systemUTC(),
) : CatalogServiceGrpc.CatalogServiceImplBase() {

    // ── GetOperation ─────────────────────────────────────────────────────────

    override fun getOperation(request: Id, response: StreamObserver<Outcome>) {
        try {
            val actor = receiptAuthority.actorScope() ?: throw Status.PERMISSION_DENIED.asRuntimeException()
            val id = parseId(request.id)
            val receipt = receiptReads.find(id, actor) ?: throw Status.NOT_FOUND.asRuntimeException()
            response.onNext(outcomeProto(receipt))
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        }
    }

    // ── SearchTitles ─────────────────────────────────────────────────────────

    override fun searchTitles(request: Search, response: StreamObserver<TitlePage>) {
        try {
            val text = request.text.takeIf { it.isNotEmpty() }
            val kind = request.kind.takeIf { it.isNotEmpty() }
            val genreIds = request.genreIdsList.map { parseId(it) }
            val year = if (request.hasYear()) request.year else null
            val month = if (request.hasMonth()) request.month else null
            val first = if (request.first > 0) request.first else DEFAULT_FIRST
            val scopeHash = CatalogCursor.scopeHash(text, kind, request.genreIdsList, year, month, "public")
            val afterCursor = request.after.takeIf { it.isNotEmpty() }?.let { raw ->
                titleCursor.decode(raw, scopeHash, clock.instant())
                    ?: throw Status.INVALID_ARGUMENT.withDescription("INVALID_CURSOR").asRuntimeException()
            }
            val page = titleSearch.search(text, kind, genreIds, year, month, first, afterCursor)
            val builder = TitlePage.newBuilder()
            page.rows.forEach { row ->
                val genreRows = titleSearch.findGenreIds(row.id)
                val provenanceRows = provenance.forSubject(row.id, "TITLE")
                builder.addNodes(titleProto(row, genreRows, provenanceRows))
            }
            page.keyset?.let { ks -> builder.endCursor = titleCursor.encode(ks, scopeHash, clock.instant()) }
            builder.hasNextPage = page.hasNextPage
            response.onNext(builder.build())
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        } catch (e: IllegalArgumentException) {
            response.onError(Status.INVALID_ARGUMENT.withDescription(e.message).asRuntimeException())
        }
    }

    // ── GetTitle ─────────────────────────────────────────────────────────────

    override fun getTitle(request: Id, response: StreamObserver<Title>) {
        try {
            val id = parseId(request.id)
            val row = titleSearch.findById(id)
            // hidden/deleted/absent: return default (empty) Title — not a substitute
            if (row == null) {
                response.onNext(Title.getDefaultInstance())
                response.onCompleted()
                return
            }
            val genreIds = titleSearch.findGenreIds(id)
            val provenanceRows = provenance.forSubject(id, "TITLE")
            response.onNext(titleProto(row, genreIds, provenanceRows))
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        }
    }

    // ── SearchPeople ─────────────────────────────────────────────────────────

    override fun searchPeople(request: Search, response: StreamObserver<PersonPage>) {
        try {
            val text = request.text.takeIf { it.isNotEmpty() }
            val first = if (request.first > 0) request.first else DEFAULT_FIRST
            val scopeHash = CatalogCursor.scopeHash(text, null, emptyList(), null, null, "public-people")
            val afterCursor = request.after.takeIf { it.isNotEmpty() }?.let { raw ->
                peopleCursor.decode(raw, scopeHash, clock.instant())?.let {
                    PersonSearch.PersonKeyset(it.name, it.id)
                } ?: throw Status.INVALID_ARGUMENT.withDescription("INVALID_CURSOR").asRuntimeException()
            }
            val page = personSearch.searchPeople(text, first, afterCursor)
            val builder = PersonPage.newBuilder()
            page.rows.forEach { builder.addNodes(personProto(it)) }
            page.keyset?.let { ks ->
                builder.endCursor = peopleCursor.encode(
                    PeopleCursor.Keyset(ks.name, ks.id),
                    scopeHash,
                    clock.instant(),
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

    // ── GetPerson ────────────────────────────────────────────────────────────

    override fun getPerson(request: Id, response: StreamObserver<Person>) {
        try {
            val id = parseId(request.id)
            val row = personSearch.findPerson(id)
            if (row == null) {
                response.onNext(Person.getDefaultInstance())
                response.onCompleted()
                return
            }
            response.onNext(personProto(row))
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        }
    }

    // ── TitleCredits ─────────────────────────────────────────────────────────

    override fun titleCredits(request: PageRequest, response: StreamObserver<CreditPage>) {
        try {
            val titleId = parseId(request.targetId)
            val first = if (request.first > 0) request.first else DEFAULT_FIRST
            val scope = creditScope(titleId)
            val afterKeyset = request.after.takeIf { it.isNotEmpty() }?.let { raw ->
                decodeCreditCursor(raw, scope)
                    ?: throw Status.INVALID_ARGUMENT.withDescription("INVALID_CURSOR").asRuntimeException()
            }
            val page = personSearch.titleCredits(titleId, first, afterKeyset)
            response.onNext(creditPageProto(page, scope))
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        }
    }

    // ── PersonCredits ────────────────────────────────────────────────────────

    override fun personCredits(request: PageRequest, response: StreamObserver<CreditPage>) {
        try {
            val personId = parseId(request.targetId)
            val first = if (request.first > 0) request.first else DEFAULT_FIRST
            val scope = creditScope(personId)
            val afterKeyset = request.after.takeIf { it.isNotEmpty() }?.let { raw ->
                decodeCreditCursor(raw, scope)
                    ?: throw Status.INVALID_ARGUMENT.withDescription("INVALID_CURSOR").asRuntimeException()
            }
            val page = personSearch.personCredits(personId, first, afterKeyset)
            response.onNext(creditPageProto(page, scope))
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        }
    }

    // ── ListGenres ───────────────────────────────────────────────────────────

    override fun listGenres(request: com.google.protobuf.Empty, response: StreamObserver<Genres>) {
        try {
            val builder = Genres.newBuilder()
            taxonomy.listGenres().forEach { g ->
                builder.addNodes(Spoolin.Genre.newBuilder().setId(g.id.toString()).setLabel(g.label).build())
            }
            response.onNext(builder.build())
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        }
    }

    // ── ListRoles ────────────────────────────────────────────────────────────

    override fun listRoles(request: com.google.protobuf.Empty, response: StreamObserver<Roles>) {
        try {
            val builder = Roles.newBuilder()
            taxonomy.listRoles().forEach { r ->
                builder.addNodes(
                    Spoolin.CreditRole.newBuilder()
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

    // ── protobuf mappers ─────────────────────────────────────────────────────

    private fun titleProto(
        row: TitleSearchSql.TitleRow,
        genreIds: List<UUID>,
        provenanceRows: List<ProvenanceReads.ProvenanceRow>,
    ): Title {
        val values = Spoolin.MovieValues.newBuilder()
            .setTitle(row.displayTitle)
            .setSynopsis(row.synopsis)
            .setSynopsisLanguage(row.synopsisLanguage)
            .addAllCountries(row.countries)
            .addAllGenreIds(genreIds.map { it.toString() })
            .setRelease(partialDateProto(row.releaseYear, row.releaseMonth, row.releaseDay))
            .also { b ->
                row.originalTitle?.let { b.originalTitle = it }
                row.originalLanguage?.let { b.originalLanguage = it }
                row.runtimeMinutes?.let { b.runtimeMinutes = it }
            }
            .build()

        return Title.newBuilder()
            .setId(row.id.toString())
            .setKind(row.kind)
            .setState(row.status)
            .setValues(values)
            .setVersion(row.version)
            .setTvEnd(partialDateProto(row.endYear, row.endMonth, row.endDay))
            .addAllProvenance(provenanceRows.map { provenanceProto(it) })
            .also { b ->
                row.assetId?.let { b.assetId = it.toString() }
                row.tvStatus?.let { b.tvStatus = it }
                row.releaseContext?.let { b.releaseContext = it }
            }
            .build()
    }

    private fun personProto(row: PersonSearch.PersonRow): Person =
        Person.newBuilder()
            .setId(row.id.toString())
            .setValues(
                PersonValues.newBuilder()
                    .setName(row.name)
                    .also { b -> row.biography?.let { b.biography = it } }
                    .build(),
            )
            .setState(row.status)
            .setVersion(row.version)
            .build()

    private fun creditPageProto(page: PersonSearch.CreditPage, scope: String): CreditPage {
        val builder = CreditPage.newBuilder()
        page.rows.forEach { r ->
            builder.addNodes(
                Spoolin.Credit.newBuilder()
                    .setId(r.id.toString())
                    .setTitleId(r.titleId.toString())
                    .setPersonId(r.personId.toString())
                    .setRoleId(r.roleId.toString())
                    .addAllCharacters(r.characters)
                    .setVersion(r.version)
                    .setPersonName(r.personName)
                    .setTitleName(r.titleName)
                    .setRole(
                        Spoolin.CreditRole.newBuilder()
                            .setId(r.roleId.toString())
                            .setCategory(r.roleCategory)
                            .setLabel(r.roleLabel)
                            .setAllowsCharacters(r.roleAllowsCharacters)
                            .build(),
                    )
                    .build(),
            )
        }
        page.keyset?.let { builder.endCursor = encodeCreditCursor(it, scope) }
        builder.hasNextPage = page.hasNextPage
        return builder.build()
    }

    private fun partialDateProto(year: Int?, month: Int?, day: Int?): PartialDate =
        PartialDate.newBuilder()
            .also { b ->
                year?.let { b.year = it }
                month?.let { b.month = it }
                day?.let { b.day = it }
            }
            .build()

    private fun provenanceProto(row: ProvenanceReads.ProvenanceRow): Provenance {
        val ts = Timestamp.newBuilder()
            .setSeconds(row.retrievedAt.epochSecond)
            .setNanos(row.retrievedAt.nano)
            .build()
        return Provenance.newBuilder()
            .setSource(row.source)
            .setExternalId(row.externalId)
            .setRetrievedAt(ts)
            .setReuseNote(row.reuseNote)
            .addAllLocallyChanged(row.locallyChanged)
            .also { b -> row.recordUrl?.let { b.recordUrl = it } }
            .build()
    }

    private fun outcomeProto(receipt: ReceiptOutcome): Outcome {
        val b = Outcome.newBuilder()
            .setOperationId(receipt.id.toString())
            .setState(receipt.state.name)
            .setResultExpired(receipt.resultExpired)
        receipt.result?.let { r ->
            b.resultId = r.resultId.toString()
            r.version?.let { b.version = it }
        }
        receipt.errorCode?.let { b.problem = Spoolin.Problem.newBuilder().setCode(it).build() }
        return b.build()
    }

    // ── credit cursor ────────────────────────────────────────────────────────

    /** Scope binds the cursor to a specific target (title or person) to prevent cross-use. */
    private fun creditScope(targetId: UUID): String = "credit-scope-v1:$targetId"

    private fun encodeCreditCursor(ks: PersonSearch.CreditKeyset, scope: String): String {
        val keyset = CatalogCursor.Keyset(null, null, null, ks.id)
        return titleCursor.encode(keyset, scope, clock.instant())
    }

    private fun decodeCreditCursor(raw: String, scope: String): PersonSearch.CreditKeyset? {
        val ks = titleCursor.decode(raw, scope, clock.instant()) ?: return null
        return PersonSearch.CreditKeyset(ks.id)
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun parseId(value: String): UUID =
        runCatching { UUID.fromString(value).also { require(it.toString() == value) } }
            .getOrElse { throw Status.INVALID_ARGUMENT.withDescription("INVALID_ID").asRuntimeException() }

}
