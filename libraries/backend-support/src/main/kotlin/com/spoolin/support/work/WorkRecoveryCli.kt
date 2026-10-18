package com.spoolin.support.work

import tools.jackson.databind.json.JsonMapper
import java.net.URI
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties
import java.util.UUID
import kotlin.system.exitProcess

/** Process-local administrative entrypoint, invoked by the installation-owning OS user through Compose. */
object WorkRecoveryCli {
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            run(args)
        } catch (_: Exception) {
            System.err.println(
                "Work recovery refused: verify operator authority, inspected fence, destination evidence and protected configuration",
            )
            exitProcess(4)
        }
    }

    private fun run(args: Array<String>) {
        val values =
            args.associate { argument ->
                require(argument.startsWith("--") && argument.contains('='))
                argument.substringBefore('=') to argument.substringAfter('=')
            }
        require(
            values.size == args.size &&
                values.keys.all {
                    it in setOf("--owner", "--work-id", "--mode", "--expected-generation", "--expected-epoch")
                },
        )
        val owner = requireNotNull(values["--owner"]).also { require(it in setOf("core", "catalog", "media")) }
        val id = UUID.fromString(values.getValue("--work-id"))
        val operator = operatorIdentity()
        connection(owner).use { connection ->
            val mode = values.getValue("--mode")
            when (mode) {
                "inspect" ->
                    println(
                        JsonMapper.builder().build().writeValueAsString(
                            requireNotNull(WorkRecoveryOperations.inspect(connection, id)),
                        ),
                    )
                "execute" -> execute(connection, owner, id, operator, values)
                else -> error("Select inspect or execute")
            }
        }
    }

    private fun execute(
        connection: Connection,
        owner: String,
        id: UUID,
        operator: String,
        values: Map<String, String>,
    ) {
        require(owner == "media") { "Destination reconciliation is not implemented for this owner" }
        val reasonPath = Path.of("/run/spoolin-operator-reason")
        protectedFile(reasonPath)
        require(
            Files.getAttribute(reasonPath, "unix:uid", NOFOLLOW_LINKS).toString() == required("SPOOLIN_OPERATOR_UID"),
        )
        connection.autoCommit = false
        try {
            check(
                WorkRecoveryOperations.recover(
                    connection,
                    id,
                    values.getValue("--expected-generation").toInt(),
                    values.getValue("--expected-epoch").toLong(),
                    operator,
                    Files.readString(reasonPath),
                    Path.of(System.getenv("SPOOLIN_MEDIA_VOLUME") ?: "/var/spoolin/media"),
                ),
            )
            connection.commit()
            println("Work recovery committed; original effect identity and attempt evidence retained")
        } finally {
            connection.rollback()
        }
    }

    private fun operatorIdentity(): String {
        val uid = required("SPOOLIN_OPERATOR_UID").also { require(it.matches(Regex("[0-9]{1,12}"))) }
        val directory = Path.of("/run/spoolin-operator-authority")
        require(Files.isDirectory(directory, NOFOLLOW_LINKS))
        require(Files.getPosixFilePermissions(directory) == PosixFilePermissions.fromString("rwx------"))
        require(Files.getAttribute(directory, "unix:uid", NOFOLLOW_LINKS).toString() == uid)
        return "local-uid:$uid"
    }

    private fun connection(owner: String): Connection {
        val prefix = owner.uppercase() + "_DB_"
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

    private fun protectedFile(path: Path) {
        require(Files.isRegularFile(path, NOFOLLOW_LINKS) && Files.size(path) in 1..4096)
        require(Files.getPosixFilePermissions(path) == PosixFilePermissions.fromString("rw-------"))
    }

    private fun required(name: String): String =
        requireNotNull(System.getenv(name)) { "Protected configuration missing" }
}
