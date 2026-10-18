package com.spoolin.support.maintenance

import java.io.PrintWriter
import java.net.URI
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLFeatureNotSupportedException
import java.util.Properties
import java.util.logging.Logger
import javax.sql.DataSource

private const val MAXIMUM_MAINTENANCE_SECRET_BYTES = 4096L

/** Separate protected maintenance credentials; this factory never issues DDL or grants. */
object MaintenanceDataSources {
    fun open(
        owner: String,
        url: String,
        passwordFile: Path,
    ): DataSource {
        require(owner in setOf("core", "catalog", "media"))
        val uri = URI(url.removePrefix("jdbc:"))
        require(url.startsWith("jdbc:postgresql://") && uri.userInfo == null && uri.path == "/spoolin_$owner")
        val query =
            uri.rawQuery
                .orEmpty()
                .split('&')
                .map { it.split('=', limit = 2) }
        require(query.count { it.first() == "sslmode" } == 1 && query.any { it == listOf("sslmode", "verify-full") })
        require(query.all { it.size == 2 && it.first() in setOf("sslmode", "sslrootcert", "connectTimeout") })
        require(passwordFile.isAbsolute && Files.isRegularFile(passwordFile, NOFOLLOW_LINKS))
        require(Files.size(passwordFile) in 1..MAXIMUM_MAINTENANCE_SECRET_BYTES)
        require(Files.getPosixFilePermissions(passwordFile) == PosixFilePermissions.fromString("rw-------"))
        return ProtectedSource(owner, url, passwordFile)
    }

    private class ProtectedSource(
        private val owner: String,
        private val url: String,
        private val path: Path,
    ) : DataSource {
        override fun getConnection(): Connection {
            val expected = "spoolin_${owner}_maintenance"
            val connection =
                DriverManager.getConnection(
                    url,
                    Properties().apply {
                        setProperty("user", expected)
                        setProperty("password", Files.readString(path).trimEnd('\n', '\r'))
                        setProperty("connectTimeout", "2")
                        setProperty("options", "-c search_path=appdata -c statement_timeout=10000 -c lock_timeout=2000")
                    },
                )
            var verified = false
            return try {
                check(connectionUser(connection) == expected)
                verified = true
                connection
            } finally {
                if (!verified) connection.close()
            }
        }

        private fun connectionUser(connection: Connection): String? =
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT current_user").use { rows ->
                    if (rows.next()) rows.getString(1) else null
                }
            }

        override fun getConnection(
            username: String?,
            password: String?,
        ): Connection = throw SQLFeatureNotSupportedException("Use protected owner maintenance configuration")

        override fun getLogWriter(): PrintWriter? = null

        override fun setLogWriter(out: PrintWriter?) {
            require(out == null)
        }

        override fun getLoginTimeout(): Int = 2

        override fun setLoginTimeout(seconds: Int) {
            require(seconds == 2)
        }

        override fun getParentLogger(): Logger = throw SQLFeatureNotSupportedException()

        override fun <T : Any?> unwrap(iface: Class<T>?): T = throw SQLFeatureNotSupportedException()

        override fun isWrapperFor(iface: Class<*>?): Boolean = false
    }
}
