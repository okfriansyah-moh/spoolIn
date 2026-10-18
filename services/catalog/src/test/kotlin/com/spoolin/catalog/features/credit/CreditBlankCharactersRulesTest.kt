package com.spoolin.catalog.features.credit

import com.google.protobuf.FieldMask
import com.spoolin.support.policy.ConfigurationSources
import com.spoolin.support.policy.InputViolation
import com.spoolin.support.policy.PatchValue
import com.spoolin.support.policy.PolicySettings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin

class CreditBlankCharactersRulesTest {
    private val policy = PolicySettings.resolve(ConfigurationSources(emptyMap(), mapOf(
        "CHARACTER_NAME_MIN_CODE_POINTS" to "3", "CHARACTER_NAME_MAX_CODE_POINTS" to "4",
    )))

    @Test
    fun `blank entries clear optional characters while nonempty entries preserve order and normalization form`() {
        val mixed = request(listOf("\u00a0", " ABC ", "\t ", "Name"))
        assertEquals(listOf("ABC", "Name"), CreditValidation.validateCreate(mixed, policy).characters)
        assertEquals(emptyList<String>(), CreditValidation.validateCreate(request(listOf("", "\u00a0\t ")), policy).characters)
        val result = CreditValidation.validateCreate(request(listOf("e\u0301x")), policy)
        assertEquals(listOf("e\u0301x"), result.characters)
    }

    @Test
    fun `all blank patch explicitly clears and omitted characters remain unchanged`() {
        val patch = request(listOf(" ", "\u00a0")).toBuilder()
            .setUpdateMask(FieldMask.newBuilder().addPaths("characters")).build()
        assertEquals(PatchValue.Value(emptyList<String>()), CreditValidation.validatePatch(patch, policy).characters)
        assertEquals(PatchValue.Omitted, CreditValidation.validatePatch(request(listOf("x")), policy).characters)
        val empty = CreditValidation.validateCreate(request(listOf(" ")), policy).characters
        CreditValidation.validateCharactersForRole(false, "CREATOR", empty)
    }

    @Test
    fun `trimmed duplicate names bounds and invalid scalars still reject rather than being discarded`() {
        listOf(listOf(" Name ", "\u00a0Name"), listOf("x"), listOf("ABCDE"), listOf("\u0000"), listOf("\uD800"))
            .forEach { values -> assertThrows(InputViolation::class.java) { CreditValidation.validateCreate(request(values), policy) } }
    }

    private fun request(characters: List<String>): Spoolin.CreditWrite = Spoolin.CreditWrite.newBuilder()
        .setTitleId(ID).setPersonId(ID).setRoleId(ID).setExpectedVersion(0).addAllCharacters(characters).build()

    companion object { private const val ID = "00000000-0000-0000-0000-000000000001" }
}
