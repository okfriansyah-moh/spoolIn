package com.spoolin.catalog.features.taxonomy.roles

import com.spoolin.support.policy.InputViolation
import com.spoolin.support.policy.TextBound
import com.spoolin.support.policy.TextRules

// PRD-BOUND-05: Custom credit role 1–100 code points; must belong to Cast or Creator.
private val ROLE_LABEL_BOUND = TextBound(1, 100)

data class ValidatedRoleCreate(
    val category: String,
    val label: String,
)

object RoleValidation {
    private val categories = setOf("CAST", "CREATOR")

    fun validateCreate(category: String, label: String): ValidatedRoleCreate {
        val normalizedCategory = category.trim().uppercase()
        if (normalizedCategory !in categories) {
            throw InputViolation("category", "Choose Cast or Creator.")
        }
        val trimmedLabel = TextRules.ordinary(label, ROLE_LABEL_BOUND, "label")
        return ValidatedRoleCreate(category = normalizedCategory, label = trimmedLabel)
    }
}
