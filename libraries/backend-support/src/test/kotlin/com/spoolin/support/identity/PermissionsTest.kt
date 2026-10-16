package com.spoolin.support.identity

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class PermissionsTest {
    @Test
    fun `public management ownership and television decisions use verified role and owner`() {
        val account = UUID.randomUUID()
        val other = UUID.randomUUID()
        listOf(null, "VIEWER", "MAINTAINER", "Actor").forEach { role ->
            assertTrue(Permissions.PUBLIC_READ)
            assertFalse(Permissions.TELEVISION_WRITE)
            assertFalse(Permissions.owned(role, account, other))
        }
        assertTrue(Permissions.management("MAINTAINER"))
        listOf(null, "VIEWER", "Actor").forEach { assertFalse(Permissions.management(it)) }
        assertTrue(Permissions.owned("VIEWER", account, account))
        assertTrue(Permissions.owned("MAINTAINER", account, account))
        assertFalse(Permissions.owned(null, account, account))
        assertFalse(Permissions.owned("Actor", account, account))
    }
}
