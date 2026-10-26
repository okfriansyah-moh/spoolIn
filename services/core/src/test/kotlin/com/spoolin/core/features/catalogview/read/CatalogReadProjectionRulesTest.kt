package com.spoolin.core.features.catalogview.read

import com.google.protobuf.Timestamp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin

class CatalogReadProjectionRulesTest {
    @Test fun `public and managed title fields differ only by artwork authorization context`() {
        val title = Spoolin.Title.newBuilder()
            .setId("title").setAssetId("asset").setVersion(9007199254740993L)
            .setValues(Spoolin.MovieValues.newBuilder().setTitle("映画").setOriginalTitle("Original")
                .setOriginalLanguage("ja").setRuntimeMinutes(100))
            .setTvStatus("ENDED").setReleaseContext("FESTIVAL")
            .build()
        val public = CatalogReadProjection.title(title, management = false)
        val managed = CatalogReadProjection.title(title, management = true)
        assertEquals(public + ("_artworkManagement" to true), managed)
        assertEquals(false, public["_artworkManagement"])
        assertEquals("asset", public["_artworkAssetId"])
        assertEquals("9007199254740993", public["version"])
        assertEquals("映画", public["title"])
        assertEquals("Original", public["originalTitle"])
        assertEquals("ja", public["originalLanguage"])
        assertEquals(100, public["runtimeMinutes"])
        assertEquals("ENDED", public["tvStatus"])
        assertEquals("FESTIVAL", public["releaseContext"])
    }

    @Test fun `partial date precision preserves supplied components and missing title metadata remains null`() {
        val examples = listOf(
            Spoolin.PartialDate.getDefaultInstance() to mapOf("year" to null, "month" to null, "day" to null, "precision" to "UNKNOWN"),
            Spoolin.PartialDate.newBuilder().setYear(2024).build() to mapOf("year" to 2024, "month" to null, "day" to null, "precision" to "YEAR"),
            Spoolin.PartialDate.newBuilder().setYear(2024).setMonth(10).build() to mapOf("year" to 2024, "month" to 10, "day" to null, "precision" to "MONTH"),
            Spoolin.PartialDate.newBuilder().setYear(2024).setMonth(10).setDay(9).build() to mapOf("year" to 2024, "month" to 10, "day" to 9, "precision" to "DAY"),
        )
        for ((date, expected) in examples) {
            val title = Spoolin.Title.newBuilder().setValues(Spoolin.MovieValues.newBuilder().setRelease(date)).setTvEnd(date).build()
            val mapped = CatalogReadProjection.title(title, management = false)
            assertEquals(expected, mapped["release"])
            assertEquals(expected, mapped["tvEnd"])
            for (key in listOf("originalTitle", "originalLanguage", "runtimeMinutes", "tvStatus", "releaseContext", "_artworkAssetId")) {
                assertNull(mapped[key], key)
            }
        }
    }

    @Test fun `provenance retains timestamps reuse and field sources without fabricating missing URLs`() {
        val provenance = Spoolin.Provenance.newBuilder()
            .setSource("WIKIDATA").setExternalId("Q1").setReuseNote("CC0")
            .setRetrievedAt(Timestamp.newBuilder().setSeconds(1).setNanos(500000000))
            .addLocallyChanged("title")
            .addFieldSources(Spoolin.FieldSource.newBuilder().setField("title").setUrl("https://example.test/title").setReuseBasis("CC0"))
            .build()
        val title = Spoolin.Title.newBuilder().addProvenance(provenance).build()
        val mapped = CatalogReadProjection.title(title, management = false)
        val source = (mapped["provenance"] as List<*>).single() as Map<*, *>
        assertEquals("WIKIDATA", source["source"])
        assertEquals("Q1", source["externalId"])
        assertEquals("CC0", source["reuseNote"])
        assertEquals("1970-01-01T00:00:01.500Z", source["retrievedAt"])
        assertEquals(listOf("title"), source["locallyChanged"])
        assertNull(source["recordUrl"])
        val fields = source["fieldSources"] as Map<*, *>
        assertEquals("https://example.test/title", (fields["title"] as Map<*, *>)["url"])
    }

    @Test fun `all page projections retain order and distinguish absent cursor from a following page`() {
        val titles = Spoolin.TitlePage.newBuilder().addNodes(Spoolin.Title.newBuilder().setId("first"))
            .addNodes(Spoolin.Title.newBuilder().setId("second")).setEndCursor("next").setHasNextPage(true).build()
        val mapped = CatalogReadProjection.titlePage(titles, management = true)
        assertEquals(listOf("first", "second"), (mapped["nodes"] as List<*>).map { (it as Map<*, *>)["id"] })
        assertEquals(mapOf("endCursor" to "next", "hasNextPage" to true), mapped["pageInfo"])
        for (page in listOf(
            CatalogReadProjection.personPage(Spoolin.PersonPage.getDefaultInstance()),
            CatalogReadProjection.creditPage(Spoolin.CreditPage.getDefaultInstance()),
        )) {
            assertEquals(emptyList<Any>(), page["nodes"])
            assertEquals(mapOf("endCursor" to null, "hasNextPage" to false), page["pageInfo"])
        }
    }

    @Test fun `credit projection preserves embedded role character order and decimal version`() {
        val credit = Spoolin.Credit.newBuilder().setId("credit").setPersonId("person").setPersonName("山田")
            .setTitleId("title").setTitleName("Film").setVersion(9007199254740993L)
            .setRole(Spoolin.CreditRole.newBuilder().setId("role").setLabel("Actor").setCategory("CAST").setAllowsCharacters(true))
            .addCharacters("Maya").addCharacters("Young Maya").build()
        val mapped = CatalogReadProjection.credit(credit)
        assertEquals("person", mapped["personId"])
        assertEquals("山田", mapped["personName"])
        assertEquals("title", mapped["titleId"])
        assertEquals("Film", mapped["titleName"])
        assertEquals("9007199254740993", mapped["version"])
        assertEquals(listOf("Maya", "Young Maya"), mapped["characters"])
        assertEquals(mapOf("id" to "role", "label" to "Actor", "category" to "CAST", "allowsCharacters" to true), mapped["role"])
    }
}
