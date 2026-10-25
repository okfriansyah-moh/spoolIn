package com.spoolin.support.maintenance

import com.spoolin.support.fixtures.JdbcBoundary
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.sql.Connection
import java.sql.Driver
import java.sql.DriverManager
import java.sql.DriverPropertyInfo
import java.sql.SQLFeatureNotSupportedException
import java.util.Properties
import java.util.logging.Logger

class ProtectedMaintenanceRulesTest {
    @TempDir lateinit var root: Path
    private val url = "jdbc:postgresql://unit-maintenance/spoolin_media?sslmode=verify-full"

    private fun secret(): Path =
        root.resolve("password").also {
            Files.writeString(it, "unit-secret\n")
            Files.setPosixFilePermissions(it, PosixFilePermissions.fromString("rw-------"))
        }

    private fun <T> driver(
        db: JdbcBoundary,
        action: (Properties) -> T,
    ): T {
        val previous = DriverManager.drivers().toList()
        previous.forEach(DriverManager::deregisterDriver)
        val observed = Properties()
        val fake =
            object : Driver {
                override fun acceptsURL(value: String?) = value == url

                override fun connect(
                    value: String?,
                    info: Properties?,
                ): Connection? {
                    if (!acceptsURL(value)) return null
                    observed.putAll(info!!)
                    return db.connection
                }

                override fun getPropertyInfo(
                    value: String?,
                    info: Properties?,
                ) = emptyArray<DriverPropertyInfo>()

                override fun getMajorVersion() = 1

                override fun getMinorVersion() = 0

                override fun jdbcCompliant() = false

                override fun getParentLogger(): Logger = Logger.getAnonymousLogger()
            }
        DriverManager.registerDriver(fake)
        return try {
            action(observed)
        } finally {
            DriverManager.deregisterDriver(fake)
            previous.forEach(DriverManager::registerDriver)
        }
    }

    @Test fun `protected maintenance source verifies actual connection role and bounds connection settings`() {
        val db = JdbcBoundary().rows(mapOf("1" to "spoolin_media_maintenance"))
        driver(db) { properties ->
            val source = MaintenanceDataSources.open("media", url, secret())
            source.connection.use { assertSame(db.connection, it) }
            assertEquals("spoolin_media_maintenance", properties.getProperty("user"))
            assertEquals("unit-secret", properties.getProperty("password"))
            assertEquals("2", properties.getProperty("connectTimeout"))
            assertTrue(properties.getProperty("options").contains("statement_timeout=10000"))
            assertNull(source.logWriter)
            source.logWriter = null
            source.loginTimeout = 2
            assertEquals(2, source.loginTimeout)
            assertFalse(source.isWrapperFor(Connection::class.java))
            assertThrows(SQLFeatureNotSupportedException::class.java) { source.getConnection("user", "password") }
            assertThrows(SQLFeatureNotSupportedException::class.java) { source.unwrap(Connection::class.java) }
            assertThrows(SQLFeatureNotSupportedException::class.java) { source.parentLogger }
            assertThrows(IllegalArgumentException::class.java) { source.loginTimeout = 10 }
        }
    }

    @Test fun `a mismatched actual role closes the connection instead of exposing maintenance access`() {
        val db = JdbcBoundary().rows(mapOf("1" to "spoolin_media_runtime"))
        driver(db) {
            val source = MaintenanceDataSources.open("media", url, secret())
            assertThrows(IllegalStateException::class.java) { source.connection }
            assertEquals(1, db.closed)
        }
    }

    @Test fun `maintenance configuration rejects wrong owner URL options file modes and symlinks`() {
        val password = secret()
        for (bad in listOf(
            "jdbc:postgresql://unit-maintenance/spoolin_core?sslmode=verify-full",
            "jdbc:postgresql://unit-maintenance/spoolin_media?sslmode=disable",
            "$url&user=wrong",
            "jdbc:postgresql://user@unit-maintenance/spoolin_media?sslmode=verify-full",
        )) {
            assertThrows(IllegalArgumentException::class.java) { MaintenanceDataSources.open("media", bad, password) }
        }
        assertThrows(IllegalArgumentException::class.java) { MaintenanceDataSources.open("other", url, password) }
        Files.setPosixFilePermissions(password, PosixFilePermissions.fromString("rw-r--r--"))
        assertThrows(IllegalArgumentException::class.java) { MaintenanceDataSources.open("media", url, password) }
        Files.setPosixFilePermissions(password, PosixFilePermissions.fromString("rw-------"))
        val link = root.resolve("link")
        Files.createSymbolicLink(link, password)
        assertThrows(IllegalArgumentException::class.java) { MaintenanceDataSources.open("media", url, link) }
    }

    @Test fun `maintenance CLI validates complete options before requesting protected credentials`() {
        val invalid =
            listOf(
                emptyArray(),
                arrayOf("owner=media"),
                arrayOf("--owner=media", "--schedule=hourly"),
                arrayOf("--owner=media", "--owner=media", "--schedule=hourly", "--mode=inspect"),
                arrayOf("--owner=other", "--schedule=hourly", "--mode=inspect"),
                arrayOf("--owner=media", "--schedule=always", "--mode=inspect"),
                arrayOf("--owner=media", "--schedule=hourly", "--mode=unknown"),
            )
        for (args in invalid) {
            assertThrows(IllegalArgumentException::class.java) {
                RetentionMaintenanceCli.run(args) { error("must not read credentials") }
            }
        }
    }
}
