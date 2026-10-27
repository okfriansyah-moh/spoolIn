package com.spoolin.catalog.unit

import com.spoolin.catalog.features.proposal.ProposalContent
import com.spoolin.catalog.features.proposal.ProposalCredits
import com.spoolin.catalog.features.proposal.ProposalDraft
import com.spoolin.support.policy.InputViolation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin
import java.util.UUID

class ProposalCreditRulesTest {
    private val id = UUID.fromString("00000000-0000-4000-8000-000000000011")
    private val other = UUID.fromString("00000000-0000-4000-8000-000000000012")

    private fun named() =
        Spoolin.ProposalCredit
            .newBuilder()
            .setPersonName(" Ada ")
            .setRoleId(other.toString())
            .addCharacterNames(" Alice ")
            .setBillingOrder(0)
            .setPersonLabel("Untrusted")
            .setRoleLabel("Untrusted")

    private fun existing() = named().clearPersonName().setPersonId(id.toString())

    private fun role() = listOf(mapOf("category" to "CAST", "allows_characters" to true))

    @Test fun `proposal credits normalize plain text and discard caller labels`() {
        val credit = ProposalCredits.normalize(listOf(named().build())).single()
        assertEquals("Ada", credit.personName)
        assertEquals(listOf("Alice"), credit.characterNamesList)
        assertFalse(credit.hasPersonLabel())
        assertFalse(credit.hasRoleLabel())
        assertEquals(0, credit.billingOrder)
        assertEquals(credit, ProposalCredits.fromContent(ProposalCredits.content(credit)))
        val byId = ProposalCredits.normalize(listOf(existing().build())).single()
        assertEquals(id.toString(), byId.personId)
        assertFalse(byId.hasPersonName())
    }

    @Test fun `proposal credit identity cardinality duplicates billing and size are validated`() {
        for (credit in listOf(
            named().setPersonId(id.toString()).build(),
            named().clearPersonName().build(),
            named().setBillingOrder(-1).build(),
            named().setRoleId("bad").build(),
            existing().setPersonId("bad").build(),
        )) {
            assertThrows(IllegalArgumentException::class.java) { ProposalCredits.normalize(listOf(credit)) }
        }
        assertThrows(IllegalArgumentException::class.java) { ProposalCredits.normalize(List(101) { named().build() }) }
        assertThrows(IllegalArgumentException::class.java) { ProposalCredits.normalize(listOf(named().build(), named().build())) }
    }

    @Test fun `credit validation rejects unknown role inactive person and incompatible characters`() {
        for (rows in listOf(emptyList<Map<String, Any?>>(), listOf(mapOf("status" to "ARCHIVED")))) {
            JdbcRows(rows).source.connection.use { conn ->
                assertThrows(IllegalArgumentException::class.java) { ProposalCredits.validate(conn, listOf(existing().build())) }
            }
        }
        JdbcRows(emptyList()).source.connection.use { conn ->
            assertThrows(IllegalArgumentException::class.java) { ProposalCredits.validate(conn, listOf(named().build())) }
        }
        JdbcRows(listOf(mapOf("category" to "CREATOR", "allows_characters" to false))).source.connection.use { conn ->
            assertThrows(InputViolation::class.java) { ProposalCredits.validate(conn, listOf(named().build())) }
        }
    }

    @Test fun `proposal publish reuses unambiguous existing named person and writes billing`() {
        val normalized = ProposalCredits.normalize(listOf(named().build()))
        val db = JdbcRows(role(), listOf(mapOf("lock" to 1)), listOf(mapOf("id" to id)))
        db.source.connection.use { ProposalCredits.publish(it, normalized, other, id, other, id) }
        assertTrue(db.writes.any { id in it && other in it && 0 in it })
        assertTrue(db.writes.any { it == listOf(other) })
        assertTrue(
            db.writes
                .flatten()
                .filterIsInstance<String>()
                .any { it == "{}" },
        )
    }

    @Test fun `proposal publish creates missing named people and refuses ambiguous names`() {
        val normalized = ProposalCredits.normalize(listOf(named().build()))
        val db = JdbcRows(role(), listOf(mapOf("lock" to 1)), emptyList())
        db.source.connection.use { ProposalCredits.publish(it, normalized, other, id, other, id) }
        assertTrue(db.writes.any { "Ada" in it })
        val ambiguous = JdbcRows(role(), listOf(mapOf("lock" to 1)), listOf(mapOf("id" to id), mapOf("id" to other)))
        ambiguous.source.connection.use { conn ->
            assertThrows(IllegalArgumentException::class.java) { ProposalCredits.publish(conn, normalized, other, id, other, id) }
        }
        assertTrue(ambiguous.writes.isEmpty())
    }

    @Test fun `credit labels use database values and named people keep supplied normalized name`() {
        JdbcRows(listOf(mapOf("name" to "Ada")), listOf(mapOf("label" to "Actor"))).source.connection.use {
            val value = ProposalCredits.labels(it, existing().build())
            assertEquals("Ada", value.personLabel)
            assertEquals("Actor", value.roleLabel)
        }
        JdbcRows(emptyList()).source.connection.use {
            val value = ProposalCredits.labels(it, ProposalCredits.normalize(listOf(named().build())).single())
            assertEquals("Ada", value.personLabel)
            assertFalse(value.hasRoleLabel())
        }
    }

    @Test fun `normalized proposal preserves optional values source precision credits and poster reference`() {
        val request =
            Spoolin.Proposal
                .newBuilder()
                .setContext(Spoolin.Context.newBuilder().setPolicyRevision("policy-1"))
                .setValues(
                    Spoolin.MovieValues
                        .newBuilder()
                        .setTitle(" River ")
                        .setOriginalTitle(" Rivière ")
                        .setSynopsis(" Journey ")
                        .setSynopsisLanguage("en")
                        .setOriginalLanguage("fr")
                        .addCountries("FR")
                        .addGenreIds(id.toString())
                        .setRuntimeMinutes(95)
                        .setRelease(
                            Spoolin.PartialDate
                                .newBuilder()
                                .setYear(2024)
                                .setMonth(2)
                                .setDay(29),
                        ),
                ).addCredits(named())
                .setPosterRef(other.toString())
                .addSources(
                    Spoolin.FieldSource
                        .newBuilder()
                        .setField("title")
                        .setUrl("https://example.com/movie")
                        .setReuseBasis("CC0")
                        .setRetrievedAt(
                            com.google.protobuf.Timestamp
                                .newBuilder()
                                .setSeconds(123),
                        ).setPrecision("day"),
                ).build()
        val normalized = ProposalDraft.normalize(request)
        assertEquals("River", normalized.values.title)
        assertEquals("Ada", normalized.creditsList.single().personName)
        val content = ProposalContent.fromProposal(normalized)
        assertEquals("policy-1", ProposalContent.policyRevision(content))
        assertEquals(normalized.values, ProposalContent.movieValues(content))
        assertEquals(normalized.sourcesList, ProposalContent.sources(content))
        assertEquals(normalized.creditsList, ProposalContent.credits(content))
        assertEquals(other.toString(), content["poster_ref"])
        assertNull(ProposalContent.poster(content))
    }

    @Test fun `minimal proposal content keeps absent release optional values and empty collections`() {
        val content = ProposalContent.parseJson("""{"values":{"title":"River","synopsis":"Journey"}}""")
        val value = ProposalContent.movieValues(content)
        assertEquals("en", value.synopsisLanguage)
        assertFalse(value.hasRelease())
        assertFalse(value.hasOriginalTitle())
        assertEquals("", ProposalContent.policyRevision(content))
        assertTrue(ProposalContent.credits(content).isEmpty())
        assertTrue(ProposalContent.sources(content).isEmpty())
        assertThrows(IllegalArgumentException::class.java) { ProposalContent.movieValues(emptyMap()) }
    }
}
