package com.spoolin.integration.policy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

/** Real signed mTLS writes commit domain text and audit jsonb together, then replay without another audit. */
class CatalogAuditTextBoundaryTest {
    private val json = JsonMapper.builder().build()
    private val multiline = "First line\nSecond\tline\r\n\"quoted\" \\ café 🎬"

    @Test
    fun `multiline synopsis commits valid audit JSON and replays its receipt`() {
        CatalogPolicyFixture().use { fixture ->
            val original = fixture.movie("Multiline movie")
            val request = original.toBuilder().setValues(original.values.toBuilder().setSynopsis(multiline)).build()
            val result = fixture.movie(request)
            assertEquals("SUCCEEDED", result.state)
            assertEquals(multiline, fixture.text("SELECT synopsis FROM title WHERE id=?", result.resultId))
            val audit =
                fixture.text("SELECT changes::text FROM catalog_audit_event WHERE subject_id=?", result.resultId)
            val change =
                json.readTree(audit).path("changes").single { it.path("path").stringValue() == "synopsis" }
            assertEquals(multiline, change.path("after").stringValue())
            assertEquals(result, fixture.movie(request))
            assertEquals(1, fixture.count("SELECT count(*) FROM catalog_audit_event WHERE subject_kind='TITLE'"))
        }
    }

    @Test
    fun `multiline biography commits valid audit JSON and replays its receipt`() {
        CatalogPolicyFixture().use { fixture ->
            val original = fixture.person("Multiline person")
            val request = original.toBuilder().setValues(original.values.toBuilder().setBiography(multiline)).build()
            val result = fixture.person(request)
            assertEquals("SUCCEEDED", result.state)
            assertEquals(multiline, fixture.text("SELECT biography FROM person WHERE id=?", result.resultId))
            val audit =
                fixture.text("SELECT changes::text FROM catalog_audit_event WHERE subject_id=?", result.resultId)
            val change =
                json.readTree(audit).path("changes").single { it.path("path").stringValue() == "biography" }
            assertEquals(multiline, change.path("after").stringValue())
            assertEquals(result, fixture.person(request))
            assertEquals(1, fixture.count("SELECT count(*) FROM catalog_audit_event WHERE subject_kind='PERSON'"))
        }
    }
}
