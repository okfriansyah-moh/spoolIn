package com.spoolin.core.features.catalogview.lifecycle

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin

class LifecycleProjectionRulesTest {
    @Test
    fun `deletion impact projection exposes token and affected titles for confirmation`() {
        val impact =
            Spoolin.Impact
                .newBuilder()
                .setTargetId("30000000-0000-4000-8000-000000000001")
                .setToken("impact-token")
                .setAffectedCreditCount(2)
                .addAffectedTitles("The Last Light")
                .addAffectedTitles("Letters from Tomorrow")
                .build()
        val map = mapDeletionImpact(impact)
        assertEquals("30000000-0000-4000-8000-000000000001", map["targetId"])
        assertEquals("impact-token", map["token"])
        assertEquals(2, map["affectedCreditCount"])
        assertEquals(listOf("The Last Light", "Letters from Tomorrow"), map["affectedTitles"])
    }

    @Test
    fun `deletion impact projection keeps zero credit consequences explicit`() {
        val impact =
            Spoolin.Impact
                .newBuilder()
                .setTargetId("30000000-0000-4000-8000-000000000002")
                .setToken("empty-impact")
                .setAffectedCreditCount(0)
                .build()
        val map = mapDeletionImpact(impact)
        assertEquals(0, map["affectedCreditCount"])
        assertFalse((map["affectedTitles"] as List<*>).isNotEmpty())
    }
}
