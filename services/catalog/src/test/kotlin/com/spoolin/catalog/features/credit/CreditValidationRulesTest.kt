package com.spoolin.catalog.features.credit

import com.spoolin.support.policy.InputViolation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin.CreditWrite

class CreditValidationRulesTest {
    @Test
    fun `character names accept embedded slash as one string (AC-026)`() {
        val names =
            CreditValidation
                .validateCreate(
                    creditWrite(
                        personId = "00000000-0000-4000-8000-000000000001",
                        roleId = "00000000-0000-4000-8000-000000000002",
                        characters = listOf("Maya / Echo"),
                    ),
                ).characters
        assertEquals(listOf("Maya / Echo"), names)
    }

    @Test
    fun `duplicate trimmed character names reject (AC-027)`() {
        assertThrows(InputViolation::class.java) {
            CreditValidation.validateCreate(
                creditWrite(
                    personId = "00000000-0000-4000-8000-000000000001",
                    roleId = "00000000-0000-4000-8000-000000000002",
                    characters = listOf("Maya", "Maya"),
                ),
            )
        }
    }

    @Test
    fun `creator roles reject character payload (AC-027-C02)`() {
        assertThrows(InputViolation::class.java) {
            CreditValidation.validateCharactersForRole(
                allowsCharacters = false,
                category = "CREATOR",
                characters = listOf("Name"),
            )
        }
    }

    private fun creditWrite(
        personId: String,
        roleId: String,
        characters: List<String>,
    ): CreditWrite =
        CreditWrite
            .newBuilder()
            .setTitleId("00000000-0000-4000-8000-000000000010")
            .setPersonId(personId)
            .setRoleId(roleId)
            .addAllCharacters(characters)
            .build()
}
