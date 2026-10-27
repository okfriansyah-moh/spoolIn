package com.spoolin.catalog

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import java.lang.reflect.Proxy
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.sql.Connection
import java.sql.Driver
import java.sql.DriverManager
import java.sql.DriverPropertyInfo
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.util.Properties
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Logger

class IdentityStartupRulesTest {
    @TempDir lateinit var root: Path

    @Test fun `startup verifies fresh migration marker before returning a usable pool`() {
        withDriver(false) { boundary ->
            val pool = Application().dataSource(environment())
            try {
                assertFalse(pool.isClosed)
                pool.connection.use { assertTrue(it.isValid(1)) }
                assertEquals(1, boundary.markerReads.get())
                assertEquals("spoolin_catalog_runtime", boundary.properties.getProperty("user"))
                assertEquals("unit-password", boundary.properties.getProperty("password"))
                assertTrue(boundary.properties.getProperty("options").contains("search_path=appdata"))
            } finally {
                pool.close()
            }
            assertTrue(pool.isClosed)
            assertTrue(boundary.closed.get() > 0)
        }
    }

    @Test fun `startup closes pool if returning the verified connection fails`() {
        withDriver(true) { boundary ->
            val failure = assertThrows(IllegalStateException::class.java) { Application().dataSource(environment()) }
            assertEquals("connection return failed", failure.message)
            assertEquals(1, boundary.markerReads.get())
            assertTrue(boundary.closed.get() > 0)
        }
    }

    private fun environment(): StandardEnvironment {
        fun secret(
            name: String,
            value: String,
        ): Path =
            root.resolve(name).also {
                Files.writeString(it, value)
                Files.setPosixFilePermissions(it, PosixFilePermissions.fromString("rw-------"))
            }
        return StandardEnvironment().also {
            it.propertySources.addFirst(
                MapPropertySource(
                    "identity-startup",
                    mapOf<String, Any>(
                        "SERVICE_KEY_FILE" to secret("key", "unit-key").toString(),
                        "CATALOG_DB_URL" to url,
                        "CATALOG_DB_USER" to "spoolin_catalog_runtime",
                        "CATALOG_DB_PASSWORD_FILE" to secret("password", "unit-password\n").toString(),
                    ),
                ),
            )
        }
    }

    private fun withDriver(
        failReturn: Boolean,
        action: (StartupJdbc) -> Unit,
    ) {
        val previous = DriverManager.drivers().toList()
        previous.forEach(DriverManager::deregisterDriver)
        val boundary = StartupJdbc(failReturn)
        DriverManager.registerDriver(boundary)
        try {
            action(boundary)
        } finally {
            DriverManager.deregisterDriver(boundary)
            previous.forEach(DriverManager::registerDriver)
        }
    }

    private class StartupJdbc(
        private val failReturn: Boolean,
    ) : Driver {
        val markerReads = AtomicInteger()
        val closed = AtomicInteger()
        val properties = Properties()

        override fun acceptsURL(value: String?) = value == url

        override fun connect(
            value: String?,
            info: Properties?,
        ): Connection? {
            if (!acceptsURL(value)) return null
            properties.putAll(requireNotNull(info))
            var physicalClosed = false
            val connection =
                proxy(Connection::class.java) { name, _ ->
                    when (name) {
                        "isValid" -> !physicalClosed
                        "isClosed" -> physicalClosed
                        "getAutoCommit" -> true
                        "getTransactionIsolation" -> Connection.TRANSACTION_READ_COMMITTED
                        "createStatement" ->
                            proxy(Statement::class.java) { method, _ ->
                                if (method ==
                                    "execute"
                                ) {
                                    true
                                } else {
                                    null
                                }
                            }
                        "prepareStatement" ->
                            proxy(PreparedStatement::class.java) { method, _ ->
                                if (method == "executeQuery") {
                                    markerReads.incrementAndGet()
                                    var row = 0
                                    proxy(ResultSet::class.java) { operation, _ ->
                                        when (operation) {
                                            "next" -> ++row == 1
                                            "getBoolean", "wasNull" -> false
                                            else -> null
                                        }
                                    }
                                } else {
                                    null
                                }
                            }
                        "clearWarnings" -> {
                            if (failReturn &&
                                markerReads.get() > 0
                            ) {
                                throw IllegalStateException("connection return failed")
                            }
                            null
                        }
                        "close" -> {
                            physicalClosed = true
                            closed.incrementAndGet()
                            null
                        }
                        else -> null
                    }
                }
            return connection
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

    companion object {
        private const val url = "jdbc:postgresql://unit-startup/spoolin_catalog?sslmode=verify-full"

        private fun <T> proxy(
            type: Class<T>,
            call: (String, Array<out Any?>?) -> Any?,
        ): T =
            type.cast(
                Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { instance, method, args ->
                    when (method.name) {
                        "toString" -> "StartupJdbc(${type.simpleName})"
                        "hashCode" -> System.identityHashCode(instance)
                        "equals" -> instance === args?.firstOrNull()
                        else ->
                            call(method.name, args) ?: when (method.returnType) {
                                java.lang.Boolean.TYPE -> false
                                java.lang.Integer.TYPE -> 0
                                java.lang.Long.TYPE -> 0L
                                else -> null
                            }
                    }
                },
            )
    }
}
