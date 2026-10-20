@file:Suppress("TooManyFunctions", "LongParameterList", "CyclomaticComplexMethod", "NestedBlockDepth")

package com.spoolin.catalog.boundary.grpc

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
import com.spoolin.support.policy.PolicyEnvironment
import com.spoolin.support.policy.PublicPolicy
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
import spoolin.v1.Spoolin.Person
import spoolin.v1.Spoolin.PersonPage
import spoolin.v1.Spoolin.PersonWrite
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
    titleSearch: TitleSearchSql,
    personSearch: PersonSearch,
    taxonomy: TaxonomyReads,
    provenance: ProvenanceReads,
    private val receiptReads: OwnerReceiptReads,
    private val receiptAuthority: ReceiptAuthority,
    titleCursor: CatalogCursor,
    peopleCursor: PeopleCursor,
    clock: Clock = Clock.systemUTC(),
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
    policy: () -> PublicPolicy = { PolicyEnvironment.resolve(System::getenv) },
) : CatalogServiceGrpc.CatalogServiceImplBase() {
    private val publicReads =
        PublicCatalogReads(
            titleSearch,
            personSearch,
            taxonomy,
            provenance,
            PublicReadContext(titleCursor, peopleCursor, clock, policy),
        )

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
    ) = publicReads.searchTitles(request, response)

    // ── GetTitle ─────────────────────────────────────────────────────────────

    override fun getTitle(
        request: Id,
        response: StreamObserver<Title>,
    ) = publicReads.getTitle(request, response)

    // ── SearchPeople ─────────────────────────────────────────────────────────

    override fun searchPeople(
        request: Search,
        response: StreamObserver<PersonPage>,
    ) = publicReads.searchPeople(request, response)

    // ── GetPerson ────────────────────────────────────────────────────────────

    override fun getPerson(
        request: Id,
        response: StreamObserver<Person>,
    ) = publicReads.getPerson(request, response)

    // ── TitleCredits ─────────────────────────────────────────────────────────

    override fun titleCredits(
        request: PageRequest,
        response: StreamObserver<CreditPage>,
    ) = publicReads.titleCredits(request, response)

    // ── PersonCredits ────────────────────────────────────────────────────────

    override fun personCredits(
        request: PageRequest,
        response: StreamObserver<CreditPage>,
    ) = publicReads.personCredits(request, response)

    // ── ListGenres ───────────────────────────────────────────────────────────

    override fun getCatalogFilterOptions(
        request: com.google.protobuf.Empty,
        response: StreamObserver<Spoolin.CatalogFilterOptions>,
    ) = publicReads.catalogFilterOptions(response)

    override fun listGenres(
        request: com.google.protobuf.Empty,
        response: StreamObserver<Genres>,
    ) = publicReads.listGenres(response)

    // ── ListRoles ────────────────────────────────────────────────────────────

    override fun listRoles(
        request: com.google.protobuf.Empty,
        response: StreamObserver<Roles>,
    ) = publicReads.listRoles(response)

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

    override fun prepareMoviePoster(request: spoolin.v1.Spoolin.PrepareMoviePosterRequest, response: StreamObserver<spoolin.v1.Spoolin.PreparedPoster>) {
        proposalWrites?.preparePoster(request,response) ?: response.onError(Status.UNAVAILABLE.asRuntimeException())
    }
    override fun getMovieProposal(request: spoolin.v1.Spoolin.MovieProposalRequest, response: StreamObserver<spoolin.v1.Spoolin.ProposalRevision>) {
        proposalWrites?.read(request,response) ?: response.onError(Status.UNAVAILABLE.asRuntimeException())
    }
    override fun readMoviePoster(request: spoolin.v1.Spoolin.MoviePosterReadRequest, response: StreamObserver<spoolin.v1.Spoolin.MoviePosterImage>) {
        proposalWrites?.readPoster(request,response) ?: response.onError(Status.UNAVAILABLE.asRuntimeException())
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

    private fun parseId(value: String): UUID =
        runCatching { UUID.fromString(value).also { require(it.toString() == value) } }
            .getOrElse { throw Status.INVALID_ARGUMENT.withDescription("INVALID_ID").asRuntimeException() }
}
