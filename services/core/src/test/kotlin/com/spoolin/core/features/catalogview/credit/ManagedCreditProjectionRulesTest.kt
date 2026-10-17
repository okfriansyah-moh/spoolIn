package com.spoolin.core.features.catalogview.credit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin

class ManagedCreditProjectionRulesTest {

    @Test
    fun `credit page preserves ordered character list in projection shape`() {
        val credit = Spoolin.Credit.newBuilder()
            .setId("00000000-0000-4000-8000-000000000099")
            .setTitleId("00000000-0000-4000-8000-000000000010")
            .setPersonId("00000000-0000-4000-8000-000000000001")
            .setRoleId("b0000001-0000-4000-8000-000000000001")
            .addCharacters("Maya")
            .addCharacters("Young Maya")
            .addCharacters("Maya / Echo")
            .setVersion(2)
            .setPersonName("Alex")
            .setTitleName("Movie A")
            .setRole(
                Spoolin.CreditRole.newBuilder()
                    .setId("b0000001-0000-4000-8000-000000000001")
                    .setCategory("CAST")
                    .setLabel("Actor")
                    .setAllowsCharacters(true)
                    .build(),
            )
            .build()
        val page = Spoolin.CreditPage.newBuilder().addNodes(credit).setHasNextPage(false).build()
        assertEquals(3, page.nodesList.first().charactersList.size)
        assertEquals("Maya / Echo", page.nodesList.first().charactersList[2])
    }
}
