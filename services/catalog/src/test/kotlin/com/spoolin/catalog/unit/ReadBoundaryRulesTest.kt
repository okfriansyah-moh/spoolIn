package com.spoolin.catalog.unit

import com.spoolin.catalog.features.artwork.ArtworkReads
import com.spoolin.catalog.features.credit.ManagedCreditReads
import com.spoolin.catalog.features.person.read.ManagedPersonReads
import com.spoolin.catalog.features.person.read.PersonSearch
import com.spoolin.catalog.features.provenance.ProvenanceReads
import com.spoolin.catalog.features.taxonomy.read.TaxonomyReads
import com.spoolin.catalog.operations.audit.AuditReads
import io.grpc.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

class ReadBoundaryRulesTest {
    private val id = UUID.fromString("00000000-0000-4000-8000-000000000011")
    private val other = UUID.fromString("00000000-0000-4000-8000-000000000012")
    private val now = Instant.parse("2026-01-01T12:00:00Z")

    private fun person(
        name: String = "Ada",
        biography: String? = "Director",
    ) = mapOf(
        "id" to id,
        "name" to name,
        "biography" to biography,
        "status" to "ARCHIVED",
        "version" to 7L,
    )

    private fun credit() =
        mapOf(
            "id" to id,
            "title_id" to other,
            "person_id" to id,
            "role_id" to other,
            "characters" to arrayOf("Alice", "Narrator"),
            "version" to 3L,
            "person_name" to "Ada",
            "person_status" to "ARCHIVED",
            "title_name" to "River",
            "role_category" to "CAST",
            "role_label" to "Actor",
            "role_allows_characters" to true,
        )

    @Test fun `managed person exposes archived identity and nullable biography`() =
        withMaintainer {
            val result = RpcResult<Spoolin.Person>()
            ManagedPersonReads(JdbcRows(listOf(person(biography = null))).source)
                .managedPerson(
                    Spoolin.Id
                        .newBuilder()
                        .setId(id.toString())
                        .build(),
                    result,
                )
            val value = result.value()
            assertEquals("Ada", value.values.name)
            assertFalse(value.values.hasBiography())
            assertEquals("ARCHIVED", value.state)
            assertEquals(7L, value.version)
        }

    @Test fun `missing managed person returns an empty identity`() =
        withMaintainer {
            val result = RpcResult<Spoolin.Person>()
            ManagedPersonReads(JdbcRows(emptyList()).source)
                .managedPerson(
                    Spoolin.Id
                        .newBuilder()
                        .setId(id.toString())
                        .build(),
                    result,
                )
            assertEquals(Spoolin.Person.getDefaultInstance(), result.value())
        }

    @Test fun `managed people page retains cursor and excludes overflow row`() =
        withMaintainer {
            val db = JdbcRows(listOf(person(), person("Bea")), listOf(person("Bea")))
            val reads = ManagedPersonReads(db.source)
            val first = RpcResult<Spoolin.PersonPage>()
            val request =
                Spoolin.PageRequest
                    .newBuilder()
                    .setState("ARCHIVED")
                    .setFirst(1)
                    .build()
            reads.managedPeople(request, first)
            assertEquals(listOf("Ada"), first.value().nodesList.map { it.values.name })
            assertTrue(first.value().hasNextPage)
            assertFalse(first.value().endCursor.isBlank())
            val second = RpcResult<Spoolin.PersonPage>()
            reads.managedPeople(request.toBuilder().setAfter(first.value().endCursor).build(), second)
            assertEquals(
                "Bea",
                second
                    .value()
                    .nodesList
                    .single()
                    .values.name,
            )
            assertFalse(second.value().hasNextPage)
        }

    @Test fun `managed selectors reject malformed state cursor id and absent actor`() {
        val reads = ManagedPersonReads(JdbcRows().source)
        val denied = RpcResult<Spoolin.Person>()
        reads.managedPerson(Spoolin.Id.getDefaultInstance(), denied)
        assertEquals(Status.Code.PERMISSION_DENIED, Status.fromThrowable(denied.failure).code)
        withMaintainer {
            for (state in listOf("", "DELETED")) {
                val result = RpcResult<Spoolin.PersonPage>()
                reads.managedPeople(
                    Spoolin.PageRequest
                        .newBuilder()
                        .setState(state)
                        .build(),
                    result,
                )
                assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(result.failure).code)
            }
            val invalid = RpcResult<Spoolin.Person>()
            reads.managedPerson(
                Spoolin.Id
                    .newBuilder()
                    .setId("bad")
                    .build(),
                invalid,
            )
            assertEquals("INVALID_ID", Status.fromThrowable(invalid.failure).description)
            val cursor = RpcResult<Spoolin.PersonPage>()
            reads.managedPeople(
                Spoolin.PageRequest
                    .newBuilder()
                    .setState("ACTIVE")
                    .setFirst(10)
                    .setAfter("invalid")
                    .build(),
                cursor,
            )
            assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(cursor.failure).code)
        }
    }

    @Test fun `managed credits preserve character order role and archived person name across pages`() =
        withMaintainer {
            val reads = ManagedCreditReads(JdbcRows(listOf(credit(), credit()), listOf(credit())).source)
            val request =
                Spoolin.PageRequest
                    .newBuilder()
                    .setTargetKind("TITLE")
                    .setTargetId(other.toString())
                    .setFirst(1)
                    .build()
            val first = RpcResult<Spoolin.CreditPage>()
            reads.managedCredits(request, first)
            val value = first.value()
            assertTrue(value.hasNextPage)
            assertEquals(listOf("Alice", "Narrator"), value.nodesList.single().charactersList)
            assertEquals("Ada", value.nodesList.single().personName)
            assertEquals(
                "Actor",
                value.nodesList
                    .single()
                    .role.label,
            )
            assertTrue(
                value.nodesList
                    .single()
                    .role.allowsCharacters,
            )
            val second = RpcResult<Spoolin.CreditPage>()
            reads.managedCredits(request.toBuilder().setAfter(value.endCursor).build(), second)
            assertFalse(second.value().hasNextPage)
        }

    @Test fun `person credit selector and empty result are supported`() =
        withMaintainer {
            val result = RpcResult<Spoolin.CreditPage>()
            ManagedCreditReads(JdbcRows(emptyList()).source).managedCredits(
                Spoolin.PageRequest
                    .newBuilder()
                    .setTargetKind("PERSON")
                    .setTargetId(id.toString())
                    .setFirst(10)
                    .build(),
                result,
            )
            assertEquals(0, result.value().nodesCount)
            assertEquals("", result.value().endCursor)
        }

    @Test fun `managed credit rejects missing unknown and invalid selectors`() =
        withMaintainer {
            for ((kind, target) in listOf("" to "", "UNKNOWN" to id.toString(), "TITLE" to "", "PERSON" to "bad")) {
                val result = RpcResult<Spoolin.CreditPage>()
                ManagedCreditReads(JdbcRows().source).managedCredits(
                    Spoolin.PageRequest
                        .newBuilder()
                        .setTargetKind(kind)
                        .setTargetId(target)
                        .build(),
                    result,
                )
                assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(result.failure).code)
            }
        }

    @Test fun `public people use returned keyset and preserve biography`() {
        val search = PersonSearch(JdbcRows(listOf(person(), person("Bea")), listOf(person(biography = null)), emptyList()).source)
        val first = search.searchPeople("Ada_%", 1, PersonSearch.PersonKeyset("A", other))
        assertTrue(first.hasNextPage)
        assertEquals("Ada", first.rows.single().name)
        assertEquals("Director", first.rows.single().biography)
        assertEquals(id, first.keyset?.id)
        assertNull(search.findPerson(id)?.biography)
        val empty = search.searchPeople(null, 10, null)
        assertTrue(empty.rows.isEmpty())
        assertNull(empty.keyset)
    }

    @Test fun `public title and person credit pages preserve display fields`() {
        val search = PersonSearch(JdbcRows(listOf(credit(), credit()), listOf(credit()), emptyList()).source)
        val first = search.titleCredits(other, 1, PersonSearch.CreditKeyset(other))
        assertTrue(first.hasNextPage)
        assertEquals("River", first.rows.single().titleName)
        assertEquals("CAST", first.rows.single().roleCategory)
        assertEquals(listOf("Alice", "Narrator"), first.rows.single().characters)
        assertEquals(id, first.keyset?.id)
        val person = search.personCredits(id, 5, null)
        assertFalse(person.hasNextPage)
        assertEquals("Ada", person.rows.single().personName)
        assertTrue(search.personCredits(id, 5, null).rows.isEmpty())
    }

    @Test fun `taxonomy projects labels roles and groups year precision without invented months`() {
        val db =
            JdbcRows(
                listOf(mapOf("id" to id, "label" to "Drama")),
                listOf(mapOf("id" to other, "category" to "CREW", "label" to "Director", "allows_characters" to false)),
                listOf(mapOf("id" to id, "label" to "Drama")),
                listOf(mapOf("release_year" to 2024, "release_month" to null), mapOf("release_year" to 2024, "release_month" to 2)),
            )
        val reads = TaxonomyReads(db.source)
        assertEquals("Drama", reads.listGenres().single().label)
        assertFalse(reads.listRoles().single().allowsCharacters)
        val filters = reads.catalogFilterOptions()
        assertEquals(listOf(TaxonomyReads.ReleaseYear(2024, listOf(2))), filters.releaseYears)
        assertEquals(id, filters.genres.single().id)
    }

    @Test fun `provenance projects retrieval time nullable link and local fields`() {
        val row =
            mapOf(
                "source" to "WIKIDATA",
                "external_id" to "Q42",
                "record_url" to null,
                "retrieved_at" to Timestamp.from(now),
                "reuse_note" to "CC0",
                "field_sources" to null,
                "locally_changed" to arrayOf("name"),
                "subject_id" to id,
            )
        val reads = ProvenanceReads(JdbcRows(listOf(row), listOf(row)).source)
        val source = reads.forSubject(id, "PERSON").single()
        assertEquals("Q42", source.externalId)
        assertNull(source.recordUrl)
        assertEquals(now, source.retrievedAt)
        assertEquals("{}", source.fieldSources)
        assertEquals(listOf("name"), source.locallyChanged)
        assertEquals(listOf(source), reads.forSubjects(listOf(id, id), "PERSON")[id])
        assertTrue(reads.forSubjects(emptyList(), "TITLE").isEmpty())
    }

    @Test fun `audit keeps pagination and nullable actor subject and operation`() {
        val row =
            mapOf(
                "id" to id,
                "actor_id" to null,
                "actor_kind" to "SYSTEM",
                "action" to "CREATE_MOVIE",
                "subject_kind" to "TITLE",
                "subject_id" to null,
                "outcome" to "SUCCEEDED",
                "changes" to null,
                "operation_id" to null,
                "correlation_id" to other,
                "created_at" to Timestamp.from(now),
            )
        val reads = AuditReads(JdbcRows(listOf(row, row), listOf(row)).source)
        val first = reads.list(null, 0, null)
        assertTrue(first.hasNextPage)
        assertEquals("{}", first.rows.single().changes)
        assertNull(first.rows.single().actorId)
        assertEquals(now, first.keyset?.createdAt)
        val next = reads.list(id, 10, first.keyset)
        assertFalse(next.hasNextPage)
        assertEquals(other, next.rows.single().correlationId)
    }

    @Test fun `artwork attachment projects selected active hold and released replacements`() {
        for ((state, selected, titleState, expected) in listOf(
            listOf("ATTACHED", id, "ACTIVE", "ACTIVE"),
            listOf("ATTACHED", other, "ACTIVE", "RELEASED"),
            listOf("PREPARING", other, "ACTIVE", "PREPARING"),
            listOf("PREPARING", id, "DELETED", "RELEASED"),
        )) {
            val row =
                mapOf(
                    "surviving_title" to id,
                    "asset_id" to other,
                    "title_id" to id,
                    "actor_id" to other,
                    "selected_version" to 8L,
                    "state" to state,
                    "selected_hold" to selected,
                    "title_status" to titleState,
                )
            val attachment = ArtworkReads(JdbcRows(listOf(row)).source).attachment(id)
            assertEquals(expected, attachment.state)
            assertEquals(8L, attachment.catalogVersion)
            assertEquals(other.toString(), attachment.assetId)
        }
    }

    @Test fun `prepared artwork expires unless retained by a review or pending attachment`() {
        val row = mapOf("expires_at" to Timestamp.from(Instant.parse("2000-01-01T00:00:00Z")), "asset_id" to other, "actor_id" to id)
        assertEquals("RELEASED", ArtworkReads(JdbcRows(emptyList(), listOf(row), emptyList(), emptyList()).source).attachment(id).state)
        assertEquals("PREPARING", ArtworkReads(JdbcRows(emptyList(), listOf(row), listOf(mapOf("one" to 1))).source).attachment(id).state)
        assertEquals(
            "PREPARING",
            ArtworkReads(JdbcRows(emptyList(), listOf(row), emptyList(), listOf(mapOf("one" to 1))).source).attachment(id).state,
        )
        val future = row + ("expires_at" to Timestamp.from(Instant.parse("2100-01-01T00:00:00Z")))
        assertEquals("PREPARING", ArtworkReads(JdbcRows(emptyList(), listOf(future)).source).attachment(id).state)
    }

    @Test fun `artwork access returns selected version and validates management before querying`() {
        val request =
            Spoolin.ArtworkAccessRequest
                .newBuilder()
                .setAssetId(other.toString())
                .build()
        val row = mapOf("id" to id, "attachment_id" to other, "version" to 4L)
        assertEquals(4L, ArtworkReads(JdbcRows(listOf(row)).source).access(request).catalogVersion)
        assertEquals(
            Status.Code.PERMISSION_DENIED,
            assertThrows(io.grpc.StatusRuntimeException::class.java) {
                ArtworkReads(JdbcRows().source).access(request.toBuilder().setManagement(true).build())
            }.status.code,
        )
        withMaintainer {
            assertEquals(
                id.toString(),
                ArtworkReads(JdbcRows(listOf(row)).source).access(request.toBuilder().setManagement(true).build()).titleId,
            )
        }
        assertEquals(
            Status.Code.INVALID_ARGUMENT,
            assertThrows(io.grpc.StatusRuntimeException::class.java) {
                ArtworkReads(JdbcRows().source).access(request.toBuilder().setAssetId("bad").build())
            }.status.code,
        )
        assertEquals(
            Status.Code.NOT_FOUND,
            assertThrows(io.grpc.StatusRuntimeException::class.java) {
                ArtworkReads(JdbcRows(emptyList()).source).access(request)
            }.status.code,
        )
        assertEquals(
            "REFERENCE_VERSION_UNAVAILABLE",
            assertThrows(io.grpc.StatusRuntimeException::class.java) {
                ArtworkReads(JdbcRows(listOf(mapOf("surviving_title" to null))).source).attachment(id)
            }.status.description,
        )
        assertEquals(
            Status.Code.NOT_FOUND,
            assertThrows(io.grpc.StatusRuntimeException::class.java) {
                ArtworkReads(JdbcRows(emptyList(), emptyList()).source).attachment(id)
            }.status.code,
        )
    }
}
