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
import kotlin.system.exitProcess

/** Offline recovery for the prepared local Maintainer; never exposed through HTTP. */
fun runLocalMaintainerPasswordCli(argument: String) {
    try {
        require(System.getenv("SPOOLIN_LOCAL_PASSWORD_RESET") == "1") { "Local recovery must be explicitly enabled" }
        resetLocalMaintainerPassword(Path.of(argument.substringAfter('=')))
        println("Local Maintainer password updated; existing sessions revoked")
    } catch (_: Exception) {
        System.err.println(
            "Local Maintainer password recovery failed; inspect protected local configuration " +
                "and database availability",
        )
        exitProcess(4)
    }
}

private fun resetLocalMaintainerPassword(path: Path) {
    val environment = StandardEnvironment()
    val policy = PolicyConfiguration().policyProvider(environment).policy
    val names = policy.textBounds.getValue("registrationName")
    val rules =
        IdentityRules(names.minCodePoints, names.maxCodePoints, policy.textBounds.getValue("email").maxCodePoints)
    val config = InitializationConfiguration.load(path, rules)
    val account = config.accounts.single { it.role == AccountRole.MAINTAINER }
    requireProtectedFile(account.passwordFile, 4096L)
    val bounds = policy.textBounds.getValue("password")
    val password = Files.readString(account.passwordFile)
    val hash =
        PasswordHasher(Argon2Engine(), NewPasswordPolicy(bounds.minCodePoints, bounds.maxCodePoints)).use {
            it.hashNew(password)
        }
    DatabaseSettings.load(environment, "core", false).pool().use { source ->
        source.committed { connection ->
            val manifests =
                connection.queryBound(
                    "SELECT setup_manifest_hash FROM appdata.operation_receipt " +
                        "WHERE receipt_type='ACCOUNT_INITIALIZATION' AND state='SUCCEEDED' AND setup_maintainer_id=?",
                    listOf(account.id),
                ) { it.getBytes("setup_manifest_hash") }
            require(manifests.size == 1 && MessageDigest.isEqual(manifests.single(), config.checksum()))
            val changed =
                connection.executeBound(
                    "UPDATE appdata.user_account SET password_hash=?,auth_epoch=auth_epoch+1, " +
                        "version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=? AND role='MAINTAINER'",
                    hash.encoded,
                    account.id,
                )
            require(changed == 1) { "Prepared Maintainer is absent" }
            connection.executeBound(
                "UPDATE appdata.browser_session SET revoked_at=COALESCE(revoked_at,CURRENT_TIMESTAMP) " +
                    "WHERE account_id=?",
                account.id,
            )
        }
    }
}
