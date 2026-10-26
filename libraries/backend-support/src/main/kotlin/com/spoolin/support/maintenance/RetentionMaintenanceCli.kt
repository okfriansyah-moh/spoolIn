package com.spoolin.support.maintenance

import tools.jackson.databind.json.JsonMapper
import java.nio.file.Path
import kotlin.system.exitProcess

private const val MAINTENANCE_BATCH_LIMIT = 100

/** Local operator entry; bounded retention uses only the selected owner's protected maintenance credential. */
object RetentionMaintenanceCli {
    @JvmStatic
    fun main(args: Array<String>) {
        runCatching { run(args) }.fold(
            onSuccess = { println(JsonMapper.builder().build().writeValueAsString(it)) },
            onFailure = {
                System.err.println(
                    "Maintenance refused: verify owner, schedule, protected maintenance credential and verified TLS",
                )
                exitProcess(if (it is IllegalArgumentException) 2 else 1)
            },
        )
    }

    fun run(
        args: Array<String>,
        read: (String) -> String? = System::getenv,
    ): Map<String, Any> {
        val options = options(args)
        val owner = options.getValue("--owner")
        val prefix = owner.uppercase(java.util.Locale.ROOT)
        val source =
            MaintenanceDataSources.open(
                owner,
                requireNotNull(read("${prefix}_DB_URL")),
                Path.of(requireNotNull(read("${prefix}_DB_MAINTENANCE_PASSWORD_FILE"))),
            )
        source.connection.use { check(it.isValid(2)) }
        val execute = options.getValue("--mode") == "execute"
        val counts =
            if (!execute) {
                emptyMap()
            } else {
                source.connection.use(com.spoolin.support.identity.NamedIdentities::verify)
                val retention = RetentionMaintenance(source, owner)
                if (options.getValue("--schedule") == "hourly") retention.hourly() else retention.daily()
            }
        return mapOf(
            "owner" to owner,
            "schedule" to options.getValue("--schedule"),
            "batchLimit" to MAINTENANCE_BATCH_LIMIT,
            "executed" to execute,
            "counts" to counts,
        )
    }

    private fun options(args: Array<String>): Map<String, String> {
        val values =
            args.associate {
                require(it.startsWith("--") && '=' in it)
                it.substringBefore('=') to it.substringAfter('=')
            }
        require(values.size == args.size && values.keys == setOf("--owner", "--schedule", "--mode"))
        require(values["--owner"] in setOf("core", "catalog", "media"))
        require(values["--schedule"] in setOf("hourly", "daily"))
        require(values["--mode"] in setOf("inspect", "execute"))
        return values
    }
}
