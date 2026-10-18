@file:Suppress("TooManyFunctions", "MagicNumber", "MaxLineLength")

package com.spoolin.catalog.features.movie

import com.spoolin.catalog.boundary.policy.CatalogWritePolicy
import com.spoolin.support.policy.PublicPolicy
import com.spoolin.support.policy.InputViolation
import com.spoolin.support.policy.PartialDate
import com.spoolin.support.policy.PatchValue
import com.spoolin.support.policy.TextBound
import com.spoolin.support.policy.TextRules
import com.spoolin.support.policy.StructuralRules
import spoolin.v1.Spoolin.MovieValues
import spoolin.v1.Spoolin.MovieWrite

private val BCP47_BOUND = TextBound(1, 35)
private const val RUNTIME_MAX = 100000

data class ValidatedMovieCreate(
    val displayTitle: String,
    val originalTitle: String?,
    val synopsis: String,
    val synopsisLanguage: String,
    val originalLanguage: String?,
    val countries: List<String>,
    val genreIds: Set<String>,
    val releaseYear: Int?,
    val releaseMonth: Int?,
    val releaseDay: Int?,
    val runtimeMinutes: Int?,
)

data class ValidatedMoviePatch(
    val displayTitle: PatchValue<String>,
    val originalTitle: PatchValue<String>,
    val synopsis: PatchValue<String>,
    val synopsisLanguage: PatchValue<String>,
    val originalLanguage: PatchValue<String>,
    val countries: PatchValue<List<String>>,
    val genreIds: PatchValue<Set<String>>,
    val releaseYear: PatchValue<Int?>,
    val releaseMonth: PatchValue<Int?>,
    val releaseDay: PatchValue<Int?>,
    val runtimeMinutes: PatchValue<Int?>,
)

/**
 * Input validation for movie create/update operations.
 *
 * PRD-BOUND-03/04: title 1–255 cp, synopsis 1–5000 cp.
 * PRD-CONCEPT-04: Preserve year/month/day precision; no invented January 1.
 * PRD-POLICY-26: Plain text only; no markup execution.
 */
object MovieValidation {
    fun validateCreate(
        values: MovieValues,
        policy: PublicPolicy = CatalogWritePolicy.environment(),
    ): ValidatedMovieCreate {
        val title = TextRules.ordinary(values.title, policy.textBounds.getValue("title"), "title")
        val originalTitle =
            values.originalTitle.takeIf { values.hasOriginalTitle() && !TextRules.blank(it) }?.let {
                TextRules.ordinary(it, policy.textBounds.getValue("originalTitle"), "originalTitle")
            }
        val synopsis = TextRules.ordinary(values.synopsis, policy.textBounds.getValue("synopsis"), "synopsis")
        val synopsisLang = if (values.hasSynopsisLanguage()) language(values.synopsisLanguage, "synopsisLanguage") else "und"
        val originalLang =
            values.originalLanguage.takeIf { values.hasOriginalLanguage() && !TextRules.blank(it) }?.let {
                language(it, "originalLanguage")
            }
        val countries = values.countriesList.map(::country)
        TextRules.genres(values.genreIdsList)
        val release = parseRelease(values, requireYear = true)
        val runtime = runtimeMinutes(values)
        return ValidatedMovieCreate(
            displayTitle = title,
            originalTitle = originalTitle,
            synopsis = synopsis,
            synopsisLanguage = synopsisLang,
            originalLanguage = originalLang,
            countries = countries,
            genreIds = values.genreIdsList.toSet(),
            releaseYear = release.year,
            releaseMonth = release.month,
            releaseDay = release.day,
            runtimeMinutes = runtime,
        )
    }

    fun validatePatch(
        write: MovieWrite,
        policy: PublicPolicy = CatalogWritePolicy.environment(),
    ): ValidatedMoviePatch {
        val mask = write.updateMask.pathsList.toSet()
        val v = write.values
        if (mask.any { it.startsWith("values.release.") }) {
            throw InputViolation("release", "Replace the complete release date.")
        }
        if ("values.release" in mask) parseRelease(v, requireYear = true)
        return ValidatedMoviePatch(
            displayTitle = patchText("values.title", mask, v.title, policy.textBounds.getValue("title"), "title"),
            originalTitle = patchOptText("values.original_title", mask, v.originalTitle.takeIf { v.hasOriginalTitle() }, policy.textBounds.getValue("originalTitle"), "originalTitle"),
            synopsis = patchText("values.synopsis", mask, v.synopsis, policy.textBounds.getValue("synopsis"), "synopsis"),
            synopsisLanguage = patchLanguage("values.synopsis_language", mask, v.synopsisLanguage, false, "synopsisLanguage"),
            originalLanguage = patchLanguage("values.original_language", mask,
                v.originalLanguage.takeIf { v.hasOriginalLanguage() }, true, "originalLanguage"),
            countries = patchCountries("values.countries", mask, v),
            genreIds = patchGenres("values.genre_ids", mask, v),
            releaseYear = patchReleaseYear("values.release", mask, v),
            releaseMonth = patchReleaseMonth("values.release.month", mask, v),
            releaseDay = patchReleaseDay("values.release.day", mask, v),
            runtimeMinutes = patchRuntime("values.runtime_minutes", mask, v),
        )
    }

    private fun patchText(
        path: String,
        mask: Set<String>,
        raw: String,
        bound: TextBound,
        field: String,
    ) = if (path in mask) PatchValue.Value(TextRules.ordinary(raw, bound, field)) else PatchValue.Omitted

    private fun patchOptText(
        path: String,
        mask: Set<String>,
        raw: String?,
        bound: TextBound,
        field: String,
    ) = if (path !in mask) {
        PatchValue.Omitted
    } else if (raw == null || TextRules.blank(raw)) {
        PatchValue.Clear
    } else {
        PatchValue.Value(TextRules.ordinary(raw, bound, field))
    }

    private fun language(raw: String, field: String): String {
        val value = TextRules.ordinary(raw, BCP47_BOUND, field)
        return try { StructuralRules.language(value) } catch (failure: InputViolation) {
            throw InputViolation(field, failure.correction)
        }
    }

    private fun country(raw: String): String = StructuralRules.country(TextRules.ordinary(raw, BCP47_BOUND, "countries"))

    private fun patchLanguage(path: String, mask: Set<String>, raw: String?, optional: Boolean, field: String): PatchValue<String> =
        when {
            path !in mask -> PatchValue.Omitted
            optional && (raw == null || TextRules.blank(raw)) -> PatchValue.Clear
            else -> PatchValue.Value(language(raw ?: "", field))
        }

    private fun patchCountries(
        path: String,
        mask: Set<String>,
        v: MovieValues,
    ): PatchValue<List<String>> =
        if (path !in mask) {
            PatchValue.Omitted
        } else {
            PatchValue.Value(v.countriesList.map(::country))
        }

    private fun patchGenres(
        path: String,
        mask: Set<String>,
        v: MovieValues,
    ): PatchValue<Set<String>> =
        if (path !in mask) {
            PatchValue.Omitted
        } else {
            PatchValue.Value(TextRules.genres(v.genreIdsList))
        }

    private fun patchReleaseYear(
        path: String,
        mask: Set<String>,
        v: MovieValues,
    ): PatchValue<Int?> =
        if (path !in mask) {
            PatchValue.Omitted
        } else if (v.release.hasYear()) {
            PatchValue.Value(v.release.year)
        } else {
            PatchValue.Clear
        }

    private fun patchReleaseMonth(
        path: String,
        mask: Set<String>,
        v: MovieValues,
    ): PatchValue<Int?> =
        if (path !in mask && "values.release" !in mask) {
            PatchValue.Omitted
        } else if (v.release.hasMonth()) {
            PatchValue.Value(v.release.month)
        } else {
            PatchValue.Clear
        }

    private fun patchReleaseDay(
        path: String,
        mask: Set<String>,
        v: MovieValues,
    ): PatchValue<Int?> =
        if (path !in mask && "values.release" !in mask) {
            PatchValue.Omitted
        } else if (v.release.hasDay()) {
            PatchValue.Value(v.release.day)
        } else {
            PatchValue.Clear
        }

    private fun patchRuntime(
        path: String,
        mask: Set<String>,
        v: MovieValues,
    ): PatchValue<Int?> =
        if (path !in mask) {
            PatchValue.Omitted
        } else if (v.hasRuntimeMinutes()) {
            PatchValue.Value(validRuntime(v.runtimeMinutes))
        } else {
            PatchValue.Clear
        }

    private fun parseRelease(
        values: MovieValues,
        requireYear: Boolean,
    ): PartialDate =
        PartialDate.of(
            year = if (values.release.hasYear()) values.release.year else null,
            month = if (values.release.hasMonth()) values.release.month else null,
            day = if (values.release.hasDay()) values.release.day else null,
            requireYear = requireYear,
        )

    private fun runtimeMinutes(values: MovieValues): Int? {
        if (!values.hasRuntimeMinutes()) return null
        return validRuntime(values.runtimeMinutes)
    }

    private fun validRuntime(minutes: Int): Int {
        if (minutes !in 1..RUNTIME_MAX) throw InputViolation("runtimeMinutes", "Use 1 to $RUNTIME_MAX minutes.")
        return minutes
    }
}
