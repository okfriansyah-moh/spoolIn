package com.spoolin.support.policy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

class DateRulesTest {
    @TestFactory
    fun dateRecipes() =
        listOf(
            dynamicTest("ATDD-279 unknown import") {
                assertEquals("UNKNOWN", PartialDate.of().precision)
                assertDate(PartialDate.of(), listOf(null, null, null, "UNKNOWN"))
            },
            dynamicTest("ATDD-279 year only") {
                val value = PartialDate.of(2024)
                assertEquals("YEAR", value.precision)
                assertNull(value.month)
                assertNull(value.day)
                assertDate(value, listOf(2024, null, null, "YEAR"))
            },
            dynamicTest("ATDD-279 month only precision") {
                val value = PartialDate.of(2024, 2)
                assertEquals("MONTH", value.precision)
                assertNull(value.day)
                assertDate(value, listOf(2024, 2, null, "MONTH"))
            },
            dynamicTest("ATDD-279 leap day") {
                assertEquals("DAY", PartialDate.of(2024, 2, 29).precision)
                assertDate(PartialDate.of(2024, 2, 29), listOf(2024, 2, 29, "DAY"))
            },
        )

    @TestFactory
    fun remainingDateRecipes() =
        listOf(
            *listOf(Triple(2023, 2, 29), Triple(null, 2, null), Triple(2024, null, 1), Triple(2024, 13, 1))
                .mapIndexed { index, (y, m, d) ->
                    dynamicTest(
                        "ATDD-279 invalid tuple $index",
                    ) { assertThrows(InputViolation::class.java) { PartialDate.of(y, m, d) } }
                }.toTypedArray(),
            *listOf(0, 10000)
                .map { year ->
                    dynamicTest(
                        "ATDD-279 rejected year $year",
                    ) { assertThrows(InputViolation::class.java) { PartialDate.of(year) } }
                }.toTypedArray(),
            *listOf(1, 9999)
                .map { year ->
                    dynamicTest("ATDD-279 accepted year $year") { assertEquals(year, PartialDate.of(year).year) }
                }.toTypedArray(),
            dynamicTest(
                "ATDD-279 manual and MCP require year",
            ) { assertThrows(InputViolation::class.java) { PartialDate.of(requireYear = true) } },
            dynamicTest("ATDD-279 determinate reversed range") {
                assertEquals(false, PartialDate.ordered(PartialDate.of(2025, 2), PartialDate.of(2024)))
            },
            dynamicTest(
                "ATDD-279 overlapping precision unknown",
            ) { assertNull(PartialDate.ordered(PartialDate.of(2024), PartialDate.of(2024, 2))) },
            dynamicTest(
                "ATDD-279 determinate order",
            ) { assertEquals(true, PartialDate.ordered(PartialDate.of(2023), PartialDate.of(2024))) },
        )

    private fun assertDate(
        value: PartialDate,
        expected: List<Any?>,
    ) {
        assertEquals(expected, listOf(value.year, value.month, value.day, value.precision))
    }
}
