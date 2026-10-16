package com.spoolin.support.policy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class PolicyRequestCapacityRulesTest {
    @Test
    fun `C01 complete generated SDL movie envelope cannot carry the old title ceiling`() {
        val request = movieRequest(21843, "x")
        assertEquals(131436, request.toByteArray(Charsets.UTF_8).size)
        assertThrows(ConfigurationViolation::class.java) {
            PolicySettings.resolve(ConfigurationSources(mapOf("TITLE_MAX_CODE_POINTS" to "21843")))
        }
    }

    @Test
    fun `C01 required synopsis minimum consumes space beside the maximum title`() {
        assertEquals(131069, movieRequest(21781, "\\u0001").toByteArray(Charsets.UTF_8).size)
        assertEquals(131075, movieRequest(21782, "\\u0001").toByteArray(Charsets.UTF_8).size)
        assertEquals(
            21781,
            PolicySettings
                .resolve(ConfigurationSources(mapOf("TITLE_MAX_CODE_POINTS" to "21781")))
                .textBounds
                .getValue("title")
                .maxCodePoints,
        )
        assertThrows(ConfigurationViolation::class.java) {
            PolicySettings.resolve(ConfigurationSources(mapOf("TITLE_MAX_CODE_POINTS" to "21782")))
        }
    }

    @TestFactory
    fun `C01 independent complete request byte boundaries`() =
        CapacityVectors.all.flatMap { vector ->
            listOf(vector.upper - 1, vector.upper, vector.upper + 1).map { count ->
                dynamicTest("C01 complete ${vector.field} request with $count code points") {
                    val json = vector.request.replace("<candidate>", "\\u0001".repeat(count))
                    if (count == vector.upper) assertEquals(vector.upperBytes, json.toByteArray(Charsets.UTF_8).size)
                    if (count > vector.upper) assertEquals(vector.excessiveBytes, json.toByteArray(Charsets.UTF_8).size)
                    val input =
                        mapOf(
                            "${PolicySettings.key(vector.field)}_MAX_CODE_POINTS" to count.toString(),
                            "${PolicySettings.key(vector.field)}_MIN_CODE_POINTS" to "1",
                        )
                    if (count <= vector.upper) {
                        assertEquals(
                            count,
                            PolicySettings
                                .resolve(ConfigurationSources(input))
                                .textBounds
                                .getValue(vector.field)
                                .maxCodePoints,
                        )
                    } else {
                        assertThrows(
                            ConfigurationViolation::class.java,
                        ) { PolicySettings.resolve(ConfigurationSources(input)) }
                    }
                }
            }
        }

    @Test
    fun `C01 raised required sibling minimum reduces available synopsis capacity`() {
        val input = mapOf("TITLE_MIN_CODE_POINTS" to "100", "SYNOPSIS_MAX_CODE_POINTS" to "21682")
        assertEquals(
            21682,
            PolicySettings
                .resolve(ConfigurationSources(input))
                .textBounds
                .getValue("synopsis")
                .maxCodePoints,
        )
        assertThrows(ConfigurationViolation::class.java) {
            PolicySettings.resolve(ConfigurationSources(input + ("SYNOPSIS_MAX_CODE_POINTS" to "21683")))
        }
    }

    @Test
    fun `C01 complete request observes metadata byte boundary and query limit`() {
        val input = mapOf("TITLE_MAX_CODE_POINTS" to "21781", "REQUEST_METADATA_BYTES" to "131069")
        assertEquals(
            131069,
            PolicySettings.resolve(ConfigurationSources(input)).requestLimits.getValue("metadataBytes"),
        )
        assertThrows(ConfigurationViolation::class.java) {
            PolicySettings.resolve(ConfigurationSources(input + ("REQUEST_METADATA_BYTES" to "131068")))
        }
        assertThrows(ConfigurationViolation::class.java) {
            PolicySettings.resolve(ConfigurationSources(mapOf("REQUEST_GRAPHQL_QUERY_BYTES" to "1")))
        }
    }

    @Test
    fun `C01 a raised required minimum cannot leave the default sibling maximum unrepresentable`() {
        listOf("TITLE_MIN_CODE_POINTS", "SYNOPSIS_MIN_CODE_POINTS").forEach { key ->
            assertThrows(ConfigurationViolation::class.java) {
                PolicySettings.resolve(
                    ConfigurationSources(mapOf(key to "21781", key.replace("MIN", "MAX") to "21781")),
                )
            }
        }
    }

    private fun movieRequest(
        title: Int,
        escapedSynopsis: String,
    ): String =
        "{\"query\":\"mutation(\$id:UUID!,\$p:String!,\$input:MovieInput!){" +
            "createMovie(operationId:\$id,policyRevision:\$p,input:\$input){problem{code}}}\"," +
            "\"variables\":{\"id\":\"c1bb0e81-232b-5f83-9d73-a5273dcac001\",\"p\":\"${"0".repeat(64)}\"," +
            "\"input\":{\"title\":\"${"\\u0001".repeat(title)}\",\"synopsis\":\"$escapedSynopsis\"," +
            "\"genreIds\":[\"c1bb0e81-232b-5f83-9d73-a5273dcac002\"],\"release\":{\"year\":2026}}}}"
}
