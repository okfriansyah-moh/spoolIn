package com.spoolin.core.features.connector

import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import com.spoolin.support.work.OperatorAccess
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE_NEW
import java.nio.file.StandardOpenOption.WRITE
import java.nio.file.attribute.PosixFilePermissions
import java.security.SecureRandom
import java.sql.Connection
import java.sql.Timestamp
import java.util.Base64
import java.util.UUID
import kotlin.system.exitProcess

private const val TOKEN_BYTES = 32
private const val MAX_TTL_SECONDS = 604800L

/** Restricted local operator command; raw connector material is written once to a protected file only. */
object ConnectorOperatorCli {
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            val operator = OperatorAccess.operatorIdentity()
            val command = ConnectorOperatorCommand.parse(args)
            OperatorAccess.connection("core").use { connection ->
                println(JsonMapper.builder().build().writeValueAsString(execute(connection, command, operator)))
            }
        } catch (_: Exception) {
            System.err.println("Connector operation refused: verify operator authority, account/client, protected output and database")
            exitProcess(4)
        }
    }

    fun execute(
        connection: Connection,
        command: ConnectorOperatorCommand,
        operator: String,
    ): Map<String, Any?> {
        require(operator.matches(Regex("local-uid:[0-9]{1,12}")))
        connection.autoCommit = false
        var committed = false
        var created: Path? = null
        try {
            val account =
                connection
                    .queryBound(
                        "SELECT role,auth_epoch FROM user_account WHERE id=? FOR UPDATE",
                        listOf(command.account),
                    ) { it.getString("role") to it.getLong("auth_epoch") }
                    .single()
            val result =
                when (command.action) {
                    "issue" -> {
                        require(account.first == "MAINTAINER") { "Maintainer account required" }
                        val path = requireNotNull(command.output)
                        val raw =
                            Base64.getUrlEncoder().withoutPadding().encodeToString(
                                ByteArray(TOKEN_BYTES).also(SecureRandom()::nextBytes),
                            )
                        protectedOutput(path, operator.substringAfter(':'))
                        created = path
                        Files.writeString(path, raw, WRITE)
                        issue(connection, command, account.second, raw, operator)
                    }
                    "revoke" -> revoke(connection, command, operator)
                    else -> list(connection, command)
                }
            connection.commit()
            committed = true
            return result
        } finally {
            if (!committed) created?.let(Files::deleteIfExists)
            connection.rollback()
            connection.autoCommit = true
        }
    }

    private fun protectedOutput(
        path: Path,
        uid: String,
    ) {
        require(path.isAbsolute && path.fileName.toString().matches(Regex("[A-Za-z0-9][A-Za-z0-9_.-]{0,127}")))
        val parent = requireNotNull(path.parent)
        require(Files.isDirectory(parent, NOFOLLOW_LINKS))
        require(Files.getPosixFilePermissions(parent) == PosixFilePermissions.fromString("rwx------"))
        require(Files.getAttribute(parent, "unix:uid", NOFOLLOW_LINKS).toString() == uid)
        Files.createFile(path, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")))
        var owned = false
        try {
            if (Files.getAttribute(path, "unix:uid", NOFOLLOW_LINKS).toString() != uid) {
                Files.setAttribute(path, "unix:uid", uid.toInt(), NOFOLLOW_LINKS)
            }
            owned = true
        } finally {
            if (!owned) Files.deleteIfExists(path)
        }
    }

    private fun issue(
        connection: Connection,
        command: ConnectorOperatorCommand,
        epoch: Long,
        raw: String,
        operator: String,
    ): Map<String, Any?> {
        val id = UUID.randomUUID()
        val now = connection.queryBound("SELECT clock_timestamp()") { it.getTimestamp(1).toInstant() }.single()
        val expiry = now.plusSeconds(command.ttl)
        check(
            connection.executeBound(
                "INSERT INTO mcp_credential(id,token_hash,account_id,client_kind,auth_epoch,scope,expires_at) " +
                    "VALUES (?,?,?,?,?,'movie:propose movie:approve',?)",
                id,
                ConnectorTokenDigest.sha256(raw.toByteArray(Charsets.UTF_8)),
                command.account,
                command.client,
                epoch,
                Timestamp.from(expiry),
            ) == 1,
        )
        audit(connection, id, "CONNECTOR_ISSUE", operator)
        return mapOf("id" to id.toString(), "client" to command.client, "expiresAt" to expiry.toString(), "issued" to true)
    }

    private fun revoke(
        connection: Connection,
        command: ConnectorOperatorCommand,
        operator: String,
    ): Map<String, Any?> {
        val id = requireNotNull(command.credential)
        val changed =
            connection.executeBound(
                "UPDATE mcp_credential SET revoked_at=COALESCE(revoked_at,clock_timestamp()) " +
                    "WHERE id=? AND account_id=? AND client_kind=?",
                id,
                command.account,
                command.client,
            )
        require(changed == 1) { "Bound connector unavailable" }
        audit(connection, id, "CONNECTOR_REVOKE", operator)
        return mapOf("id" to id.toString(), "revoked" to true)
    }

    private fun list(
        connection: Connection,
        command: ConnectorOperatorCommand,
    ): Map<String, Any?> {
        val rows =
            connection.queryBound(
                "SELECT id,expires_at,revoked_at FROM mcp_credential WHERE account_id=? AND client_kind=? ORDER BY created_at,id LIMIT 101",
                listOf(command.account, command.client),
            ) {
                mapOf(
                    "id" to it.getObject("id").toString(),
                    "expiresAt" to it.getTimestamp("expires_at").toInstant().toString(),
                    "revoked" to (it.getTimestamp("revoked_at") != null),
                )
            }
        require(rows.size <= 100) { "Bound connector listing exceeds local command limit" }
        return mapOf("client" to command.client, "credentials" to rows)
    }

    private fun audit(
        connection: Connection,
        id: UUID,
        action: String,
        operator: String,
    ) {
        check(
            connection.executeBound(
                "INSERT INTO operational_event(id,actor_kind,action,subject_kind,subject_id,outcome," +
                    "correlation_id,event_type,operator_id) " +
                    "VALUES (?,'SERVICE',?,'MCP_CREDENTIAL',?,'SUCCEEDED',?,'ACTIVITY',?)",
                UUID.randomUUID(),
                action,
                id,
                UUID.randomUUID(),
                operator,
            ) == 1,
        )
    }
}

data class ConnectorOperatorCommand(
    val action: String,
    val account: UUID,
    val client: String,
    val output: Path? = null,
    val credential: UUID? = null,
    val ttl: Long = MAX_TTL_SECONDS,
) {
    init {
        require(action in setOf("issue", "revoke", "list") && client in setOf("CLAUDE", "CHATGPT"))
        require(ttl in 1..MAX_TTL_SECONDS)
        require((action == "issue") == (output != null) && (action == "revoke") == (credential != null))
    }

    companion object {
        fun parse(args: Array<String>): ConnectorOperatorCommand {
            val values =
                args.associate { value ->
                    require(value.startsWith("--") && value.contains('='))
                    value.substringBefore('=') to value.substringAfter('=')
                }
            require(
                values.size == args.size &&
                    values.keys.all {
                        it in setOf("--command", "--account", "--client", "--output", "--credential-id", "--ttl-seconds")
                    },
            )
            return ConnectorOperatorCommand(
                values.getValue("--command"),
                id(values.getValue("--account")),
                values.getValue("--client"),
                values["--output"]?.let(Path::of),
                values["--credential-id"]?.let(::id),
                values["--ttl-seconds"]?.toLong() ?: MAX_TTL_SECONDS,
            )
        }

        private fun id(value: String): UUID = UUID.fromString(value).also { require(it.toString() == value) }
    }
}
