package com.spoolin.core.operations.crypto

import com.spoolin.core.DatabaseSettings
import com.spoolin.core.features.account.persistence.AccountProtectionSettings
import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.KeyRegistry
import com.spoolin.core.features.crypto.MountedKeys
import com.spoolin.core.features.crypto.rotation.KeyRotationPlan
import com.spoolin.core.features.crypto.rotation.RotationCliOptions
import com.spoolin.core.features.crypto.rotation.RotationCommandAudit
import com.spoolin.core.features.crypto.rotation.RotationKeys
import com.spoolin.core.features.crypto.rotation.RotationOutcome
import com.spoolin.support.logging.applyNormalizedLogLevel
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
import javax.sql.DataSource
import kotlin.system.exitProcess

private const val ROTATION_REFUSAL_EXIT = 4

/** Protected operator command; results contain no key material, database addresses or exception messages. */
fun main(args: Array<String>) {
    applyNormalizedLogLevel(System.getenv("LOG_LEVEL"), "core")
    val code =
        try {
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
        } catch (_: java.security.GeneralSecurityException) {
            refusal("ROTATION_CRYPTO_UNAVAILABLE")
        } catch (_: RuntimeException) {
            // The process boundary must also redact framework/provider exceptions carrying connection or file details.
            refusal("ROTATION_REFUSED")
        }
    if (code != 0) exitProcess(code)
}

private fun executeRotation(
    options: RotationCliOptions,
    environment: ConfigurableEnvironment,
    operator: String,
): Int {
    val configured = Path.of(environment.getRequiredProperty("ROTATION_KEYS_FILE"))
    require(Files.isSameFile(configured, options.keys)) { "Use the application's configured rotation map" }
    val settings =
        Binder
            .get(environment)
            .bind("spoolin.account-protection", Bindable.of(AccountProtectionSettings::class.java))
            .get()
    return RotationKeys.load(configured).use { keys ->
        validateApplicationSettings(settings, keys, options)
        rotateWithDatabase(options, environment, operator, keys)
    }
}

private fun validateApplicationSettings(
    settings: AccountProtectionSettings,
    keys: MountedKeys,
    options: RotationCliOptions,
) {
    MountedKeys(settings.keys, null).use { application -> validateApplicationMaterial(application, keys, options) }
}

private fun rotateWithDatabase(
    options: RotationCliOptions,
    environment: ConfigurableEnvironment,
    operator: String,
    keys: MountedKeys,
): Int =
    DatabaseSettings.load(environment, "core", false).pool().use { pool ->
        val result = rotate(pool, keys, options, operator)
        printOutcome(result, options)
        if (result.ok) 0 else 1
    }

private fun printOutcome(
    result: RotationOutcome,
    options: RotationCliOptions,
) {
    println(
        JsonMapper.builder().build().writeValueAsString(
            mapOf(
                "ok" to result.ok,
                "purpose" to options.purpose.name,
                "dryRun" to options.dryRun,
                "backfilled" to result.backfilled,
                "remaining" to result.remaining,
                "message" to result.message,
            ),
        ),
    )
}

private fun rotate(
    pool: DataSource,
    keys: MountedKeys,
    options: RotationCliOptions,
    operator: String,
): RotationOutcome {
    val plan = KeyRotationPlan(options.from, options.to, options.purpose)
    if (options.dryRun) return plan.rotate(pool, KeyRegistry(pool), true, keys, options.maxBatches)
    val audit = RotationCommandAudit(pool, options, operator)
    audit.record("AUTHORIZED", "SUCCEEDED")
    var recorded = false
    return try {
        if (options.register) RotationKeys.registerTarget(pool, keys, options.to, options.purpose)
        val result = plan.rotate(pool, KeyRegistry(pool), false, keys, options.maxBatches)
        audit.completed(result)
        recorded = true
        result
    } finally {
        if (!recorded) audit.record("FAILED", "FAILED")
    }
}

/** The mounted application registry must survive restart and agree with the reload map before WRITE activation. */
internal fun validateApplicationMaterial(
    application: MountedKeys,
    rotation: MountedKeys,
    options: RotationCliOptions,
) {
    listOf(options.from, options.to).forEach { id ->
        val actual = application.key(id, options.purpose) ?: error("Application material missing")
        try {
            compareMaterial(actual, rotation, id, options.purpose)
        } finally {
            actual.fill(0)
        }
    }
}

private fun compareMaterial(
    actual: ByteArray,
    rotation: MountedKeys,
    id: String,
    purpose: KeyPurpose,
) {
    val proposed = rotation.key(id, purpose) ?: error("Rotation material missing")
    try {
        check(MessageDigest.isEqual(actual, proposed)) { "Mounted material differs" }
    } finally {
        proposed.fill(0)
    }
}

private fun refusal(code: String): Int {
    System.err.println(
        "rotation: $code; verify operator authority, mounted source/target material, " +
            "registration and database readiness",
    )
    return ROTATION_REFUSAL_EXIT
}
