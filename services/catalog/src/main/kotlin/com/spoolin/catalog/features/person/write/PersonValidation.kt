@file:Suppress("MagicNumber")

package com.spoolin.catalog.features.person.write

import com.spoolin.catalog.boundary.policy.CatalogWritePolicy
import com.spoolin.support.policy.PublicPolicy
import com.spoolin.support.policy.PatchValue
import com.spoolin.support.policy.TextBound
import com.spoolin.support.policy.TextRules
import spoolin.v1.Spoolin.PersonValues
import spoolin.v1.Spoolin.PersonWrite

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
    fun validateCreate(
        values: PersonValues,
        policy: PublicPolicy = CatalogWritePolicy.environment(),
    ): ValidatedPersonCreate {
        val name = TextRules.ordinary(values.name, policy.textBounds.getValue("personName"), "name")
        val biography =
            values.biography
                .takeIf { it.isNotEmpty() }
                ?.let { TextRules.ordinary(it, policy.textBounds.getValue("biography"), "biography") }
        return ValidatedPersonCreate(name = name, biography = biography)
    }

    fun validatePatch(
        write: PersonWrite,
        policy: PublicPolicy = CatalogWritePolicy.environment(),
    ): ValidatedPersonPatch {
        val mask = write.updateMask.pathsList.toSet()
        val v = write.values
        return ValidatedPersonPatch(
            name = patchName("values.name", mask, v.name, policy.textBounds.getValue("personName")),
            biography = patchBiography("values.biography", mask, v.biography, policy.textBounds.getValue("biography")),
        )
    }

    private fun patchName(
        path: String,
        mask: Set<String>,
        raw: String,
        bound: TextBound,
    ): PatchValue<String> =
        if (path in mask) {
            PatchValue.Value(TextRules.ordinary(raw, bound, "name"))
        } else {
            PatchValue.Omitted
        }

    private fun patchBiography(
        path: String,
        mask: Set<String>,
        raw: String,
        bound: TextBound,
    ): PatchValue<String> =
        if (path !in mask) {
            PatchValue.Omitted
        } else if (raw.isEmpty()) {
            PatchValue.Clear
        } else {
            PatchValue.Value(TextRules.ordinary(raw, bound, "biography"))
        }
}
