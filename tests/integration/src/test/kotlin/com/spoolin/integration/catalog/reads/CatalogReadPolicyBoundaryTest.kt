package com.spoolin.integration.catalog.reads

import com.spoolin.integration.identity.SignedReceiptFixture
import com.spoolin.support.policy.ConfigurationSources
import com.spoolin.support.policy.PolicySettings
import com.spoolin.support.policy.PublicPolicy
import com.spoolin.support.receipts.committed
import com.spoolin.support.receipts.executeBound
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.Spoolin.PageRequest
import spoolin.v1.Spoolin.Search
import java.util.UUID

/** Actual generated GraphQL, mTLS owner handlers and separately committed PostgreSQL fixtures. */
class CatalogReadPolicyBoundaryTest {
    @Test
    fun `public RPC enforces effective search and page bounds and rejects stale-policy cursors`() {
        var policy = configured()
        PublicCatalogReadFixture({ policy }).use { fixture ->
            addTitle(fixture, "Another", 2024, 1, 1)
            val stub = CatalogServiceGrpc.newBlockingStub(fixture.caller)
            val first = stub.searchTitles(Search.getDefaultInstance())
            assertEquals(1, first.nodesCount)
            assertTrue(first.hasNextPage)
            assertEquals(2, stub.searchTitles(Search.newBuilder().setFirst(2).build()).nodesCount)
            listOf(-1, 3).forEach { size -> invalid { stub.searchTitles(Search.newBuilder().setFirst(size).build()) } }
            listOf("x", "12345").forEach { text ->
                invalid { stub.searchTitles(Search.newBuilder().setText(text).build()) }
                invalid { stub.searchPeople(Search.newBuilder().setText(text).build()) }
            }
            assertEquals(1, stub.searchTitles(Search.newBuilder().setText(" Bo ").build()).nodesCount)
            assertEquals(1, stub.searchPeople(Search.newBuilder().setText(" Bo ").build()).nodesCount)
            assertEquals(1, stub.searchTitles(Search.newBuilder().setText(" \u00a0 ").build()).nodesCount)
            stub.searchTitles(Search.newBuilder().setText("A\uD83D\uDE00BC").build())
            invalid { stub.searchPeople(Search.newBuilder().setKind("").build()) }
            invalid { stub.searchPeople(Search.newBuilder().setAfter(first.endCursor).build()) }
            policy = configured("SEARCH_MAX_CODE_POINTS" to "3")
            invalid { stub.searchTitles(Search.newBuilder().setAfter(first.endCursor).build()) }
        }
    }

    @Test
    fun `full partial and unknown dates paginate once each using exact bound keyset parameters`() {
        PublicCatalogReadFixture({ configured() }).use { fixture ->
            val full = addTitle(fixture, "Full", 2025, 2, 5)
            val month = addTitle(fixture, "Month", 2025, 2, null)
            val earlier = addTitle(fixture, "Earlier", 2025, 1, 1)
            val unknown = addTitle(fixture, "Unknown", null, null, null)
            val stub = CatalogServiceGrpc.newBlockingStub(fixture.caller)
            val seen = mutableListOf<String>()
            var after: String? = null
            repeat(5) {
                val request =
                    Search
                        .newBuilder()
                        .setFirst(1)
                        .also { b -> after?.let { b.after = it } }
                        .build()
                val page = stub.searchTitles(request)
                seen.add(page.nodesList.single().id)
                after = page.endCursor
                assertEquals(it < 4, page.hasNextPage)
            }
            assertEquals(listOf(full, month, earlier, fixture.title, unknown).map(UUID::toString), seen)
        }
    }

    @Test
    fun `actual Core GraphQL coercion applies custom default and preserves safe invalid cursor errors`() {
        PublicCatalogReadFixture({ configured() }).use { fixture ->
            addTitle(fixture, "Second", 2024, 1, 1)
            val default = fixture.graphql("{ titles(filter: {}) { nodes { id } } }")
            val explicit = fixture.graphql("{ titles(filter: {}, first: 2) { nodes { id } } }")
            assertEquals(1, titleIds(default).size)
            assertEquals(2, titleIds(explicit).size)
            val rejected = fixture.graphql("""{ titles(filter: {}, after: "invalid") { nodes { id } } }""")
            assertTrue(rejected.toString().contains("FIELD_INVALID"))
            assertTrue(!rejected.toString().contains("INVALID_CURSOR"))
            assertTrue(fixture.graphql("{ titles(filter: {}, first: 3) { nodes { id } } }").containsKey("errors"))
        }
    }

    @Test
    fun `managed signed cursors bind state policy session and current live role over mTLS`() {
        SignedReceiptFixture("catalog-managed-read-policy.json").use { identity ->
            var policy = configured()
            PublicCatalogReadFixture({ policy }, identity).use { fixture ->
                addTitle(fixture, "Earlier", 2024, 1, 1)
                val first = fixture.managed("ManagedTitles").managedTitles(page())
                assertTrue(first.hasNextPage)
                val next = fixture.managed("ManagedTitles").managedTitles(page(first.endCursor))
                assertNotEquals(first.nodesList.single().id, next.nodesList.single().id)
                invalid { fixture.managed("ManagedTitles").managedTitles(page(first.endCursor, "ARCHIVED")) }
                val session =
                    identity.fixture.pool.committed {
                        identity.sessions.issue(it, identity.account, 0, null)
                    }
                invalid { fixture.managed("ManagedTitles", session.principal.id).managedTitles(page(first.endCursor)) }
                policy = configured("SEARCH_MAX_CODE_POINTS" to "3")
                invalid { fixture.managed("ManagedTitles").managedTitles(page(first.endCursor)) }
                identity.fixture.pool.committed {
                    identity.sessions.revoke(it, identity.issued.principal.id, identity.account)
                }
                assertEquals(
                    Status.Code.PERMISSION_DENIED,
                    assertThrows(StatusRuntimeException::class.java) {
                        fixture.managed("ManagedTitles").managedTitles(page())
                    }.status.code,
                )
            }
        }
    }

    @Test
    fun `managed people preserve escaped names and reject unsigned or cross-state cursors`() {
        SignedReceiptFixture("catalog-managed-person-cursor.json").use { identity ->
            PublicCatalogReadFixture({ configured() }, identity).use { fixture ->
                val name = "A\\"
                val id = UUID.randomUUID()
                fixture.pool.committed { it.executeBound("INSERT INTO person(id,name) VALUES(?,?)", id, name) }
                val first = fixture.managed("ManagedPeople").managedPeople(page())
                assertEquals(
                    name,
                    first.nodesList
                        .single()
                        .values.name,
                )
                val next = fixture.managed("ManagedPeople").managedPeople(page(first.endCursor))
                assertEquals(fixture.person.toString(), next.nodesList.single().id)
                invalid { fixture.managed("ManagedPeople").managedPeople(page("eA:$id")) }
                invalid { fixture.managed("ManagedPeople").managedPeople(page(first.endCursor, "ARCHIVED")) }
            }
        }
    }

    private fun titleIds(result: Map<String, Any?>): List<*> {
        val data = result["data"] as Map<*, *>
        return (data["titles"] as Map<*, *>)["nodes"] as List<*>
    }

    private fun page(
        after: String? = null,
        state: String = "ACTIVE",
    ): PageRequest =
        PageRequest
            .newBuilder()
            .setState(state)
            .also { b -> after?.let { b.after = it } }
            .build()

    private fun invalid(action: () -> Unit) {
        assertEquals(Status.Code.INVALID_ARGUMENT, assertThrows(StatusRuntimeException::class.java, action).status.code)
    }

    private fun addTitle(
        fixture: PublicCatalogReadFixture,
        name: String,
        year: Int?,
        month: Int?,
        day: Int?,
    ): UUID =
        UUID.randomUUID().also { id ->
            fixture.pool.committed {
                it.executeBound(
                    "INSERT INTO title(id,kind,display_title,synopsis,synopsis_language,release_year," +
                        "release_month,release_day) VALUES(?,'MOVIE',?,'Synthetic read fixture','en',?,?,?)",
                    id,
                    name,
                    year,
                    month,
                    day,
                )
            }
        }

    private fun configured(vararg overrides: Pair<String, String>): PublicPolicy =
        PolicySettings.resolve(
            ConfigurationSources(
                emptyMap(),
                mapOf(
                    "REQUEST_DEFAULT_PAGE" to "1",
                    "REQUEST_MAX_PAGE" to "2",
                    "SEARCH_MIN_CODE_POINTS" to "2",
                    "SEARCH_MAX_CODE_POINTS" to "4",
                ) + overrides,
            ),
        )
}
