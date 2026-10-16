package com.spoolin.support.receipts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.time.Instant
import java.util.UUID

class IntentRecipeRulesTest {
    @TestFactory
    fun `ordinary cutoff recipes reject equality without signature skew`() =
        listOf(
            "sessionIdle",
            "sessionAbsolute",
            "connector",
            "draft",
            "cursor",
            "impact",
            "challenge",
            "approval",
        ).flatMap { name ->
            listOf(-1L, 0L, 1L).map { micros ->
                DynamicTest.dynamicTest("$name at cutoff $micros microseconds") {
                    val cutoff = Instant.parse("2026-10-01T00:00:00Z")
                    assertEquals(micros < 0, eligible(cutoff.plusNanos(micros * 1000), cutoff))
                }
            }
        }

    @TestFactory
    fun `typed content presence ordering version and actor remain bound`() =
        variants().mapIndexed { index, changed ->
            DynamicTest.dynamicTest("intent comparison recipe $index") {
                val original = original()
                val hashes = IntentHashes(mapOf("retained" to ByteArray(32) { 19 }), "retained")
                val fingerprint = hashes.hash(original, "retained")
                val expected = if (changed.actorScope == original.actorScope) Comparison.CONFLICT else Comparison.HIDDEN
                assertEquals(
                    expected,
                    hashes.compare(changed, original.actorScope, original.kind, fingerprint, "retained"),
                )
            }
        }

    private fun original() =
        OperationIntent(
            UUID.fromString("5c3b73a8-03b8-4c2d-8ca1-e5d48a842d76"),
            "account:A",
            "WriteMovie",
            "policyP",
            listOf("version" to "5", "clear" to "", "characters" to "one,two", "title" to "é"),
        )

    private fun variants(): List<OperationIntent> {
        val base = original()
        return listOf(
            base.copy(actorScope = "account:B"),
            base.copy(kind = "DeleteMovie"),
            base.copy(policyRevision = "policyQ"),
            base.copy(fields = base.fields + ("extra" to null)),
            base.copy(fields = base.fields.filterNot { it.first == "clear" }),
            base.copy(fields = base.fields.map { if (it.first == "clear") it.first to null else it }),
            base.copy(fields = base.fields.map { if (it.first == "version") it.first to "6" else it }),
            base.copy(fields = base.fields.map { if (it.first == "version") it.first to null else it }),
            base.copy(fields = base.fields.filterNot { it.first == "version" }),
            base.copy(fields = base.fields.map { if (it.first == "characters") it.first to "two,one" else it }),
            base.copy(fields = base.fields.map { if (it.first == "title") it.first to "é" else it }),
            base.copy(fields = base.fields.map { if (it.first == "title") it.first to "É" else it }),
            base.copy(fields = base.fields.map { if (it.first == "title") it.first to "é " else it }),
            base.copy(fields = base.fields.map { if (it.first == "characters") it.first to "one" else it }),
            base.copy(fields = base.fields + ("submissionNonce" to "other")),
            base.copy(actorScope = "anonymous:submissionB"),
        )
    }
}
