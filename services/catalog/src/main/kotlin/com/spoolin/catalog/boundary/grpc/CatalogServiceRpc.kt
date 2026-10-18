@file:Suppress("TooManyFunctions", "LongParameterList", "CyclomaticComplexMethod", "NestedBlockDepth")

package com.spoolin.catalog.boundary.grpc

import com.google.protobuf.Timestamp
import com.spoolin.catalog.features.credit.CreditWrites
import com.spoolin.catalog.features.credit.ManagedCreditReads
import com.spoolin.catalog.features.movie.ManagedMovieReads
import com.spoolin.catalog.features.movie.MovieWrites
import com.spoolin.catalog.features.person.read.ManagedPersonReads
import com.spoolin.catalog.features.person.read.PersonSearch
import com.spoolin.catalog.features.person.write.PersonWrites
import com.spoolin.catalog.features.provenance.ProvenanceReads
import com.spoolin.catalog.features.search.CatalogCursor
import com.spoolin.catalog.features.search.PeopleCursor
import com.spoolin.catalog.features.search.TitleSearchSql
import com.spoolin.catalog.features.taxonomy.read.TaxonomyReads
import com.spoolin.catalog.features.taxonomy.roles.RoleWrites
import com.spoolin.catalog.operations.receipts.OwnerReceiptReads
import com.spoolin.support.policy.PublicPolicy
import com.spoolin.support.policy.PolicyEnvironment
import com.spoolin.support.policy.ReadPolicy
import com.spoolin.support.policy.InputViolation
import com.spoolin.support.receipts.ReceiptAuthority
import com.spoolin.support.receipts.ReceiptOutcome
import io.grpc.Status
import io.grpc.stub.StreamObserver
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.Spoolin
import spoolin.v1.Spoolin.AuditPage
import spoolin.v1.Spoolin.CreditPage
import spoolin.v1.Spoolin.CreditWrite
import spoolin.v1.Spoolin.Genres
import spoolin.v1.Spoolin.Id
import spoolin.v1.Spoolin.MovieWrite
import spoolin.v1.Spoolin.Outcome
import spoolin.v1.Spoolin.PageRequest
import spoolin.v1.Spoolin.PartialDate
import spoolin.v1.Spoolin.Person
import spoolin.v1.Spoolin.PersonPage
import spoolin.v1.Spoolin.PersonValues
import spoolin.v1.Spoolin.PersonWrite
import spoolin.v1.Spoolin.Provenance
import spoolin.v1.Spoolin.RoleWrite
import spoolin.v1.Spoolin.Roles
import spoolin.v1.Spoolin.Search
import spoolin.v1.Spoolin.Title
import spoolin.v1.Spoolin.TitlePage
import spoolin.v1.Spoolin.VersionedId
import java.time.Clock
import java.util.UUID


/**
 * Catalog gRPC service — public read methods, write methods, and receipt lookup.
 *
 * Public reads (no actor required, mTLS "core" identity only):
 *   SearchTitles, GetTitle, SearchPeople, GetPerson,
 *   PersonCredits, TitleCredits, ListGenres, ListRoles.
 * Protected writes (MAINTAINER actor required): WriteMovie.
 * Protected managed reads (MAINTAINER actor required):
 *   ManagedTitle, ManagedTitles, Audit.
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
    private val movieWrites: MovieWrites? = null,
    private val managedReads: ManagedMovieReads? = null,
    private val personWrites: PersonWrites? = null,
    private val managedPersonReads: ManagedPersonReads? = null,
    private val roleWrites: RoleWrites? = null,
    private val creditWrites: CreditWrites? = null,
    private val managedCreditReads: ManagedCreditReads? = null,
    private val artwork: com.spoolin.catalog.features.artwork.ArtworkRpc? = null,
    private val movieLifecycle: com.spoolin.catalog.features.lifecycle.MovieLifecycle? = null,
    private val personLifecycle: com.spoolin.catalog.features.lifecycle.PersonLifecycle? = null,
    private val deletionImpact: com.spoolin.catalog.features.lifecycle.DeletionImpact? = null,
    private val proposalWrites: com.spoolin.catalog.features.proposal.ProposalWrites? = null,
    private val policy: () -> PublicPolicy = { PolicyEnvironment.resolve(System::getenv) },
) : CatalogServiceGrpc.CatalogServiceImplBase() {
    override fun getAttachment(
        request: Id,
        response: StreamObserver<spoolin.v1.Spoolin.Attachment>,
    ) {
        val handler = artwork ?: return response.onError(Status.UNAVAILABLE.asRuntimeException())
        handler.getAttachment(request, response)
    }

    override fun resolveArtworkAccess(
        request: spoolin.v1.Spoolin.ArtworkAccessRequest,
        response: StreamObserver<spoolin.v1.Spoolin.ArtworkGrant>,
    ) {
        val handler = artwork ?: return response.onError(Status.UNAVAILABLE.asRuntimeException())
        handler.resolveArtworkAccess(request, response)
    }

    // ── GetOperation ─────────────────────────────────────────────────────────

    override fun getOperation(
        request: Id,
        response: StreamObserver<Outcome>,
    ) {
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

    override fun searchTitles(
        request: Search,
        response: StreamObserver<TitlePage>,
    ) {
        try {
            val text = searchText(request.text.takeIf { request.hasText() })
            val kind = request.kind.takeIf { it.isNotEmpty() }
            val genreIds = request.genreIdsList.map { parseId(it) }
            val year = if (request.hasYear()) request.year else null
            val month = if (request.hasMonth()) request.month else null
            val first = pageSize(request.first)
            val scopeHash = ReadPolicy(policy()).cursorScope(
                "public-titles", first,
                mapOf("text" to text, "kind" to kind, "genres" to request.genreIdsList.distinct().sorted(),
                    "year" to year, "month" to month),
            )
            val afterCursor =
                request.after.takeIf { it.isNotEmpty() }?.let { raw ->
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

    override fun getTitle(
        request: Id,
        response: StreamObserver<Title>,
    ) {
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

    override fun searchPeople(
        request: Search,
        response: StreamObserver<PersonPage>,
    ) {
        try {
            val text = searchText(request.text.takeIf { request.hasText() })
            val first = pageSize(request.first)
            require(!request.hasKind() && request.genreIdsCount == 0 && !request.hasYear() && !request.hasMonth()) {
                "Title filters are not valid for people search"
            }
            val scopeHash = ReadPolicy(policy()).cursorScope("public-people", first, mapOf("text" to text))
            val afterCursor =
                request.after.takeIf { it.isNotEmpty() }?.let { raw ->
                    peopleCursor.decode(raw, scopeHash, clock.instant())?.let {
                        PersonSearch.PersonKeyset(it.name, it.id)
                    } ?: throw Status.INVALID_ARGUMENT.withDescription("INVALID_CURSOR").asRuntimeException()
                }
            val page = personSearch.searchPeople(text, first, afterCursor)
            val builder = PersonPage.newBuilder()
            page.rows.forEach { builder.addNodes(personProto(it)) }
            page.keyset?.let { ks ->
                builder.endCursor =
                    peopleCursor.encode(
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

    override fun getPerson(
        request: Id,
        response: StreamObserver<Person>,
    ) {
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

    override fun titleCredits(
        request: PageRequest,
        response: StreamObserver<CreditPage>,
    ) {
        try {
            val titleId = parseId(request.targetId)
            val first = pageSize(request.first)
            val scope = creditScope(titleId, "TITLE", first)
            val afterKeyset =
                request.after.takeIf { it.isNotEmpty() }?.let { raw ->
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

    override fun personCredits(
        request: PageRequest,
        response: StreamObserver<CreditPage>,
    ) {
        try {
            val personId = parseId(request.targetId)
            val first = pageSize(request.first)
            val scope = creditScope(personId, "PERSON", first)
            val afterKeyset =
                request.after.takeIf { it.isNotEmpty() }?.let { raw ->
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

    override fun listGenres(
        request: com.google.protobuf.Empty,
        response: StreamObserver<Genres>,
    ) {
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

    // ── ListRoles ────────────────────────────────────────────────────────────

    override fun listRoles(
        request: com.google.protobuf.Empty,
        response: StreamObserver<Roles>,
    ) {
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

    override fun movieLifecycle(
        request: spoolin.v1.Spoolin.Lifecycle,
        response: StreamObserver<Outcome>,
    ) {
        val handler = movieLifecycle
        if (handler == null) {
            response.onError(Status.UNAVAILABLE.asRuntimeException())
            return
        }
        handler.apply(request, response)
    }

    override fun personLifecycle(
        request: spoolin.v1.Spoolin.Lifecycle,
        response: StreamObserver<Outcome>,
    ) {
        val handler = personLifecycle
        if (handler == null) {
            response.onError(Status.UNAVAILABLE.asRuntimeException())
            return
        }
        handler.apply(request, response)
    }

    override fun deletionImpact(
        request: Id,
        response: StreamObserver<spoolin.v1.Spoolin.Impact>,
    ) {
        val handler = deletionImpact
        if (handler == null) {
            response.onError(Status.UNAVAILABLE.asRuntimeException())
            return
        }
        handler.preview(request, response)
    }

    override fun beginArtwork(
        request: spoolin.v1.Spoolin.Hold,
        response: StreamObserver<Outcome>,
    ) {
        val handler = artwork
        if (handler == null) {
            response.onError(Status.UNAVAILABLE.withDescription("Artwork unavailable").asRuntimeException())
            return
        }
        handler.beginArtwork(request, response)
    }

    override fun cancelArtwork(
        request: spoolin.v1.Spoolin.CancelArtworkRequest,
        response: StreamObserver<spoolin.v1.Spoolin.ArtworkCancelOutcome>,
    ) {
        val handler = artwork
        if (handler == null) {
            response.onError(Status.UNAVAILABLE.withDescription("Artwork unavailable").asRuntimeException())
            return
        }
        handler.cancelArtwork(request, response)
    }

    // ── WriteMovie ────────────────────────────────────────────────────────────

    override fun writeMovie(
        request: MovieWrite,
        response: StreamObserver<Outcome>,
    ) {
        val handler = movieWrites
        if (handler == null) {
            response.onError(Status.UNAVAILABLE.withDescription("Write operations unavailable").asRuntimeException())
            return
        }
        handler.writeMovie(request, response)
    }

    // ── CreateRole ────────────────────────────────────────────────────────────

    override fun createRole(
        request: RoleWrite,
        response: StreamObserver<Outcome>,
    ) {
        val handler = roleWrites
        if (handler == null) {
            response.onError(Status.UNAVAILABLE.withDescription("Write operations unavailable").asRuntimeException())
            return
        }
        handler.createRole(request, response)
    }

    // ── WriteCredit ───────────────────────────────────────────────────────────

    override fun writeCredit(
        request: CreditWrite,
        response: StreamObserver<Outcome>,
    ) {
        val handler = creditWrites
        if (handler == null) {
            response.onError(Status.UNAVAILABLE.withDescription("Write operations unavailable").asRuntimeException())
            return
        }
        handler.writeCredit(request, response)
    }

    // ── RemoveCredit ──────────────────────────────────────────────────────────

    override fun removeCredit(
        request: VersionedId,
        response: StreamObserver<Outcome>,
    ) {
        val handler = creditWrites
        if (handler == null) {
            response.onError(Status.UNAVAILABLE.withDescription("Write operations unavailable").asRuntimeException())
            return
        }
        handler.removeCredit(request, response)
    }

    // ── ManagedCredits ────────────────────────────────────────────────────────

    override fun managedCredits(
        request: PageRequest,
        response: StreamObserver<CreditPage>,
    ) {
        val handler = managedCreditReads
        if (handler == null) {
            response.onError(Status.UNAVAILABLE.withDescription("Managed reads unavailable").asRuntimeException())
            return
        }
        handler.managedCredits(request, response)
    }

    // ── WritePerson ───────────────────────────────────────────────────────────

    override fun writePerson(
        request: PersonWrite,
        response: StreamObserver<Outcome>,
    ) {
        val handler = personWrites
        if (handler == null) {
            response.onError(Status.UNAVAILABLE.withDescription("Write operations unavailable").asRuntimeException())
            return
        }
        handler.writePerson(request, response)
    }

    // ── ManagedPerson ─────────────────────────────────────────────────────────

    override fun managedPerson(
        request: Id,
        response: StreamObserver<Person>,
    ) {
        val handler = managedPersonReads
        if (handler == null) {
            response.onError(Status.UNAVAILABLE.withDescription("Managed reads unavailable").asRuntimeException())
            return
        }
        handler.managedPerson(request, response)
    }

    // ── ManagedPeople ─────────────────────────────────────────────────────────

    override fun managedPeople(
        request: PageRequest,
        response: StreamObserver<PersonPage>,
    ) {
        val handler = managedPersonReads
        if (handler == null) {
            response.onError(Status.UNAVAILABLE.withDescription("Managed reads unavailable").asRuntimeException())
            return
        }
        handler.managedPeople(request, response)
    }

    // ── ManagedTitle ─────────────────────────────────────────────────────────

    override fun managedTitle(
        request: Id,
        response: StreamObserver<Title>,
    ) {
        val handler = managedReads
        if (handler == null) {
            response.onError(Status.UNAVAILABLE.withDescription("Managed reads unavailable").asRuntimeException())
            return
        }
        handler.managedTitle(request, response)
    }

    // ── ManagedTitles ─────────────────────────────────────────────────────────

    override fun managedTitles(
        request: PageRequest,
        response: StreamObserver<TitlePage>,
    ) {
        val handler = managedReads
        if (handler == null) {
            response.onError(Status.UNAVAILABLE.withDescription("Managed reads unavailable").asRuntimeException())
            return
        }
        handler.managedTitles(request, response)
    }

    // ── Audit ─────────────────────────────────────────────────────────────────

    override fun audit(
        request: PageRequest,
        response: StreamObserver<AuditPage>,
    ) {
        val handler = managedReads
        if (handler == null) {
            response.onError(Status.UNAVAILABLE.withDescription("Audit reads unavailable").asRuntimeException())
            return
        }
        handler.audit(request, response)
    }

    override fun proposeMovie(
        request: spoolin.v1.Spoolin.Proposal,
        response: StreamObserver<spoolin.v1.Spoolin.ProposalRevision>,
    ) {
        val handler = proposalWrites
        if (handler == null) {
            response.onError(Status.UNAVAILABLE.asRuntimeException())
            return
        }
        handler.propose(request, response)
    }

    override fun approveAndCreate(
        request: spoolin.v1.Spoolin.ApprovalInput,
        response: StreamObserver<Outcome>,
    ) {
        val handler = proposalWrites
        if (handler == null) {
            response.onError(Status.UNAVAILABLE.asRuntimeException())
            return
        }
        handler.approve(request, response)
    }

    override fun rejectProposal(
        request: spoolin.v1.Spoolin.ApprovalInput,
        response: StreamObserver<Outcome>,
    ) {
        val handler = proposalWrites
        if (handler == null) {
            response.onError(Status.UNAVAILABLE.asRuntimeException())
            return
        }
        handler.reject(request, response)
    }

    // ── protobuf mappers ─────────────────────────────────────────────────────

    private fun titleProto(
        row: TitleSearchSql.TitleRow,
        genreIds: List<UUID>,
        provenanceRows: List<ProvenanceReads.ProvenanceRow>,
    ): Title {
        val values =
            Spoolin.MovieValues
                .newBuilder()
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
                }.build()

        return Title
            .newBuilder()
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
            }.build()
    }

    private fun personProto(row: PersonSearch.PersonRow): Person =
        Person
            .newBuilder()
            .setId(row.id.toString())
            .setValues(
                PersonValues
                    .newBuilder()
                    .setName(row.name)
                    .also { b -> row.biography?.let { b.biography = it } }
                    .build(),
            ).setState(row.status)
            .setVersion(row.version)
            .build()

    private fun creditPageProto(
        page: PersonSearch.CreditPage,
        scope: String,
    ): CreditPage {
        val builder = CreditPage.newBuilder()
        page.rows.forEach { r ->
            builder.addNodes(
                Spoolin.Credit
                    .newBuilder()
                    .setId(r.id.toString())
                    .setTitleId(r.titleId.toString())
                    .setPersonId(r.personId.toString())
                    .setRoleId(r.roleId.toString())
                    .addAllCharacters(r.characters)
                    .setVersion(r.version)
                    .setPersonName(r.personName)
                    .setTitleName(r.titleName)
                    .setRole(
                        Spoolin.CreditRole
                            .newBuilder()
                            .setId(r.roleId.toString())
                            .setCategory(r.roleCategory)
                            .setLabel(r.roleLabel)
                            .setAllowsCharacters(r.roleAllowsCharacters)
                            .build(),
                    ).build(),
            )
        }
        page.keyset?.let { builder.endCursor = encodeCreditCursor(it, scope) }
        builder.hasNextPage = page.hasNextPage
        return builder.build()
    }

    private fun partialDateProto(
        year: Int?,
        month: Int?,
        day: Int?,
    ): PartialDate = ProtoHelpers.partialDate(year, month, day)

    private fun provenanceProto(row: ProvenanceReads.ProvenanceRow): Provenance = ProtoHelpers.provenance(row)

    private fun outcomeProto(receipt: ReceiptOutcome): Outcome {
        val b =
            Outcome
                .newBuilder()
                .setOperationId(receipt.id.toString())
                .setState(receipt.state.name)
                .setResultExpired(receipt.resultExpired)
        receipt.result?.let { r ->
            b.resultId = r.resultId.toString()
            r.version?.let { b.version = it }
        }
        receipt.errorCode?.let {
            b.problem =
                Spoolin.Problem
                    .newBuilder()
                    .setCode(it)
                    .build()
        }
        return b.build()
    }

    // ── credit cursor ────────────────────────────────────────────────────────

    /** Scope binds the cursor to a specific target (title or person) to prevent cross-use. */
    private fun creditScope(targetId: UUID, kind: String, first: Int): String =
        ReadPolicy(policy()).cursorScope(
            "public-credits", first, mapOf("kind" to kind, "target" to targetId.toString()),
        )

    private fun pageSize(first: Int): Int = readInput { ReadPolicy(policy()).rpcPageSize(first) }

    private fun searchText(raw: String?): String? = readInput { ReadPolicy(policy()).search(raw) }

    private fun <T> readInput(read: () -> T): T = try {
        read()
    } catch (_: InputViolation) {
        throw Status.INVALID_ARGUMENT.withDescription("FIELD_INVALID").asRuntimeException()
    }

    private fun encodeCreditCursor(
        ks: PersonSearch.CreditKeyset,
        scope: String,
    ): String {
        val keyset = CatalogCursor.Keyset(null, null, null, ks.id)
        return titleCursor.encode(keyset, scope, clock.instant())
    }

    private fun decodeCreditCursor(
        raw: String,
        scope: String,
    ): PersonSearch.CreditKeyset? {
        val ks = titleCursor.decode(raw, scope, clock.instant()) ?: return null
        return PersonSearch.CreditKeyset(ks.id)
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun parseId(value: String): UUID =
        runCatching { UUID.fromString(value).also { require(it.toString() == value) } }
            .getOrElse { throw Status.INVALID_ARGUMENT.withDescription("INVALID_ID").asRuntimeException() }
}
