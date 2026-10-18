package com.spoolin.catalog.seed

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class SeedRoleIdentityRulesTest {
    @Test
    fun `starter source properties preserve migration identities without a label lookup`() {
        val properties = listOf("P161", "P57", "P58", "P162")
        properties.forEachIndexed { index, property ->
            val expected = UUID.fromString("b0000001-0000-4000-8000-00000000000${index + 1}")
            assertEquals(expected, SeedRoleIdentity.resolve("wikidata", property, UUID.randomUUID().toString()))
        }
    }

    @Test
    fun `unrelated source roles retain their own source identity`() {
        val sourceIdentity = UUID.randomUUID()
        assertEquals(sourceIdentity, SeedRoleIdentity.resolve("wikidata", "P999999", sourceIdentity.toString()))
        assertEquals(sourceIdentity, SeedRoleIdentity.resolve("other", "P161", sourceIdentity.toString()))
    }
}
