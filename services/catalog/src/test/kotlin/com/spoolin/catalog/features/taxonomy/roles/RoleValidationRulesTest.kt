package com.spoolin.catalog.features.taxonomy.roles

import com.spoolin.support.policy.InputViolation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class RoleValidationRulesTest {
    @Test
    fun `custom role label accepts one character (PRD-BOUND-05)`() {
        val result = RoleValidation.validateCreate("CAST", "V")
        assertEquals("CAST", result.category)
        assertEquals("V", result.label)
    }

    @Test
    fun `101 code point role label rejects`() {
        assertThrows(InputViolation::class.java) {
            RoleValidation.validateCreate("CREATOR", "A".repeat(101))
        }
    }

    @Test
    fun `invalid category rejects`() {
        assertThrows(InputViolation::class.java) {
            RoleValidation.validateCreate("WRITER", "Editor")
        }
    }
}
