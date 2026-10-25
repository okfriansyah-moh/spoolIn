package com.spoolin.catalog.unit

import com.spoolin.catalog.boundary.grpc.PublicCatalogReads
import com.spoolin.catalog.boundary.grpc.PublicReadContext
import com.spoolin.catalog.boundary.policy.CatalogWritePolicy
import com.spoolin.catalog.features.movie.ManagedMovieReads
import com.spoolin.catalog.features.person.read.PersonSearch
import com.spoolin.catalog.features.provenance.ProvenanceReads
import com.spoolin.catalog.features.search.CatalogCursor
import com.spoolin.catalog.features.search.PeopleCursor
import com.spoolin.catalog.features.search.TitleSearchSql
import com.spoolin.catalog.features.taxonomy.read.TaxonomyReads
import com.spoolin.catalog.operations.audit.AuditReads
import io.grpc.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin
import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class TitleReadRulesTest {
    private val id = UUID.fromString("00000000-0000-4000-8000-000000000011")
    private val other = UUID.fromString("00000000-0000-4000-8000-000000000012")
    private val now = Instant.parse("2026-01-01T12:00:00Z")

    private fun row(full: Boolean = true) =
        mapOf(
            "id" to id,
            "kind" to "TV",
            "status" to "ARCHIVED",
            "display_title" to "River",
            "original_title" to if (full) "Rivière" else null,
            "synopsis" to "Journey",
            "synopsis_language" to "en",
            "original_language" to if (full) "fr" else null,
            "countries" to if (full) arrayOf("FR", "US") else null,
            "release_year" to 2024,
            "release_month" to if (full) 2 else null,
            "release_day" to if (full) 29 else null,
            "release_context" to if (full) "Premiere" else null,
            "runtime_minutes" to if (full) 95 else null,
            "tv_status" to if (full) "ENDED" else null,
            "end_year" to if (full) 2025 else null,
            "end_month" to if (full) 1 else null,
            "end_day" to if (full) 2 else null,
            "asset_id" to if (full) other else null,
            "version" to 7L,
        )

    private fun sources() =
        listOf(
            mapOf(
                "subject_id" to id,
                "source" to "WIKIDATA",
                "external_id" to "Q42",
                "record_url" to "https://www.wikidata.org/wiki/Q42",
                "retrieved_at" to Timestamp.from(now),
                "reuse_note" to "CC0",
                "field_sources" to "{}",
                "locally_changed" to arrayOf("synopsis"),
            ),
        )

    private fun context() =
        PublicReadContext(
            CatalogCursor(ByteArray(32) { 1 }),
            PeopleCursor(ByteArray(32) { 2 }),
            Clock.fixed(now, ZoneOffset.UTC),
        ) { CatalogWritePolicy.environment() }

    private fun public(db: JdbcRows) =
        PublicCatalogReads(
            TitleSearchSql(db.source),
            PersonSearch(db.source),
            TaxonomyReads(db.source),
            ProvenanceReads(db.source),
            context(),
        )

    private fun managed(db: JdbcRows) =
        ManagedMovieReads(db.source, ProvenanceReads(db.source), TitleSearchSql(db.source), AuditReads(db.source))

    @Test fun `public title detail projects complete movie television and provenance fields`() {
        val result = RpcResult<Spoolin.Title>()
        public(JdbcRows(listOf(row()), listOf(mapOf("genre_id" to other)), sources(), emptyList()))
            .getTitle(
                Spoolin.Id
                    .newBuilder()
                    .setId(id.toString())
                    .build(),
                result,
            )
        val value = result.value()
        assertEquals("River", value.values.title)
        assertEquals("Rivière", value.values.originalTitle)
        assertEquals(listOf("FR", "US"), value.values.countriesList)
        assertEquals(29, value.values.release.day)
        assertEquals(95, value.values.runtimeMinutes)
        assertEquals("ENDED", value.tvStatus)
        assertEquals(2025, value.tvEnd.year)
        assertEquals(other.toString(), value.assetId)
        assertEquals("Q42", value.provenanceList.single().externalId)
        assertEquals(listOf("synopsis"), value.provenanceList.single().locallyChangedList)
    }

    @Test fun `public title missing optional fields preserves year only precision`() {
        val result = RpcResult<Spoolin.Title>()
        public(JdbcRows(listOf(row(false)), emptyList(), emptyList(), emptyList()))
            .getTitle(
                Spoolin.Id
                    .newBuilder()
                    .setId(id.toString())
                    .build(),
                result,
            )
        val value = result.value()
        assertFalse(value.values.hasOriginalTitle())
        assertFalse(value.values.release.hasMonth())
        assertFalse(value.values.release.hasDay())
        assertFalse(value.values.hasRuntimeMinutes())
        assertFalse(value.hasAssetId())
        assertEquals(0, value.values.countriesCount)
    }

    @Test fun `public title search produces overflow flag and resumable cursor`() {
        val db =
            JdbcRows(
                listOf(row(), row(false)),
                listOf(mapOf("title_id" to id, "genre_id" to other)),
                sources(),
                emptyList(),
                listOf(row(false)),
                emptyList(),
                emptyList(),
                emptyList(),
            )
        val reads = public(db)
        val request =
            Spoolin.Search
                .newBuilder()
                .setText("River_%")
                .setKind("TV")
                .setYear(2024)
                .setMonth(2)
                .addGenreIds(other.toString())
                .setFirst(1)
                .build()
        val first = RpcResult<Spoolin.TitlePage>()
        reads.searchTitles(request, first)
        assertTrue(first.value().hasNextPage)
        assertEquals(1, first.value().nodesCount)
        val second = RpcResult<Spoolin.TitlePage>()
        reads.searchTitles(request.toBuilder().setAfter(first.value().endCursor).build(), second)
        assertFalse(second.value().hasNextPage)
        assertFalse(
            second
                .value()
                .nodesList
                .single()
                .values
                .hasOriginalTitle(),
        )
    }

    @Test fun `public read absent identity and malformed query errors stay distinct`() {
        val missing = RpcResult<Spoolin.Title>()
        public(JdbcRows(emptyList())).getTitle(
            Spoolin.Id
                .newBuilder()
                .setId(id.toString())
                .build(),
            missing,
        )
        assertEquals("", missing.value().id)
        val invalid = RpcResult<Spoolin.Title>()
        public(JdbcRows()).getTitle(
            Spoolin.Id
                .newBuilder()
                .setId("bad")
                .build(),
            invalid,
        )
        assertEquals("INVALID_ID", Status.fromThrowable(invalid.failure).description)
        for (request in listOf(
            Spoolin.Search
                .newBuilder()
                .setFirst(-1)
                .build(),
            Spoolin.Search
                .newBuilder()
                .setFirst(1)
                .setAfter("invalid")
                .build(),
            Spoolin.Search
                .newBuilder()
                .setFirst(1)
                .addGenreIds("bad")
                .build(),
        )) {
            val result = RpcResult<Spoolin.TitlePage>()
            public(JdbcRows()).searchTitles(request, result)
            assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(result.failure).code)
        }
    }

    @Test fun `managed title exposes retained archive and full provenance`() =
        withMaintainer {
            val result = RpcResult<Spoolin.Title>()
            managed(JdbcRows(listOf(row()), listOf(mapOf("genre_id" to other)), sources(), emptyList()))
                .managedTitle(
                    Spoolin.Id
                        .newBuilder()
                        .setId(id.toString())
                        .build(),
                    result,
                )
            val value = result.value()
            assertEquals("ARCHIVED", value.state)
            assertEquals("TV", value.kind)
            assertEquals("Premiere", value.releaseContext)
            assertEquals("fr", value.values.originalLanguage)
            assertEquals("Q42", value.provenanceList.single().externalId)
        }

    @Test fun `managed title page and cursor handle partial dates and empty continuation`() =
        withMaintainer {
            val db = JdbcRows(listOf(row(false), row()), emptyList(), emptyList(), emptyList(), emptyList())
            val reads = managed(db)
            val request =
                Spoolin.PageRequest
                    .newBuilder()
                    .setState("ARCHIVED")
                    .setFirst(1)
                    .build()
            val first = RpcResult<Spoolin.TitlePage>()
            reads.managedTitles(request, first)
            assertTrue(first.value().hasNextPage)
            assertFalse(
                first
                    .value()
                    .nodesList
                    .single()
                    .values.release
                    .hasMonth(),
            )
            val next = RpcResult<Spoolin.TitlePage>()
            reads.managedTitles(request.toBuilder().setAfter(first.value().endCursor).build(), next)
            assertEquals(0, next.value().nodesCount)
            assertFalse(next.value().hasNextPage)
        }

    @Test fun `managed title rejects absent actor invalid selectors and returns empty for absent id`() {
        val denied = RpcResult<Spoolin.Title>()
        managed(JdbcRows()).managedTitle(Spoolin.Id.getDefaultInstance(), denied)
        assertEquals(Status.Code.PERMISSION_DENIED, Status.fromThrowable(denied.failure).code)
        withMaintainer {
            for (state in listOf("", "DELETED")) {
                val result = RpcResult<Spoolin.TitlePage>()
                managed(JdbcRows()).managedTitles(
                    Spoolin.PageRequest
                        .newBuilder()
                        .setState(state)
                        .build(),
                    result,
                )
                assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(result.failure).code)
            }
            val missing = RpcResult<Spoolin.Title>()
            managed(JdbcRows(emptyList())).managedTitle(
                Spoolin.Id
                    .newBuilder()
                    .setId(id.toString())
                    .build(),
                missing,
            )
            assertEquals("", missing.value().id)
        }
    }

    @Test fun `managed audit projects safe actor references and decoded field changes`() =
        withMaintainer {
            val audit =
                mapOf(
                    "id" to id,
                    "actor_id" to other,
                    "actor_kind" to "HUMAN",
                    "action" to "UPDATE_MOVIE",
                    "subject_kind" to "TITLE",
                    "subject_id" to id,
                    "outcome" to "SUCCEEDED",
                    "operation_id" to other,
                    "correlation_id" to other,
                    "created_at" to Timestamp.from(now),
                    "changes" to """{"changes":[{"path":"title","before":"Old","after":"New"}],"summarised":false}""",
                )
            val reads = managed(JdbcRows(listOf(audit), emptyList()))
            val result = RpcResult<Spoolin.AuditPage>()
            val request =
                Spoolin.PageRequest
                    .newBuilder()
                    .setFirst(10)
                    .setTargetId(id.toString())
                    .build()
            reads.audit(request, result)
            val value = result.value()
            assertEquals(other.toString(), value.nodesList.single().actorRef)
            assertEquals("UPDATE_MOVIE", value.nodesList.single().action)
            assertEquals(
                now.epochSecond,
                value.nodesList
                    .single()
                    .at.seconds,
            )
            assertFalse(value.endCursor.isBlank())
            val next = RpcResult<Spoolin.AuditPage>()
            reads.audit(request.toBuilder().setAfter(value.endCursor).build(), next)
            assertEquals(0, next.value().nodesCount)
        }
}
