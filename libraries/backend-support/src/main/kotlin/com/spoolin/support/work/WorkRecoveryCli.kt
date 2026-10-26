package com.spoolin.support.work

import tools.jackson.databind.json.JsonMapper
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.sql.Connection
import java.util.UUID
import kotlin.system.exitProcess

private const val RECOVERY_FAILURE_EXIT = 4

/** Process-local administrative entrypoint, invoked by the installation-owning OS user through Compose. */
object WorkRecoveryCli {
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            run(args)
        } catch (_: Exception) {
            System.err.println(
                "Work recovery refused: verify operator authority, inspected fence, " +
                    "destination evidence and protected configuration",
            )
            exitProcess(RECOVERY_FAILURE_EXIT)
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
        val operator = OperatorAccess.operatorIdentity()
        OperatorAccess.connection(owner).use { connection ->
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
        com.spoolin.support.identity.NamedIdentities.verify(connection)
        val reasonPath = Path.of("/run/spoolin-operator-reason")
        OperatorAccess.protectedFile(reasonPath)
        require(
            Files.getAttribute(reasonPath, "unix:uid", NOFOLLOW_LINKS).toString() ==
                OperatorAccess.required("SPOOLIN_OPERATOR_UID"),
        )
        connection.autoCommit = false
        try {
            val action =
                WorkRecoveryAction(
                    id,
                    values.getValue("--expected-generation").toInt(),
                    values.getValue("--expected-epoch").toLong(),
                    WorkRecoveryCommit(operator, Files.readString(reasonPath), UUID.randomUUID()),
                )
            val destination =
                java.util.ServiceLoader
                    .load(WorkRecoveryDestination::class.java)
                    .singleOrNull { it.owner == owner }
            try {
                check(
                    WorkRecoveryOperations.recover(
                        connection,
                        action,
                        Path.of(System.getenv("SPOOLIN_MEDIA_VOLUME") ?: "/var/spoolin/media"),
                        destination,
                    ),
                )
            } finally {
                destination?.close()
            }
            connection.commit()
            println("Work recovery committed; original effect identity and attempt evidence retained")
        } finally {
            connection.rollback()
        }
    }
}
