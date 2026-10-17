package com.spoolin.catalog.features.person.write

import com.spoolin.support.policy.PatchValue
import com.spoolin.support.policy.TextBound
import com.spoolin.support.policy.TextRules
import spoolin.v1.Spoolin.PersonValues
import spoolin.v1.Spoolin.PersonWrite

// PRD-BOUND-01: Catalog person name 1–100 code points.
// Distinct from account registration name defaults (2–100, PRD-CONCEPT-09).
// PRD-POLICY-18: Support mononyms, Unicode, spaces, hyphens, apostrophes;
//   no given-name/family-name split required.
private val NAME_BOUND = TextBound(1, 100)

// TD-ROW-L4490: Optional plain text, default max 5000 code points.
private val BIOGRAPHY_BOUND = TextBound(1, 5000, required = false)

data class ValidatedPersonCreate(
    val name: String,
    val biography: String?,
)

data class ValidatedPersonPatch(
    val name: PatchValue<String>,
    val biography: PatchValue<String>,
)

/**
 * Input validation for person create and update operations.
 *
 * PRD-BOUND-01: name 1–100 code points (not unique; mononyms and punctuation intact).
 * PRD-POLICY-18: No required family/given name split; one-character professional names allowed.
 * AC-140-C01: Accept 1 and 100, reject 0 and 101 with a field-specific error and no partial change.
 */
object PersonValidation {

    fun validateCreate(values: PersonValues): ValidatedPersonCreate {
        val name = TextRules.ordinary(values.name, NAME_BOUND, "name")
        val biography = values.biography
            .takeIf { it.isNotEmpty() }
            ?.let { TextRules.ordinary(it, BIOGRAPHY_BOUND, "biography") }
        return ValidatedPersonCreate(name = name, biography = biography)
    }

    fun validatePatch(write: PersonWrite): ValidatedPersonPatch {
        val mask = write.updateMask.pathsList.toSet()
        val v = write.values
        return ValidatedPersonPatch(
            name = patchName("values.name", mask, v.name),
            biography = patchBiography("values.biography", mask, v.biography),
        )
    }

    private fun patchName(path: String, mask: Set<String>, raw: String): PatchValue<String> =
        if (path in mask) PatchValue.Value(TextRules.ordinary(raw, NAME_BOUND, "name"))
        else PatchValue.Omitted

    private fun patchBiography(path: String, mask: Set<String>, raw: String): PatchValue<String> =
        if (path !in mask) PatchValue.Omitted
        else if (raw.isEmpty()) PatchValue.Clear
        else PatchValue.Value(TextRules.ordinary(raw, BIOGRAPHY_BOUND, "biography"))
}
