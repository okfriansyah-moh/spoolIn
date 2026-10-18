package com.spoolin.core.features.crypto.rotation

import com.spoolin.core.DatabaseSettings
import com.spoolin.core.features.account.persistence.AccountProtectionSettings
import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.KeyRegistry
import com.spoolin.core.features.crypto.MountedKeys
import com.spoolin.support.logging.applyNormalizedLogLevel
import com.spoolin.support.receipts.committed
import com.spoolin.support.receipts.executeBound
import java.util.UUID
import javax.sql.DataSource
import com.spoolin.support.work.OperatorAccess
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.StandardEnvironment
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.system.exitProcess

/** Protected operator command; results contain no key material, database addresses or exception messages. */
fun main(args: Array<String>) {
    applyNormalizedLogLevel(System.getenv("LOG_LEVEL"), "core")
    val code = try {
        val operator = OperatorAccess.operatorIdentity()
        val options = RotationCliOptions.parse(args, System.getenv("ROTATION_KEYS_FILE"))
        val environment = StandardEnvironment()
        ConfigDataEnvironmentPostProcessor.applyTo(environment)
        executeRotation(options, environment, operator)
    } catch (_: IllegalArgumentException) {
        refusal("ROTATION_INPUT_OR_CONFIGURATION_INVALID")
    } catch (_: java.io.IOException) {
        refusal("ROTATION_PROTECTED_INPUT_UNAVAILABLE")
    } catch (_: java.sql.SQLException) {
        refusal("ROTATION_DATABASE_UNAVAILABLE")
    } catch (_: RuntimeException) {
        // The process boundary must also redact framework/provider exceptions carrying connection or file details.
        refusal("ROTATION_REFUSED")
    }
    if (code != 0) exitProcess(code)
}

private fun executeRotation(options: RotationCliOptions, environment: ConfigurableEnvironment, operator: String): Int {
    val configured = Path.of(environment.getRequiredProperty("ROTATION_KEYS_FILE"))
    require(Files.isSameFile(configured, options.keys)) { "Use the application's configured rotation map" }
    val settings = Binder.get(environment).bind("spoolin.account-protection", Bindable.of(AccountProtectionSettings::class.java)).get()
    RotationKeys.load(configured).use { keys ->
        MountedKeys(settings.keys, null).use { application -> validateApplicationMaterial(application, keys, options) }
        DatabaseSettings.load(environment, "core", false).pool().use { pool ->
            val result = rotate(pool, keys, options, operator)
            println(JsonMapper.builder().build().writeValueAsString(mapOf(
                "ok" to result.ok, "purpose" to options.purpose.name, "dryRun" to options.dryRun,
                "backfilled" to result.backfilled, "remaining" to result.remaining, "message" to result.message,
            )))
            return if (result.ok) 0 else 1
        }
    }
}

private fun rotate(pool: DataSource, keys: MountedKeys, options: RotationCliOptions, operator: String): RotationOutcome {
    val plan = KeyRotationPlan(options.from, options.to, options.purpose)
    if (options.dryRun) return plan.rotate(pool, KeyRegistry(pool), true, keys, options.maxBatches)
    val audit = RotationCommandAudit(pool, options, operator)
    audit.record("AUTHORIZED", "SUCCEEDED")
    return try {
        if (options.register) RotationKeys.registerTarget(pool, keys, options.to, options.purpose)
        val result = plan.rotate(pool, KeyRegistry(pool), false, keys, options.maxBatches)
        audit.record(if (result.ok) "COMPLETED" else "INCOMPLETE", if (result.ok) "SUCCEEDED" else "FAILED", result)
        result
    } catch (failure: RuntimeException) {
        audit.record("FAILED", "FAILED")
        throw failure
    } catch (failure: java.sql.SQLException) {
        audit.record("FAILED", "FAILED")
        throw failure
    }
}

private class RotationCommandAudit(private val pool: DataSource, private val options: RotationCliOptions, private val operator: String) {
    private val correlation = UUID.randomUUID()

    fun record(phase: String, outcome: String, result: RotationOutcome? = null) {
        val changes = JsonMapper.builder().build().writeValueAsString(mapOf(
            "purpose" to options.purpose.name, "fromKeyId" to options.from, "toKeyId" to options.to,
            "phase" to phase, "maxBatches" to options.maxBatches,
            "backfilled" to result?.backfilled, "remaining" to result?.remaining,
        ))
        pool.committed { connection ->
            check(connection.executeBound("INSERT INTO operational_event(id,actor_kind,action,subject_kind,outcome,changes," +
                "correlation_id,event_type,operator_id) VALUES(?,'HUMAN','KEY_ROTATION','CRYPTO_KEY',?,?::jsonb,?,'ACTIVITY',?)",
                UUID.randomUUID(), outcome, changes, correlation, operator) == 1)
        }
    }
}

/** The mounted application registry must survive restart and agree with the reload map before WRITE activation. */
internal fun validateApplicationMaterial(application: MountedKeys, rotation: MountedKeys, options: RotationCliOptions) {
    listOf(options.from, options.to).forEach { id ->
        val actual = application.key(id, options.purpose) ?: error("Application material missing")
        try {
            val proposed = rotation.key(id, options.purpose) ?: error("Rotation material missing")
            try { check(MessageDigest.isEqual(actual, proposed)) { "Mounted material differs" } }
            finally { proposed.fill(0) }
        } finally { actual.fill(0) }
    }
}

internal data class RotationCliOptions(
    val from: String,
    val to: String,
    val purpose: KeyPurpose,
    val keys: Path,
    val dryRun: Boolean,
    val register: Boolean,
    val maxBatches: Int,
) {
    companion object {
        fun parse(args: Array<String>, defaultFile: String?): RotationCliOptions {
            val values = mutableMapOf<String, String>()
            val flags = mutableSetOf<String>()
            var index = 0
            while (index < args.size) {
                val name = args[index++]
                if (name in setOf("--dry-run", "--register-target")) {
                    require(flags.add(name)) { "Duplicate option" }
                } else {
                    require(name in setOf("--from", "--to", "--purpose", "--keys", "--max-batches")) { "Unknown option" }
                    require(index < args.size && !args[index].startsWith("--")) { "Missing option value" }
                    require(values.put(name, args[index++]) == null) { "Duplicate option" }
                }
            }
            return checked(values, flags, defaultFile)
        }

        private fun checked(values: Map<String, String>, flags: Set<String>, defaultFile: String?): RotationCliOptions {
            val from = requireNotNull(values["--from"])
            val to = requireNotNull(values["--to"])
            require(from != to && listOf(from, to).all { it.matches(Regex("[A-Za-z0-9_-]{1,64}")) })
            val purpose = KeyPurpose.valueOf(values["--purpose"] ?: "DRAFT")
            val keys = Path.of(requireNotNull(values["--keys"] ?: defaultFile))
            require(keys.isAbsolute && keys.normalize() == keys)
            val batches = values["--max-batches"] ?: "100"
            require(batches.matches(Regex("[1-9][0-9]{0,4}")) && batches.toInt() in 1..10000)
            return RotationCliOptions(from, to, purpose, keys, "--dry-run" in flags, "--register-target" in flags, batches.toInt())
        }
    }
}

private fun refusal(code: String): Int {
    System.err.println("rotation: $code; verify operator authority, mounted source/target material, registration and database readiness")
    return 4
}
