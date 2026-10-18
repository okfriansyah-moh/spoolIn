package com.spoolin.core.features.crypto.rotation

import com.spoolin.core.DatabaseSettings
import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.KeyRegistry
import org.springframework.core.env.StandardEnvironment
import java.nio.file.Path

/** Operator entry; protected key files are required even when only validating a dry run. */
fun main(args: Array<String>) {
    val options = arguments(args)
    val dataSource = DatabaseSettings.load(StandardEnvironment(), "core", false).pool()
    dataSource.use { pool ->
        RotationKeys.load(options.keys).use { keys ->
            if (options.register && !options.dryRun) RotationKeys.registerTarget(pool, keys, options.to, options.purpose)
            val result =
                KeyRotationPlan(
                    options.from,
                    options.to,
                    options.purpose,
                ).rotate(pool, KeyRegistry(pool), options.dryRun, keys, options.maxBatches)
            println("${result.message}; backfilled=${result.backfilled}; remaining=${result.remaining}")
            if (!result.ok) kotlin.system.exitProcess(1)
        }
    }
}

private data class Arguments(
    val from: String,
    val to: String,
    val purpose: KeyPurpose,
    val keys: Path,
    val dryRun: Boolean,
    val register: Boolean,
    val maxBatches: Int,
)

private fun arguments(args: Array<String>): Arguments {
    var from = ""
    var to = ""
    var purpose = KeyPurpose.DRAFT
    var keyFile = System.getenv("ROTATION_KEYS_FILE")
    var dryRun = false
    var register = false
    var batches = 100
    var index = 0

    fun value(): String = args.getOrElse(++index) { usage() }
    while (index < args.size) {
        when (args[index]) {
            "--from" -> from = value()
            "--to" -> to = value()
            "--purpose" -> purpose = KeyPurpose.valueOf(value().uppercase(java.util.Locale.ROOT))
            "--keys" -> keyFile = value()
            "--max-batches" -> batches = value().toInt()
            "--dry-run" -> dryRun = true
            "--register-target" -> register = true
            else -> usage()
        }
        index += 1
    }
    if (from.isBlank() || to.isBlank() || keyFile.isNullOrBlank()) usage()
    return Arguments(from, to, purpose, Path.of(requireNotNull(keyFile)), dryRun, register, batches)
}

private fun usage(): Nothing {
    System.err.println(
        "usage: KeyRotationCli --from ID --to ID --purpose DRAFT|PII|EMAIL_LOOKUP --keys /protected/map.json [--register-target] [--max-batches 100] [--dry-run]",
    )
    kotlin.system.exitProcess(2)
}
