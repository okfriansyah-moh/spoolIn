package com.spoolin.core.features.crypto.rotation

import com.spoolin.core.DatabaseSettings
import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.KeyRegistry
import org.springframework.core.env.StandardEnvironment

/** CLI entry for ops/bin/rotate-keys.sh --execute (T040). */
fun main(args: Array<String>) {
    var fromAlias = ""
    var toAlias = ""
    var purpose = KeyPurpose.DRAFT
    var dryRun = false
    var index = 0
    while (index < args.size) {
        when (args[index]) {
            "--from" -> fromAlias = args.getOrElse(++index) { usage() }
            "--to" -> toAlias = args.getOrElse(++index) { usage() }
            "--purpose" ->
                purpose =
                    KeyPurpose.valueOf(
                        args.getOrElse(++index) { usage() }.uppercase(),
                    )
            "--dry-run" -> dryRun = true
            else -> usage()
        }
        index += 1
    }
    if (fromAlias.isBlank() || toAlias.isBlank()) usage()
    val environment = StandardEnvironment()
    val dataSource = DatabaseSettings.load(environment, "core", false).pool()
    dataSource.use { pool ->
        val outcome =
            KeyRotationPlan(fromAlias, toAlias, purpose)
                .rotate(pool, KeyRegistry(pool), dryRun)
        println(outcome.message)
        if (!outcome.ok) kotlin.system.exitProcess(1)
    }
}

private fun usage(): Nothing {
    System.err.println(
        "usage: KeyRotationCli --from <id> --to <id> [--purpose DRAFT|PII|EMAIL_LOOKUP] [--dry-run]",
    )
    kotlin.system.exitProcess(2)
}
