package com.spoolin.core.features.bootstrap

import com.spoolin.core.DatabaseSettings
import com.spoolin.core.features.account.persistence.AccountPayload
import com.spoolin.core.features.account.persistence.IdentityRules
import com.spoolin.core.features.account.persistence.ProtectedAccounts
import com.spoolin.core.features.crypto.AccountCryptography
import com.spoolin.core.features.crypto.AeadCipher
import com.spoolin.core.features.crypto.Argon2Engine
import com.spoolin.core.features.crypto.KeyRegistry
import com.spoolin.core.features.crypto.MountedKeys
import com.spoolin.core.features.crypto.NewPasswordPolicy
import com.spoolin.core.features.crypto.NonceAllocator
import com.spoolin.core.features.crypto.PasswordHasher
import com.spoolin.core.features.crypto.ProtectionAlert
import com.spoolin.core.features.policy.PolicyConfiguration
import org.springframework.core.env.StandardEnvironment
import java.nio.file.Path
import javax.sql.DataSource
import kotlin.system.exitProcess

private const val INITIALIZATION_FAILURE_EXIT = 4

fun runInitializationCli(argument: String) {
    try {
        initializeFromFile(Path.of(argument.substringAfter('=')))
        println(
            "Account initialization complete; prepared identity and role evidence verified; " +
                "existing passwords preserved",
        )
    } catch (_: Exception) {
        System.err.println(
            "Account initialization failed: inspect protected configuration, identity conflicts, keys " +
                "and database availability",
        )
        exitProcess(INITIALIZATION_FAILURE_EXIT)
    }
}

private fun initializeFromFile(path: Path) {
    val environment = StandardEnvironment()
    val policy = PolicyConfiguration().policyProvider(environment).policy
    val names = policy.textBounds.getValue("registrationName")
    val rules =
        IdentityRules(
            names.minCodePoints,
            names.maxCodePoints,
            policy.textBounds.getValue("email").maxCodePoints,
        )
    val config = InitializationConfiguration.load(path, rules)
    val passwords = policy.textBounds.getValue("password")
    DatabaseSettings.load(environment, "core", false).pool().use { source ->
        source.connection.use(com.spoolin.support.identity.NamedIdentities::verify)
        val readKeys =
            System.getenv("ACCOUNT_READ_KEYS_FILE")?.let { value ->
                require(value.isNotBlank()) { "Initialization read key configuration is empty" }
                Path.of(value).also { require(it.isAbsolute) }
            }
        val passwordPolicy = NewPasswordPolicy(passwords.minCodePoints, passwords.maxCodePoints)
        MountedKeys(InitializationReadKeys.merge(config.keys, readKeys)).use { keys ->
            initializeAccounts(source, config, keys, passwordPolicy, rules)
        }
    }
}

private fun initializeAccounts(
    source: DataSource,
    config: InitializationConfiguration,
    keys: MountedKeys,
    passwordPolicy: NewPasswordPolicy,
    rules: IdentityRules,
) {
    PasswordHasher(Argon2Engine(), passwordPolicy).use { hasher ->
        val registry = KeyRegistry(source)
        val alerts = ProtectionAlert { throw IllegalStateException("Prepared account identity unavailable") }
        val crypto = AccountCryptography(registry, keys, NonceAllocator(source), AeadCipher(), alerts)
        val accounts = ProtectedAccounts(source, registry, crypto, AccountPayload(), alerts, rules)
        AccountInitialization(source, accounts, hasher).initialize(config)
    }
}
