package com.spoolin.support.policy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

class TextRecipesTest {
    @TestFactory
    fun unicodeRecipes() =
        listOf(
            "NBSP trims to mononym" to { assertEquals("V", TextRules.ordinary("\u00a0V\u00a0", TextBound(1, 100))) },
            "composed unchanged" to { assertEquals("é", TextRules.ordinary("é", TextBound(1, 1))) },
            "decomposed counts two" to { assertEquals("e\u0301", TextRules.ordinary("e\u0301", TextBound(2, 2))) },
            "101 emoji reject" to {
                assertNamedPersonRejection("😀".repeat(101))
            },
            "exact empty person input reports named field" to { assertNamedPersonRejection("") },
            "blank reject" to {
                assertNamedPersonRejection(" \u00a0 ")
            },
            "unpaired high reject" to
                {
                    assertEquals(
                        "name",
                        assertThrows(
                            InputViolation::class.java,
                        ) { TextRules.ordinary("\ud800", TextBound(1, 100), "name") }.path,
                    )
                },
            "unpaired low reject" to {
                assertThrows(InputViolation::class.java) { TextRules.ordinary("\udc00", TextBound(1, 100)) }
                Unit
            },
            "NUL reject" to {
                assertThrows(InputViolation::class.java) { TextRules.ordinary("a\u0000b", TextBound(1, 100)) }
                Unit
            },
            "non whitespace control remains" to
                { assertEquals("\u001cV\u001c", TextRules.ordinary("\u001cV\u001c", TextBound(3, 3))) },
            "password exact" to
                {
                    assertEquals(
                        " a\u00a0 ",
                        TextRules.ordinary(" a\u00a0 ", TextBound(4, 4, normalization = "EXACT")),
                    )
                },
        ).map { (name, assertion) -> dynamicTest("ATDD-276 $name", assertion) }

    private fun assertNamedPersonRejection(value: String) {
        val error =
            assertThrows(InputViolation::class.java) {
                TextRules.ordinary(value, TextBound(1, 100), "person.name")
            }
        assertEquals("person.name", error.path)
        assertEquals("Use 1 to 100 Unicode code points.", error.correction)
    }

    @TestFactory
    fun presenceRecipes(): List<org.junit.jupiter.api.DynamicTest> {
        val bound = TextBound(1, 255, false)
        return listOf(
            dynamicTest("ATDD-278 omission preserves Cahaya without mask") {
                assertEquals(PatchValue.Omitted, TextRules.optional(null, false, bound, "originalTitle"))
            },
            *listOf<String?>(null, "", " \u00a0 ")
                .mapIndexed { index, value ->
                    dynamicTest(
                        "ATDD-278 clear recipe $index",
                    ) { assertEquals(PatchValue.Clear, TextRules.optional(value, true, bound, "originalTitle")) }
                }.toTypedArray(),
            dynamicTest("ATDD-278 nonblank changes") {
                assertEquals(PatchValue.Value("Cahaya"), TextRules.optional(" Cahaya ", true, bound, "originalTitle"))
            },
            dynamicTest("ATDD-278 blank character entries clear while nonblank order remains") {
                assertEquals(emptyList<String>(), TextRules.characters(listOf("", " ", "\u0085"), TextBound(1, 200)))
                assertEquals(
                    listOf("Maya", "Maya / Echo"),
                    TextRules.characters(listOf(" ", " Maya ", "", "Maya / Echo"), TextBound(1, 200)),
                )
            },
            dynamicTest("ATDD-278 ordered slash preserved") {
                assertEquals(
                    listOf("Maya", "Maya / Echo"),
                    TextRules.characters(listOf("Maya", "Maya / Echo"), TextBound(1, 200)),
                )
            },
            dynamicTest("ATDD-278 reverse order preserved") {
                assertEquals(
                    listOf("Maya / Echo", "Maya"),
                    TextRules.characters(listOf("Maya / Echo", "Maya"), TextBound(1, 200)),
                )
            },
            dynamicTest(
                "ATDD-278 empty required genres reject",
            ) { assertThrows(InputViolation::class.java) { TextRules.genres(emptyList()) } },
            dynamicTest(
                "ATDD-278 genres are set like",
            ) { assertEquals(setOf("a", "b"), TextRules.genres(listOf("b", "a"))) },
            dynamicTest("T006-A2 tightening retains unchanged stored value") {
                assertEquals("Cahaya", TextRules.changed("Cahaya", "Cahaya", TextBound(1, 3), "title"))
            },
            dynamicTest("T006-A2 changed overlimit rejects") {
                assertThrows(
                    InputViolation::class.java,
                ) { TextRules.changed("Cahaya", "Cahayax", TextBound(1, 3), "title") }
            },
        )
    }
}
