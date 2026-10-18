package com.spoolin.integration.catalog.reads

import com.spoolin.catalog.features.provenance.ProvenanceReads
import com.spoolin.catalog.features.search.TitleSearchSql
import com.spoolin.integration.identity.SignedReceiptFixture
import com.spoolin.support.receipts.committed
import com.spoolin.support.receipts.executeBound
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.Spoolin.PageRequest
import spoolin.v1.Spoolin.Search
import spoolin.v1.Spoolin.TitlePage
import java.util.UUID

class CatalogPageQueryBoundaryTest {
    @Test
    fun `public and managed pages execute three real queries independent of returned title count`() {
        SignedReceiptFixture("catalog-page-query-boundary.json").use { identity ->
            lateinit var observed: CountedCatalogDataSource
            PublicCatalogReadFixture(
                proof = identity,
                observe = { CountedCatalogDataSource(it).also { observed = it } },
            ).use { fixture ->
                val ids = seedPage(fixture)
                val stub = CatalogServiceGrpc.newBlockingStub(fixture.caller)
                observed.completed.clear()
                assertEquals(1, stub.searchTitles(Search.newBuilder().setFirst(1).build()).nodesCount)
                assertEquals(3, observed.completed.size)
                observed.completed.clear()
                val public = stub.searchTitles(Search.newBuilder().setFirst(20).build())
                assertPage(public, ids, fixture.genre)
                assertEquals(3, observed.completed.size)
                observed.completed.clear()
                val managed =
                    fixture.managed("ManagedTitles").managedTitles(
                        PageRequest
                            .newBuilder()
                            .setFirst(20)
                            .setState("ACTIVE")
                            .build(),
                    )
                assertEquals(public.nodesList, managed.nodesList)
                assertEquals(3, observed.completed.size)
                observed.completed.clear()
                val empty = stub.searchTitles(Search.newBuilder().setText("absent-query-needle").build())
                assertEquals(0, empty.nodesCount)
                assertEquals(1, observed.completed.size)
            }
        }
    }

    @Test
    fun `batch helpers bind at most one hundred IDs and issue no empty selection queries`() {
        PublicCatalogReadFixture().use { fixture ->
            val observed = CountedCatalogDataSource(fixture.pool)
            val titles = TitleSearchSql(observed)
            val provenance = ProvenanceReads(observed)
            assertTrue(titles.findGenresForTitles(emptyList()).isEmpty())
            assertTrue(provenance.forSubjects(emptyList(), "TITLE").isEmpty())
            assertTrue(observed.completed.isEmpty())
            val ids = listOf(fixture.title) + List(200) { UUID.randomUUID() }
            assertEquals(listOf(fixture.genre), titles.findGenresForTitles(ids).getValue(fixture.title))
            assertEquals(3, observed.completed.size)
            observed.completed.clear()
            assertEquals(1, provenance.forSubjects(ids, "TITLE").getValue(fixture.title).size)
            assertEquals(3, observed.completed.size)
            assertTrue(observed.completed.all { sql -> sql.count { it == '?' } <= 101 })
        }
    }

    private fun seedPage(fixture: PublicCatalogReadFixture): List<UUID> {
        val ids = listOf(fixture.title) + List(19) { UUID.randomUUID() }
        fixture.pool.committed { connection ->
            ids.drop(1).forEachIndexed { index, id ->
                connection.executeBound(
                    "INSERT INTO title(id,kind,display_title,synopsis,synopsis_language,release_year) " +
                        "VALUES(?,'MOVIE',?,'Synthetic synopsis','en',?)",
                    id,
                    "Page title $index",
                    2024 - index,
                )
            }
            ids.forEach { id ->
                connection.executeBound(
                    "INSERT INTO title_genre(title_id,genre_id) VALUES(?,?) ON CONFLICT DO NOTHING",
                    id,
                    fixture.genre,
                )
                addSource(connection, id, "a-first", false)
                addSource(connection, id, "z-last", false)
                addSource(connection, id, "deleted", true)
            }
        }
        return ids
    }

    private fun addSource(
        connection: java.sql.Connection,
        id: UUID,
        source: String,
        deleted: Boolean,
    ) {
        connection.executeBound(
            "INSERT INTO provenance_record(source,external_id,subject_kind,subject_id,retrieved_at," +
                "reuse_note,field_sources,deleted_at) VALUES(?,?,'TITLE',?,'2025-01-01T00:00:00Z'," +
                "'Synthetic permission','{}'::jsonb,CASE WHEN ? THEN CURRENT_TIMESTAMP ELSE NULL END)",
            source,
            id.toString(),
            id,
            deleted,
        )
    }

    private fun assertPage(
        page: TitlePage,
        ids: List<UUID>,
        genre: UUID,
    ) {
        assertEquals(ids.map(UUID::toString), page.nodesList.map { it.id })
        assertFalse(page.hasNextPage)
        page.nodesList.forEachIndexed { index, title ->
            assertEquals(listOf(genre.toString()), title.values.genreIdsList)
            val expected =
                if (index == 0) listOf("a-first", "synthetic-test", "z-last") else listOf("a-first", "z-last")
            assertEquals(expected, title.provenanceList.map { it.source })
            assertTrue(title.provenanceList.all { it.externalId.isNotEmpty() && it.reuseNote.isNotEmpty() })
        }
    }
}
