package com.spoolin.support.work

import java.net.URI
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties
import java.util.UUID

interface WorkRecoveryDestination : AutoCloseable {
    override fun close() = Unit

    val owner: String

    fun validate(
        connection: Connection,
        work: WorkRecoveryInspection,
    )
}

data class WorkRecoveryAction(
    val id: UUID,
    val expectedGeneration: Int,
    val expectedEpoch: Long,
    val commit: WorkRecoveryCommit,
)

/** Protected process-local access; no HTTP administrative endpoint or database credential output. */
object OperatorAccess {
    fun operatorIdentity(): String {
        val uid = required("SPOOLIN_OPERATOR_UID").also { require(it.matches(Regex("[0-9]{1,12}"))) }
        val directory = Path.of("/run/spoolin-operator-authority")
        require(Files.isDirectory(directory, NOFOLLOW_LINKS))
        require(Files.getPosixFilePermissions(directory) == PosixFilePermissions.fromString("rwx------"))
        require(Files.getAttribute(directory, "unix:uid", NOFOLLOW_LINKS).toString() == uid)
        return "local-uid:$uid"
    }

    fun connection(owner: String): Connection {
        require(owner in setOf("core", "catalog", "media"))
        val prefix = owner.uppercase(java.util.Locale.ROOT) + "_DB_"
        val url = required(prefix + "URL")
        val uri = URI(url.removePrefix("jdbc:"))
        require(url.startsWith("jdbc:postgresql://") && uri.userInfo == null && uri.path == "/spoolin_$owner")
        val parameters =
            uri.rawQuery
                .orEmpty()
                .split('&')
                .map { it.split('=', limit = 2) }
        require(
            parameters.count { it.first() == "sslmode" } == 1 &&
                parameters.any { it == listOf("sslmode", "verify-full") },
        )
        require(parameters.all { it.size == 2 && it.first() in setOf("sslmode", "sslrootcert", "connectTimeout") })
        val user = required(prefix + "USER").also { require(it == "spoolin_${owner}_runtime") }
        val password = Path.of(required(prefix + "PASSWORD_FILE"))
        protectedFile(password)
        return DriverManager.getConnection(
            url,
            Properties().apply {
                setProperty("user", user)
                setProperty("password", Files.readString(password).trimEnd('\n', '\r'))
                setProperty("connectTimeout", "2")
                setProperty("options", "-c search_path=appdata -c statement_timeout=5000 -c lock_timeout=2000")
            },
        )
    }

    private const val MAXIMUM_SECRET_BYTES = 4096L

    fun protectedFile(path: Path) {
        require(Files.isRegularFile(path, NOFOLLOW_LINKS) && Files.size(path) in 1..MAXIMUM_SECRET_BYTES)
        require(Files.getPosixFilePermissions(path) == PosixFilePermissions.fromString("rw-------"))
    }

    fun required(name: String): String = requireNotNull(System.getenv(name)) { "Protected configuration missing" }
}
