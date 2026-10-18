@file:Suppress("MagicNumber")

package com.spoolin.catalog.features.taxonomy.roles

import com.spoolin.catalog.boundary.policy.CatalogWritePolicy
import com.spoolin.support.policy.PublicPolicy
import com.spoolin.support.policy.InputViolation
import com.spoolin.support.policy.TextRules


data class ValidatedRoleCreate(
    val category: String,
    val label: String,
)

object RoleValidation {
    private val categories = setOf("CAST", "CREATOR")

    fun validateCreate(
        category: String,
        label: String,
        policy: PublicPolicy = CatalogWritePolicy.environment(),
    ): ValidatedRoleCreate {
        val normalizedCategory = category.trim().uppercase()
        if (normalizedCategory !in categories) {
            throw InputViolation("category", "Choose Cast or Creator.")
        }
        val trimmedLabel = TextRules.ordinary(label, policy.textBounds.getValue("creditRole"), "label")
        return ValidatedRoleCreate(category = normalizedCategory, label = trimmedLabel)
    }
}
