package com.spoolin.core.recovery.crypto

import com.spoolin.core.boundary.bootstrap.OwnerFixture
import com.spoolin.core.features.crypto.queryBound
import org.junit.jupiter.api.Assertions.assertTrue

object NonceTlsFixture {
    fun identity(fixture: OwnerFixture): List<Any> =
        fixture.connection("core").use { connection ->
            val rows =
                connection.queryBound(
                    "SELECT current_database(), current_user, ssl FROM pg_stat_ssl WHERE pid=pg_backend_pid()",
                    emptyList(),
                ) { listOf(it.getString(1), it.getString(2), it.getBoolean(3)) }
            assertTrue(rows.isNotEmpty())
            rows.single()
        }
}
