package com.spoolin.core.features.connector

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID

class ConnectorOperatorRulesTest {
    private val account = UUID.randomUUID().toString()

    @Test
    fun `operator command requires exact client account and action fields`() {
        val valid = arrayOf("--command=list", "--account=$account", "--client=CLAUDE")
        assertEquals("CLAUDE", ConnectorOperatorCommand.parse(valid).client)
        for (invalid in listOf(
            valid + "--client=CHATGPT", valid + "--unknown=value",
            valid.map { if (it == "--client=CLAUDE") "--client=unknown" else it }.toTypedArray(),
            valid.map { if (it == "--command=list") "--command=issue" else it }.toTypedArray(),
            valid + "--credential-id=${UUID.randomUUID()}", valid + "--ttl-seconds=604801",
        )) {
            assertThrows(IllegalArgumentException::class.java) { ConnectorOperatorCommand.parse(invalid) }
        }
    }
}
