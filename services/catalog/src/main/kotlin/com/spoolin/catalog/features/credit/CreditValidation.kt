@file:Suppress("MagicNumber")

package com.spoolin.catalog.features.credit

import com.spoolin.catalog.boundary.policy.CatalogWritePolicy
import com.spoolin.support.policy.PublicPolicy
import com.spoolin.support.policy.InputViolation
import com.spoolin.support.policy.PatchValue
import com.spoolin.support.policy.TextBound
import com.spoolin.support.policy.TextRules
import spoolin.v1.Spoolin.CreditWrite


data class ValidatedCreditCreate(
    val personId: String,
    val roleId: String,
    val characters: List<String>,
)

data class ValidatedCreditPatch(
    val roleId: PatchValue<String>,
    val characters: PatchValue<List<String>>,
)

object CreditValidation {
    fun validateCreate(
        request: CreditWrite,
        policy: PublicPolicy = CatalogWritePolicy.environment(),
    ): ValidatedCreditCreate {
        requirePersonRoleIds(request)
        return ValidatedCreditCreate(
            personId = request.personId,
            roleId = request.roleId,
            characters = normalizeCharacters(request.charactersList, "characters", policy.textBounds.getValue("characterName")),
        )
    }

    fun validatePatch(
        request: CreditWrite,
        policy: PublicPolicy = CatalogWritePolicy.environment(),
    ): ValidatedCreditPatch {
        val mask = request.updateMask.pathsList.toSet()
        return ValidatedCreditPatch(
            roleId = patchRole(mask, request.roleId),
            characters = patchCharacters(mask, request.charactersList, policy.textBounds.getValue("characterName")),
        )
    }

    fun validateCharactersForRole(
        allowsCharacters: Boolean,
        category: String,
        characters: List<String>,
    ) {
        if (category == "CREATOR" && characters.isNotEmpty()) {
            throw InputViolation("characters", "Creator roles cannot include character names.")
        }
        if (!allowsCharacters && characters.isNotEmpty()) {
            throw InputViolation("characters", "This role does not accept character names.")
        }
    }

    private fun patchRole(
        mask: Set<String>,
        raw: String,
    ): PatchValue<String> =
        if ("roleId" in mask) {
            PatchValue.Value(parseUuid(raw, "roleId"))
        } else {
            PatchValue.Omitted
        }

    private fun patchCharacters(
        mask: Set<String>,
        raw: List<String>,
        bound: TextBound,
    ): PatchValue<List<String>> =
        when {
            "characters" in mask -> PatchValue.Value(normalizeCharacters(raw, "characters", bound))
            else -> PatchValue.Omitted
        }

    private fun requirePersonRoleIds(request: CreditWrite) {
        parseUuid(request.personId, "personId")
        parseUuid(request.roleId, "roleId")
        parseUuid(request.titleId, "titleId")
    }

    private fun parseUuid(
        value: String,
        path: String,
    ): String =
        runCatching {
            java.util.UUID
                .fromString(value)
                .also { require(it.toString() == value) }
                .toString()
        }.getOrElse { throw InputViolation(path, "Supply a lowercase, hyphenated UUID.") }

    private fun normalizeCharacters(
        values: List<String>,
        path: String,
        bound: TextBound,
    ): List<String> {
        val trimmed = values.map { TextRules.ordinary(it, bound, path) }
        val seen = mutableSetOf<String>()
        for (name in trimmed) {
            val key = name.trim()
            if (key in seen) throw InputViolation(path, "Remove duplicate character names.")
            seen.add(key)
        }
        return trimmed
    }
}
