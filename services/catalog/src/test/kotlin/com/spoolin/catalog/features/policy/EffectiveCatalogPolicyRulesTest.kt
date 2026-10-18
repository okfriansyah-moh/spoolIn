package com.spoolin.catalog.features.policy

import com.google.protobuf.FieldMask
import com.spoolin.catalog.features.credit.CreditValidation
import com.spoolin.catalog.features.movie.MovieValidation
import com.spoolin.catalog.features.person.write.PersonValidation
import com.spoolin.catalog.features.taxonomy.roles.RoleValidation
import com.spoolin.support.policy.ConfigurationSources
import com.spoolin.support.policy.InputViolation
import com.spoolin.support.policy.PatchValue
import com.spoolin.support.policy.PolicySettings
import com.spoolin.support.policy.PublicPolicy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin

class EffectiveCatalogPolicyRulesTest {
    @Test
    fun `all seven configurable owner fields honor effective minima maxima and Unicode scalars`() {
        cases().forEach { field ->
            val policy = configured(mapOf("${field.prefix}_MIN_CODE_POINTS" to "2", "${field.prefix}_MAX_CODE_POINTS" to "3"))
            assertEquals("e\u0301", field.validate("\u00a0e\u0301\u00a0", policy))
            assertEquals("🎬x", field.validate("🎬x", policy))
            assertEquals("abc", field.validate("abc", policy))
            listOf("x", "abcd", "\uD800").forEach { invalid ->
                assertThrows(InputViolation::class.java) { field.validate(invalid, policy) }
            }
        }
    }

    @Test
    fun `effective patch validation preserves omitted legacy fields and explicit optional clears`() {
        val policy =
            configured(
                mapOf(
                    "PERSON_NAME_MIN_CODE_POINTS" to "5",
                    "BIOGRAPHY_MIN_CODE_POINTS" to "5",
                    "TITLE_MIN_CODE_POINTS" to "5",
                    "ORIGINAL_TITLE_MIN_CODE_POINTS" to "5",
                    "CHARACTER_NAME_MIN_CODE_POINTS" to "5",
                ),
            )
        val person =
            Spoolin.PersonWrite
                .newBuilder()
                .setValues(Spoolin.PersonValues.newBuilder().setName("x"))
                .setUpdateMask(FieldMask.newBuilder().addPaths("values.biography"))
                .build()
        val personPatch = PersonValidation.validatePatch(person, policy)
        assertEquals(PatchValue.Omitted, personPatch.name)
        assertEquals(PatchValue.Clear, personPatch.biography)
        val movie =
            Spoolin.MovieWrite
                .newBuilder()
                .setValues(movie().toBuilder().setTitle("x"))
                .setUpdateMask(FieldMask.newBuilder().addPaths("values.original_title"))
                .build()
        val moviePatch = MovieValidation.validatePatch(movie, policy)
        assertEquals(PatchValue.Omitted, moviePatch.displayTitle)
        assertEquals(PatchValue.Clear, moviePatch.originalTitle)
        val credit =
            Spoolin.CreditWrite
                .newBuilder()
                .setUpdateMask(FieldMask.newBuilder().addPaths("characters"))
                .build()
        assertEquals(PatchValue.Value(emptyList<String>()), CreditValidation.validatePatch(credit, policy).characters)
    }

    @Test
    fun `Unicode blank optional text clears and whole date replacement never retains omitted month or day`() {
        val values =
            movie()
                .toBuilder()
                .setOriginalTitle("\u00a0 ")
                .setOriginalLanguage(" ")
                .build()
        assertEquals(null, MovieValidation.validateCreate(values).originalTitle)
        val person =
            Spoolin.PersonWrite
                .newBuilder()
                .setValues(Spoolin.PersonValues.newBuilder().setBiography("\u00a0 "))
                .setUpdateMask(FieldMask.newBuilder().addPaths("values.biography"))
                .build()
        assertEquals(PatchValue.Clear, PersonValidation.validatePatch(person).biography)
        val update =
            Spoolin.MovieWrite
                .newBuilder()
                .setValues(
                    movie().toBuilder().setRelease(
                        Spoolin.PartialDate.newBuilder().setYear(2024),
                    ),
                ).setUpdateMask(FieldMask.newBuilder().addPaths("values.release"))
                .build()
        val patch = MovieValidation.validatePatch(update)
        assertEquals(PatchValue.Value(2024), patch.releaseYear)
        assertEquals(PatchValue.Clear, patch.releaseMonth)
        assertEquals(PatchValue.Clear, patch.releaseDay)
        val invalid =
            update
                .toBuilder()
                .setValues(
                    update.values.toBuilder().setRelease(
                        Spoolin.PartialDate
                            .newBuilder()
                            .setYear(2023)
                            .setMonth(2)
                            .setDay(29),
                    ),
                ).build()
        assertThrows(InputViolation::class.java) { MovieValidation.validatePatch(invalid) }
    }

    private data class Field(
        val prefix: String,
        val validate: (String, PublicPolicy) -> String,
    )

    private fun cases(): List<Field> =
        listOf(
            Field(
                "TITLE",
            ) { raw, policy -> MovieValidation.validateCreate(movie().toBuilder().setTitle(raw).build(), policy).displayTitle },
            Field("ORIGINAL_TITLE") { raw, policy ->
                requireNotNull(MovieValidation.validateCreate(movie().toBuilder().setOriginalTitle(raw).build(), policy).originalTitle)
            },
            Field(
                "SYNOPSIS",
            ) { raw, policy -> MovieValidation.validateCreate(movie().toBuilder().setSynopsis(raw).build(), policy).synopsis },
            Field("PERSON_NAME") { raw, policy ->
                PersonValidation
                    .validateCreate(
                        Spoolin.PersonValues
                            .newBuilder()
                            .setName(raw)
                            .build(),
                        policy,
                    ).name
            },
            Field("BIOGRAPHY") { raw, policy ->
                val values =
                    Spoolin.PersonValues
                        .newBuilder()
                        .setName("x")
                        .setBiography(raw)
                        .build()
                requireNotNull(PersonValidation.validateCreate(values, policy).biography)
            },
            Field("CREDIT_ROLE") { raw, policy -> RoleValidation.validateCreate("CAST", raw, policy).label },
            Field("CHARACTER_NAME") { raw, policy ->
                CreditValidation
                    .validateCreate(
                        Spoolin.CreditWrite
                            .newBuilder()
                            .setTitleId(ID)
                            .setPersonId(ID)
                            .setRoleId(ID)
                            .addCharacters(raw)
                            .build(),
                        policy,
                    ).characters
                    .single()
            },
        )

    private fun movie(): Spoolin.MovieValues =
        Spoolin.MovieValues
            .newBuilder()
            .setTitle("Title")
            .setSynopsis("Synopsis")
            .setSynopsisLanguage("en")
            .addGenreIds(ID)
            .setRelease(Spoolin.PartialDate.newBuilder().setYear(2024))
            .build()

    companion object {
        private const val ID = "00000000-0000-0000-0000-000000000001"

        private fun configured(values: Map<String, String>): PublicPolicy = PolicySettings.resolve(ConfigurationSources(emptyMap(), values))
    }
}
