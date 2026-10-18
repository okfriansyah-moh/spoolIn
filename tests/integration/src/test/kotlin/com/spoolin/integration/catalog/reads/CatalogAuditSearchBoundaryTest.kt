package com.spoolin.integration.catalog.reads

import com.spoolin.catalog.operations.audit.AuditDiff
import com.spoolin.catalog.operations.audit.AuditInsert
import com.spoolin.integration.identity.SignedReceiptFixture
import com.spoolin.support.receipts.committed
import com.spoolin.support.receipts.executeBound
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.Spoolin.Search
import java.time.Instant
import java.util.UUID

class CatalogAuditSearchBoundaryTest {
    @Test
    fun `credited cast and creator names match visible titles once and archived people cannot supply a match`() {
        PublicCatalogReadFixture().use { fixture ->
            val independent = title(fixture, "Independent Needle%")
            val creatorOnly = title(fixture, "Creator credit")
            val needle = person(fixture, "Needle%")
            val creator = person(fixture, "Maker")
            val falseWildcard = person(fixture, "NeedleX")
            credit(fixture, fixture.title, needle, "CAST")
            credit(fixture, fixture.title, needle, "CREATOR")
            credit(fixture, fixture.archived, needle, "CAST")
            credit(fixture, creatorOnly, creator, "CREATOR")
            credit(fixture, creatorOnly, falseWildcard, "CAST")
            val stub = CatalogServiceGrpc.newBlockingStub(fixture.caller)
            assertEquals(setOf(fixture.title.toString(), independent.toString()), search(stub, "needle%"))
            assertEquals(setOf(creatorOnly.toString()), search(stub, "MAKER"))
            fixture.pool.committed { it.executeBound("UPDATE person SET status='ARCHIVED' WHERE id=?", needle) }
            assertEquals(setOf(independent.toString()), search(stub, "needle%"))
            fixture.pool.committed { it.executeBound("UPDATE person SET status='ARCHIVED' WHERE id=?", creator) }
            assertTrue(search(stub, "maker").isEmpty())
        }
    }

    @Test
    fun `actual audit GraphQL returns typed safe values legacy changes and honest omission metadata`() {
        SignedReceiptFixture("catalog-audit-projection.json").use { identity ->
            PublicCatalogReadFixture(proof = identity).use { fixture ->
                record(fixture, identity.account, INLINE)
                record(fixture, identity.account, LEGACY)
                record(
                    fixture,
                    identity.account,
                    AuditDiff.movie(
                        mapOf("synopsis" to "x".repeat(70000)),
                        mapOf("synopsis" to "y".repeat(70000)),
                    ),
                )
                val result = fixture.graphql("{ audit(first: 3) { nodes { id changes actorRef } } }")
                assertFalse(result.containsKey("errors"), result.toString())
                val data = result["data"] as Map<*, *>
                val nodes = (data["audit"] as Map<*, *>)["nodes"] as List<*>
                assertEquals(3, nodes.size)
                val changes = nodes.map { (it as Map<*, *>)["changes"] as Map<*, *> }
                val normal = changes.single { "displayTitle" in it }
                assertEquals(mapOf("before" to "Old", "after" to "New"), normal["displayTitle"])
                assertEquals(mapOf("before" to null, "after" to 108), normal["runtimeMinutes"])
                assertEquals(mapOf("before" to emptyList<String>(), "after" to listOf("ID")), normal["countries"])
                assertEquals(mapOf("before" to 1, "after" to 2), changes.single { "version" in it }["version"])
                val summary = changes.single { "synopsis" in it }["synopsis"] as Map<*, *>
                assertEquals(true, summary["valueOmitted"])
                assertEquals(140004, summary["byteLength"])
                assertEquals(64, (summary["afterDigest"] as String).length)
                assertFalse(result.toString().contains("must-not-leak"))
                assertFalse(result.toString().contains("password"))
            }
        }
    }

    private fun search(
        stub: CatalogServiceGrpc.CatalogServiceBlockingStub,
        text: String,
    ): Set<String> =
        stub
            .searchTitles(Search.newBuilder().setText(text).build())
            .nodesList
            .map { it.id }
            .toSet()

    private fun title(
        fixture: PublicCatalogReadFixture,
        name: String,
    ): UUID =
        UUID.randomUUID().also { id ->
            fixture.pool.committed {
                it.executeBound(
                    "INSERT INTO title(id,kind,display_title,synopsis,synopsis_language) " +
                        "VALUES(?,'MOVIE',?,'Synthetic search record','en')",
                    id,
                    name,
                )
            }
        }

    private fun person(
        fixture: PublicCatalogReadFixture,
        name: String,
    ): UUID =
        UUID.randomUUID().also { id ->
            fixture.pool.committed { it.executeBound("INSERT INTO person(id,name) VALUES(?,?)", id, name) }
        }

    private fun credit(
        fixture: PublicCatalogReadFixture,
        title: UUID,
        person: UUID,
        category: String,
    ) {
        fixture.pool.committed { connection ->
            val role = UUID.randomUUID()
            connection.executeBound(
                "INSERT INTO credit_role(id,category,label,normalized_label,allows_characters) VALUES(?,?,?,?,?)",
                role,
                category,
                role.toString(),
                role.toString(),
                category == "CAST",
            )
            connection.executeBound(
                "INSERT INTO credit(id,title_id,person_id,role_id) VALUES(?,?,?,?)",
                UUID.randomUUID(),
                title,
                person,
                role,
            )
        }
    }

    private fun record(
        fixture: PublicCatalogReadFixture,
        actor: UUID,
        changes: String,
    ) {
        fixture.pool.committed { connection ->
            AuditInsert.record(
                connection,
                null,
                actor,
                "HUMAN",
                "UPDATE_MOVIE",
                "TITLE",
                fixture.title,
                "SUCCEEDED",
                changes,
                UUID.randomUUID(),
                Instant.now(),
            )
        }
    }

    companion object {
        private const val INLINE = """{"changes":[{"path":"displayTitle","before":"Old","after":"New"},
            {"path":"runtimeMinutes","before":null,"after":108},{"path":"countries","before":[],"after":["ID"]},
            {"path":"password","before":null,"after":"must-not-leak"}],"summarised":false}"""
        private const val LEGACY = """{"version":{"before":1,"after":2},
            "accountEmail":{"after":"must-not-leak"}}"""
    }
}
