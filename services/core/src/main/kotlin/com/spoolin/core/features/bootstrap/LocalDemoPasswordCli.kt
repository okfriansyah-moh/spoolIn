package com.spoolin.core.features.bootstrap

import com.spoolin.core.DatabaseSettings
import com.spoolin.core.features.account.persistence.AccountRole
import com.spoolin.core.features.account.persistence.IdentityRules
import com.spoolin.core.features.crypto.Argon2Engine
import com.spoolin.core.features.crypto.NewPasswordPolicy
import com.spoolin.core.features.crypto.PasswordHasher
import com.spoolin.core.features.policy.PolicyConfiguration
import com.spoolin.core.requireProtectedFile
import com.spoolin.support.receipts.committed
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import org.springframework.core.env.StandardEnvironment
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.sql.Connection
import kotlin.system.exitProcess

/** Offline recovery for prepared local demo accounts; never exposed through HTTP. */
fun runLocalDemoPasswordCli(
    argument: String,
    roles: Set<AccountRole> = setOf(AccountRole.MAINTAINER),
) {
    try {
        require(System.getenv("SPOOLIN_LOCAL_PASSWORD_RESET") == "1") { "Local recovery must be explicitly enabled" }
        resetLocalDemoPasswords(Path.of(argument.substringAfter('=')), roles)
        println("Prepared local account passwords updated; existing sessions revoked")
    } catch (_: Exception) {
        System.err.println(
            "Local account password recovery failed; inspect protected local configuration " +
                "and database availability",
        )
        exitProcess(4)
    }
}

private fun resetLocalDemoPasswords(path: Path, roles: Set<AccountRole>) {
    val environment = StandardEnvironment()
    val policy = PolicyConfiguration().policyProvider(environment).policy
    val names = policy.textBounds.getValue("registrationName")
    val rules =
        IdentityRules(names.minCodePoints, names.maxCodePoints, policy.textBounds.getValue("email").maxCodePoints)
    val config = InitializationConfiguration.load(path, rules)
    val bounds = policy.textBounds.getValue("password")
    val hashes =
        PasswordHasher(Argon2Engine(), NewPasswordPolicy(bounds.minCodePoints, bounds.maxCodePoints)).use { hasher ->
            config.accounts.filter { it.role in roles }.map { account ->
                requireProtectedFile(account.passwordFile, 4096L)
                account to hasher.hashNew(Files.readString(account.passwordFile))
            }
        }
    DatabaseSettings.load(environment, "core", false).pool().use { source ->
        source.committed { connection ->
            requireInitializedAccounts(connection, config)
            hashes.forEach { (account, hash) ->
                val changed =
                    connection.executeBound(
                        "UPDATE appdata.user_account SET password_hash=?,auth_epoch=auth_epoch+1, " +
                            "version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=? AND role=?",
                        hash.encoded,
                        account.id,
                        account.role.name,
                    )
                require(changed == 1) { "Prepared account is absent" }
                connection.executeBound(
                    "UPDATE appdata.browser_session SET revoked_at=COALESCE(revoked_at,CURRENT_TIMESTAMP) " +
                        "WHERE account_id=?",
                    account.id,
                )
            }
        }
    }
}

private fun requireInitializedAccounts(connection: Connection, config: InitializationConfiguration) {
    val viewer = config.accounts.single { it.role == AccountRole.VIEWER }
    val maintainer = config.accounts.single { it.role == AccountRole.MAINTAINER }
    val manifests =
        connection.queryBound(
            "SELECT setup_manifest_hash FROM appdata.operation_receipt " +
                "WHERE receipt_type='ACCOUNT_INITIALIZATION' AND state='SUCCEEDED' " +
                "AND setup_viewer_id=? AND setup_maintainer_id=?",
            listOf(viewer.id, maintainer.id),
        ) { it.getBytes("setup_manifest_hash") }
    require(manifests.size == 1 && MessageDigest.isEqual(manifests.single(), config.checksum()))
}
