package com.spoolin.core.features.connector

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ConnectorTokenRulesTest {
    @Test
    fun digest_is_32_bytes() {
        val digest = ConnectorTokenDigest.sha256("example-token".toByteArray())
        assertEquals(32, digest.size)
    }
}
