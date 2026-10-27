package com.spoolin.catalog.unit

import com.google.protobuf.Empty
import com.spoolin.catalog.boundary.grpc.CatalogServiceRpc
import com.spoolin.catalog.features.person.read.PersonSearch
import com.spoolin.catalog.features.provenance.ProvenanceReads
import com.spoolin.catalog.features.search.CatalogCursor
import com.spoolin.catalog.features.search.PeopleCursor
import com.spoolin.catalog.features.search.TitleSearchSql
import com.spoolin.catalog.features.taxonomy.read.TaxonomyReads
import com.spoolin.catalog.operations.receipts.OwnerReceiptReads
import com.spoolin.support.receipts.ReceiptAuthority
import io.grpc.Status
import io.grpc.stub.StreamObserver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin

class CatalogFacadeRulesTest {
    private fun rpc(
        db: JdbcRows,
        scope: String? = null,
    ) = CatalogServiceRpc(
        TitleSearchSql(db.source),
        PersonSearch(db.source),
        TaxonomyReads(db.source),
        ProvenanceReads(db.source),
        OwnerReceiptReads(db.source, unitClock),
        ReceiptAuthority { scope },
        CatalogCursor(ByteArray(32) { 1 }),
        PeopleCursor(ByteArray(32) { 2 }),
        unitClock,
    )

    private fun <T> unavailable(call: (StreamObserver<T>) -> Unit) {
        val result = RpcResult<T>()
        call(result)
        assertEquals(Status.Code.UNAVAILABLE, Status.fromThrowable(result.failure).code)
        assertFalse(result.completed)
        assertTrue(result.values.isEmpty())
    }

    @Test fun `missing optional write and managed handlers fail closed without success payloads`() {
        val rpc = rpc(JdbcRows())
        unavailable<Spoolin.Outcome> { rpc.writeMovie(Spoolin.MovieWrite.getDefaultInstance(), it) }
        unavailable<Spoolin.Outcome> { rpc.writePerson(Spoolin.PersonWrite.getDefaultInstance(), it) }
        unavailable<Spoolin.Outcome> { rpc.writeCredit(Spoolin.CreditWrite.getDefaultInstance(), it) }
        unavailable<Spoolin.Outcome> { rpc.removeCredit(Spoolin.VersionedId.getDefaultInstance(), it) }
        unavailable<Spoolin.Outcome> { rpc.createRole(Spoolin.RoleWrite.getDefaultInstance(), it) }
        unavailable<Spoolin.Outcome> { rpc.movieLifecycle(Spoolin.Lifecycle.getDefaultInstance(), it) }
        unavailable<Spoolin.Outcome> { rpc.personLifecycle(Spoolin.Lifecycle.getDefaultInstance(), it) }
        unavailable<Spoolin.Impact> { rpc.deletionImpact(Spoolin.Id.getDefaultInstance(), it) }
        unavailable<Spoolin.Title> { rpc.managedTitle(Spoolin.Id.getDefaultInstance(), it) }
        unavailable<Spoolin.TitlePage> { rpc.managedTitles(Spoolin.PageRequest.getDefaultInstance(), it) }
        unavailable<Spoolin.Person> { rpc.managedPerson(Spoolin.Id.getDefaultInstance(), it) }
        unavailable<Spoolin.PersonPage> { rpc.managedPeople(Spoolin.PageRequest.getDefaultInstance(), it) }
        unavailable<Spoolin.CreditPage> { rpc.managedCredits(Spoolin.PageRequest.getDefaultInstance(), it) }
        unavailable<Spoolin.AuditPage> { rpc.audit(Spoolin.PageRequest.getDefaultInstance(), it) }
        unavailable<Spoolin.Outcome> { rpc.beginArtwork(Spoolin.Hold.getDefaultInstance(), it) }
        unavailable<Spoolin.ArtworkCancelOutcome> { rpc.cancelArtwork(Spoolin.CancelArtworkRequest.getDefaultInstance(), it) }
        unavailable<Spoolin.Attachment> { rpc.getAttachment(Spoolin.Id.getDefaultInstance(), it) }
        unavailable<Spoolin.ArtworkGrant> { rpc.resolveArtworkAccess(Spoolin.ArtworkAccessRequest.getDefaultInstance(), it) }
        unavailable<Spoolin.ProposalRevision> { rpc.proposeMovie(Spoolin.Proposal.getDefaultInstance(), it) }
        unavailable<Spoolin.ProposalRevision> { rpc.getMovieProposal(Spoolin.MovieProposalRequest.getDefaultInstance(), it) }
        unavailable<Spoolin.MovieProposalQueue> { rpc.listMovieProposals(Spoolin.MovieProposalQueueRequest.getDefaultInstance(), it) }
        unavailable<Spoolin.PreparedPoster> { rpc.prepareMoviePoster(Spoolin.PrepareMoviePosterRequest.getDefaultInstance(), it) }
        unavailable<Spoolin.MoviePosterImage> { rpc.readMoviePoster(Spoolin.MoviePosterReadRequest.getDefaultInstance(), it) }
        unavailable<Spoolin.Outcome> { rpc.approveAndCreate(Spoolin.ApprovalInput.getDefaultInstance(), it) }
        unavailable<Spoolin.Outcome> { rpc.rejectProposal(Spoolin.ApprovalInput.getDefaultInstance(), it) }
    }

    @Test fun `receipt lookup requires scope returns missing status and projects safe completed receipt`() {
        val request =
            Spoolin.Id
                .newBuilder()
                .setId(unitOperation.toString())
                .build()
        val denied = RpcResult<Spoolin.Outcome>()
        rpc(JdbcRows()).getOperation(request, denied)
        assertEquals(Status.Code.PERMISSION_DENIED, Status.fromThrowable(denied.failure).code)
        val absent = RpcResult<Spoolin.Outcome>()
        rpc(JdbcRows(emptyList()), "account:$unitActor").getOperation(request, absent)
        assertEquals(Status.Code.NOT_FOUND, Status.fromThrowable(absent.failure).code)
        val intent =
            com.spoolin.support.receipts
                .OperationIntent(unitOperation, "account:$unitActor", "WriteMovie", "policy", emptyList())
        val row = receiptRow(intent, "SUCCEEDED", """{"resultId":"$unitTarget","version":"4"}""")
        val result = RpcResult<Spoolin.Outcome>()
        rpc(JdbcRows(listOf(row)), "account:$unitActor").getOperation(request, result)
        assertEquals("SUCCEEDED", result.value().state)
        assertEquals(4L, result.value().version)
        assertEquals(unitTarget.toString(), result.value().resultId)
        val invalid = RpcResult<Spoolin.Outcome>()
        rpc(JdbcRows(), "account:$unitActor").getOperation(
            Spoolin.Id
                .newBuilder()
                .setId("bad")
                .build(),
            invalid,
        )
        assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(invalid.failure).code)
    }

    @Test fun `public people route exposes names biographies pagination and validates incompatible filters`() {
        val person = mapOf("id" to unitTarget, "name" to "Ada", "biography" to "Biography", "status" to "ACTIVE", "version" to 2L)
        val rpc = rpc(JdbcRows(listOf(person, person), listOf(person), listOf(person), emptyList()))
        val first = RpcResult<Spoolin.PersonPage>()
        val request =
            Spoolin.Search
                .newBuilder()
                .setFirst(1)
                .setText("Ada")
                .build()
        rpc.searchPeople(request, first)
        assertTrue(first.value().hasNextPage)
        assertEquals(
            "Ada",
            first
                .value()
                .nodesList
                .single()
                .values.name,
        )
        val next = RpcResult<Spoolin.PersonPage>()
        rpc.searchPeople(request.toBuilder().setAfter(first.value().endCursor).build(), next)
        assertFalse(next.value().hasNextPage)
        val detail = RpcResult<Spoolin.Person>()
        rpc.getPerson(
            Spoolin.Id
                .newBuilder()
                .setId(unitTarget.toString())
                .build(),
            detail,
        )
        assertEquals("Biography", detail.value().values.biography)
        val missing = RpcResult<Spoolin.Person>()
        rpc.getPerson(
            Spoolin.Id
                .newBuilder()
                .setId(unitAsset.toString())
                .build(),
            missing,
        )
        assertEquals("", missing.value().id)
        for (invalid in listOf(request.toBuilder().setKind("MOVIE").build(), request.toBuilder().setAfter("bad").build())) {
            val result = RpcResult<Spoolin.PersonPage>()
            rpc.searchPeople(invalid, result)
            assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(result.failure).code)
        }
    }

    @Test fun `public credit routes project full role metadata and bind cursors to target`() {
        val credit =
            mapOf(
                "id" to unitTarget,
                "title_id" to unitTarget,
                "person_id" to unitActor,
                "role_id" to unitAsset,
                "characters" to arrayOf("Alice"),
                "version" to 2L,
                "person_name" to "Ada",
                "title_name" to "River",
                "role_category" to "CAST",
                "role_label" to "Actor",
                "role_allows_characters" to true,
            )
        val rpc = rpc(JdbcRows(listOf(credit, credit), listOf(credit), emptyList()))
        val request =
            Spoolin.PageRequest
                .newBuilder()
                .setTargetId(unitTarget.toString())
                .setFirst(1)
                .build()
        val first = RpcResult<Spoolin.CreditPage>()
        rpc.titleCredits(request, first)
        assertTrue(first.value().hasNextPage)
        assertEquals(
            "Actor",
            first
                .value()
                .nodesList
                .single()
                .role.label,
        )
        val next = RpcResult<Spoolin.CreditPage>()
        rpc.titleCredits(request.toBuilder().setAfter(first.value().endCursor).build(), next)
        assertFalse(next.value().hasNextPage)
        val swapped = RpcResult<Spoolin.CreditPage>()
        rpc.personCredits(request.toBuilder().setAfter(first.value().endCursor).build(), swapped)
        assertEquals("INVALID_CURSOR", Status.fromThrowable(swapped.failure).description)
        val person = RpcResult<Spoolin.CreditPage>()
        rpc.personCredits(request, person)
        assertEquals(0, person.value().nodesCount)
    }

    @Test fun `public taxonomy route returns typed roles genres and honest release precision`() {
        val genre = listOf(mapOf("id" to unitTarget, "label" to "Drama"))
        val role = listOf(mapOf("id" to unitAsset, "label" to "Actor", "category" to "CAST", "allows_characters" to true))
        val rpc = rpc(JdbcRows(genre, role, genre, listOf(mapOf("release_year" to 2024, "release_month" to 2))))
        val genres = RpcResult<Spoolin.Genres>()
        rpc.listGenres(Empty.getDefaultInstance(), genres)
        assertEquals(
            "Drama",
            genres
                .value()
                .nodesList
                .single()
                .label,
        )
        val roles = RpcResult<Spoolin.Roles>()
        rpc.listRoles(Empty.getDefaultInstance(), roles)
        assertEquals(
            "CAST",
            roles
                .value()
                .nodesList
                .single()
                .category,
        )
        val filters = RpcResult<Spoolin.CatalogFilterOptions>()
        rpc.getCatalogFilterOptions(Empty.getDefaultInstance(), filters)
        assertEquals(
            2024,
            filters
                .value()
                .releaseYearsList
                .single()
                .year,
        )
        assertEquals(
            listOf(2),
            filters
                .value()
                .releaseYearsList
                .single()
                .monthsList,
        )
    }
}
