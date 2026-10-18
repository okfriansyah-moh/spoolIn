package com.spoolin.catalog.features.policy

import com.google.protobuf.FieldMask
import com.spoolin.catalog.boundary.policy.CatalogWriteIntent
import com.spoolin.support.receipts.OperationIntent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin
import java.util.UUID

class CatalogWriteIntentRulesTest {
    @Test
    fun `movie fingerprint binds all text dates optional presence collections versions and mask`() {
        val original = movie()
        val values = original.values
        val alternatives =
            listOf(
                original.toBuilder().setId(ID).build(),
                original.toBuilder().setExpectedVersion(0).build(),
                original.toBuilder().setValues(values.toBuilder().setTitle("Changed")).build(),
                original.toBuilder().setValues(values.toBuilder().clearOriginalTitle()).build(),
                original.toBuilder().setValues(values.toBuilder().setOriginalTitle("")).build(),
                original.toBuilder().setValues(values.toBuilder().setSynopsis("Changed")).build(),
                original.toBuilder().setValues(values.toBuilder().setSynopsisLanguage("fr")).build(),
                original.toBuilder().setValues(values.toBuilder().clearOriginalLanguage()).build(),
                original.toBuilder().setValues(values.toBuilder().clearRuntimeMinutes()).build(),
                original.toBuilder().setValues(values.toBuilder().setRuntimeMinutes(0)).build(),
                original.toBuilder().setValues(values.toBuilder().clearRelease()).build(),
                original.toBuilder().setValues(values.toBuilder().setRelease(values.release.toBuilder().setYear(2021))).build(),
                original.toBuilder().setValues(values.toBuilder().setRelease(values.release.toBuilder().clearMonth())).build(),
                original.toBuilder().setValues(values.toBuilder().setRelease(values.release.toBuilder().setDay(28))).build(),
                original.toBuilder().setValues(values.toBuilder().clearCountries().addCountries("FR")).build(),
                original.toBuilder().setValues(values.toBuilder().clearGenreIds().addGenreIds(OTHER)).build(),
                original.toBuilder().setUpdateMask(FieldMask.newBuilder().addPaths("values.title")).build(),
            )
        alternatives.forEachIndexed { index, changed ->
            assertNotEquals(CatalogWriteIntent.movie(original), CatalogWriteIntent.movie(changed), "movie variant $index")
        }
        assertEquals("catalog-write-v2", CatalogWriteIntent.movie(original).first().second)
    }

    @Test
    fun `set-like genre and mask ordering is stable while ordered countries are not discarded`() {
        val original =
            movie()
                .toBuilder()
                .setUpdateMask(
                    FieldMask
                        .newBuilder()
                        .addPaths("values.title")
                        .addPaths("values.synopsis"),
                ).build()
        val reordered =
            original
                .toBuilder()
                .setValues(
                    original.values
                        .toBuilder()
                        .clearGenreIds()
                        .addGenreIds(OTHER)
                        .addGenreIds(ID),
                ).setUpdateMask(
                    FieldMask
                        .newBuilder()
                        .addPaths("values.synopsis")
                        .addPaths("values.title"),
                ).build()
        assertEquals(CatalogWriteIntent.movie(original), CatalogWriteIntent.movie(reordered))
        val countries =
            original
                .toBuilder()
                .setValues(
                    original.values
                        .toBuilder()
                        .clearCountries()
                        .addCountries("FR")
                        .addCountries("GB"),
                ).build()
        assertNotEquals(CatalogWriteIntent.movie(original), CatalogWriteIntent.movie(countries))
    }

    @Test
    fun `person fingerprint binds biography presence expected-version and preserves invalid code units before validation`() {
        val original =
            Spoolin.PersonWrite
                .newBuilder()
                .setValues(Spoolin.PersonValues.newBuilder().setName("Name"))
                .build()
        val variants =
            listOf(
                original.toBuilder().setExpectedVersion(0).build(),
                original.toBuilder().setId(ID).build(),
                original.toBuilder().setValues(original.values.toBuilder().setBiography("")).build(),
                original.toBuilder().setValues(original.values.toBuilder().setBiography("Biography")).build(),
                original.toBuilder().setUpdateMask(FieldMask.newBuilder().addPaths("values.biography")).build(),
            )
        variants.forEach { assertNotEquals(CatalogWriteIntent.person(original), CatalogWriteIntent.person(it)) }
        val first = original.toBuilder().setValues(original.values.toBuilder().setName("\uD800")).build()
        val second = original.toBuilder().setValues(original.values.toBuilder().setName("\uD801")).build()
        assertNotEquals(CatalogWriteIntent.person(first), CatalogWriteIntent.person(second))
    }

    @Test
    fun `credit fingerprint binds ordered character arrays without delimiter collisions and all owner identities`() {
        val original =
            Spoolin.CreditWrite
                .newBuilder()
                .setTitleId(ID)
                .setPersonId(ID)
                .setRoleId(ID)
                .addCharacters("A,B")
                .addCharacters("C")
                .build()
        val variants =
            listOf(
                original.toBuilder().setId(OTHER).build(),
                original.toBuilder().setTitleId(OTHER).build(),
                original.toBuilder().setPersonId(OTHER).build(),
                original.toBuilder().setRoleId(OTHER).build(),
                original.toBuilder().setExpectedVersion(1).build(),
                original
                    .toBuilder()
                    .clearCharacters()
                    .addCharacters("A")
                    .addCharacters("B,C")
                    .build(),
                original
                    .toBuilder()
                    .clearCharacters()
                    .addCharacters("C")
                    .addCharacters("A,B")
                    .build(),
                original.toBuilder().setUpdateMask(FieldMask.newBuilder().addPaths("characters")).build(),
            )
        variants.forEach { assertNotEquals(CatalogWriteIntent.credit(original), CatalogWriteIntent.credit(it)) }
    }

    @Test
    fun `outer actor kind and policy remain covered and no legacy field list replays as the new format`() {
        val request = movie()
        val fields = CatalogWriteIntent.movie(request)
        val current = OperationIntent(UUID.randomUUID(), "account:$ID", "WriteMovie", "policy-a", fields)
        assertNotEquals(current.serialized().toList(), current.copy(policyRevision = "policy-b").serialized().toList())
        assertNotEquals(current.serialized().toList(), current.copy(actorScope = "account:$OTHER").serialized().toList())
        assertNotEquals(current.serialized().toList(), current.copy(kind = "WritePerson").serialized().toList())
        assertNotEquals(current.serialized().toList(), current.copy(fields = listOf("title" to request.values.title)).serialized().toList())
        val role =
            Spoolin.RoleWrite
                .newBuilder()
                .setCategory("CAST")
                .setLabel("Role")
                .build()
        assertNotEquals(CatalogWriteIntent.role(role), CatalogWriteIntent.role(role.toBuilder().setLabel("Other").build()))
    }

    private fun movie(): Spoolin.MovieWrite =
        Spoolin.MovieWrite
            .newBuilder()
            .setValues(
                Spoolin.MovieValues
                    .newBuilder()
                    .setTitle("Title")
                    .setOriginalTitle("Original")
                    .setSynopsis("Line1\nLine2")
                    .setSynopsisLanguage("en")
                    .setOriginalLanguage("en")
                    .setRuntimeMinutes(90)
                    .addCountries("GB")
                    .addCountries("FR")
                    .addGenreIds(ID)
                    .addGenreIds(OTHER)
                    .setRelease(
                        Spoolin.PartialDate
                            .newBuilder()
                            .setYear(2020)
                            .setMonth(2)
                            .setDay(29),
                    ),
            ).build()

    companion object {
        private const val ID = "00000000-0000-0000-0000-000000000001"
        private const val OTHER = "00000000-0000-0000-0000-000000000002"
    }
}
