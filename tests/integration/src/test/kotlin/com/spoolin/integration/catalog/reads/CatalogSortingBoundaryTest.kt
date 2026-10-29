package com.spoolin.integration.catalog.reads

import com.spoolin.support.receipts.executeBound
import io.grpc.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.Spoolin.Search
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

class CatalogSortingBoundaryTest {
    @Test
    fun `six orders traverse real SQL and transport with precision Unicode and timestamp ties`() {
        PublicCatalogReadFixture().use { fixture ->
            seed(fixture)
            val stub = CatalogServiceGrpc.newBlockingStub(fixture.caller)
            val expected = mapOf(
                "RELEASE_DESC" to listOf(0, 5, 1, 6, 7, 2, 3, 4),
                "RELEASE_ASC" to listOf(3, 6, 7, 0, 5, 1, 2, 4),
                "TITLE_ASC" to (0..7).toList(),
                "TITLE_DESC" to listOf(6, 7, 5, 4, 2, 3, 0, 1),
                "ADDED_ASC" to listOf(1, 5, 4, 0, 2, 3, 6, 7),
                "ADDED_DESC" to listOf(6, 7, 2, 3, 0, 4, 5, 1),
            )
            expected.forEach { (sort, indexes) ->
                assertEquals(indexes.map(::id), traverse(stub, sort), sort)
            }
            assertEquals(traverse(stub, "RELEASE_DESC"), traverse(stub, null))
            assertGraphqlAndCursorBinding(fixture, stub)
        }
    }

    @Test
    fun `oversized legacy titles paginate and changed archived or deleted anchors require restart`() {
        PublicCatalogReadFixture().use { fixture ->
            val anchor = UUID(0, 201)
            val original = "Oversized fixture " + "A".repeat(1000)
            fixture.pool.connection.use { connection ->
                listOf(anchor to original, UUID(0, 202) to "Oversized fixture Z").forEach { (key, title) ->
                    connection.executeBound(
                        "INSERT INTO title(id,kind,display_title,synopsis,synopsis_language) VALUES(?,'MOVIE',?,'Synthetic','en')",
                        key, title,
                    )
                }
            }
            val stub = CatalogServiceGrpc.newBlockingStub(fixture.caller)
            val request = Search.newBuilder().setText("Oversized fixture ").setSort("TITLE_ASC").setFirst(1)
            val first = stub.searchTitles(request.build())
            request.after = first.endCursor
            assertEquals(UUID(0, 202).toString(), stub.searchTitles(request.build()).nodesList.single().id)
            fixture.pool.connection.use { connection ->
                connection.executeBound("UPDATE title SET display_title=? WHERE id=?", "Changed title", anchor)
                assertThrows(io.grpc.StatusRuntimeException::class.java) { stub.searchTitles(request.build()) }
                connection.executeBound("UPDATE title SET display_title=?,status='ARCHIVED' WHERE id=?", original, anchor)
                assertThrows(io.grpc.StatusRuntimeException::class.java) { stub.searchTitles(request.build()) }
                connection.executeBound("DELETE FROM title WHERE id=?", anchor)
                assertThrows(io.grpc.StatusRuntimeException::class.java) { stub.searchTitles(request.build()) }
            }
        }
    }

    private fun traverse(stub: CatalogServiceGrpc.CatalogServiceBlockingStub, sort: String?): List<String> {
        val request = Search.newBuilder().setText("Sort fixture ").setFirst(2)
        sort?.let(request::setSort)
        val ids = mutableListOf<String>()
        repeat(5) {
            val page = stub.searchTitles(request.build())
            ids.addAll(page.nodesList.map { row -> row.id })
            if (!page.hasNextPage) return ids
            request.after = page.endCursor
        }
        error("Traversal did not terminate")
    }

    private fun assertGraphqlAndCursorBinding(fixture: PublicCatalogReadFixture, stub: CatalogServiceGrpc.CatalogServiceBlockingStub) {
        val base = Search.newBuilder().setText("Sort fixture ").setFirst(2)
        val legacy = stub.searchTitles(base.build())
        assertEquals(2, stub.searchTitles(base.setAfter(legacy.endCursor).setSort("RELEASE_DESC").build()).nodesCount)
        val failure = assertThrows(io.grpc.StatusRuntimeException::class.java) {
            stub.searchTitles(base.setSort("TITLE_ASC").build())
        }
        assertEquals(Status.Code.INVALID_ARGUMENT, failure.status.code)
        assertThrows(io.grpc.StatusRuntimeException::class.java) {
            stub.searchTitles(Search.newBuilder().setSort("bad").build())
        }
        val response = fixture.graphql("""{ titles(filter:{text:"Sort fixture ",sort:ADDED_ASC},first:2) { nodes { id } } }""")
        val data = response["data"] as Map<*, *>
        val connection = data["titles"] as Map<*, *>
        assertEquals(listOf(id(1), id(5)), (connection["nodes"] as List<*>).map { (it as Map<*, *>)["id"] })
    }

    private fun seed(fixture: PublicCatalogReadFixture) {
        val names = listOf("Alpha", "alpha", "ß", "ss", "Zebra", "Éclair", "σ", "Σ")
        val dates = listOf(listOf(2024, 10, 9), listOf(2024, 10, null), listOf(2024, null, null),
            listOf(2023, null, null), listOf(null, null, null), listOf(2024, 10, 9), listOf(2024, 1, 2), listOf(2024, 1, 2))
        val added = listOf(3L, 0L, 4L, 4L, 2L, 1L, 5L, 5L)
        fixture.pool.connection.use { connection ->
            names.forEachIndexed { index, name ->
                connection.executeBound(
                    "INSERT INTO title(id,kind,display_title,synopsis,synopsis_language,release_year,release_month,release_day,created_at) " +
                        "VALUES(?,'MOVIE',?,'Synthetic sorting fixture','en',?,?,?,?)",
                    UUID.fromString(id(index)), "Sort fixture $name", dates[index][0], dates[index][1], dates[index][2],
                    Timestamp.from(Instant.parse("2026-01-01T00:00:00Z").plusNanos(added[index] * 1000)),
                )
            }
        }
    }

    private fun id(index: Int): String = UUID(0, index.toLong() + 101).toString()
}
