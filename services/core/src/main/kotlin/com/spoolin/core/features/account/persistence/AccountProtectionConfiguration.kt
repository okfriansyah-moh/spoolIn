package com.spoolin.core.features.account.persistence

import com.spoolin.core.features.crypto.AccountCryptography
import com.spoolin.core.features.crypto.AeadCipher
import com.spoolin.core.features.crypto.Argon2Engine
import com.spoolin.core.features.crypto.KeyMount
import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.KeyRegistry
import com.spoolin.core.features.crypto.MountedKeys
import com.spoolin.core.features.crypto.NonceAllocator
import com.spoolin.core.features.crypto.PasswordHasher
import com.spoolin.core.features.crypto.ProtectionAlert
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@ConfigurationProperties("spoolin.account-protection")
class AccountProtectionSettings(
    val keys: Map<String, KeyMount> = emptyMap(),
)

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = ["spoolin.account-protection.enabled"], havingValue = "true")
@EnableConfigurationProperties(AccountProtectionSettings::class)
class AccountProtectionConfiguration {
    @Bean
    fun mountedKeys(settings: AccountProtectionSettings): MountedKeys {
        require(
            settings.keys.values
                .map { it.purpose }
                .containsAll(listOf(KeyPurpose.PII, KeyPurpose.EMAIL_LOOKUP)),
        ) {
            "Account protection keys are incomplete"
        }
        return MountedKeys(settings.keys)
    }

    @Bean
    fun protectionAlert(): ProtectionAlert =
        ProtectionAlert { row ->
            LoggerFactory.getLogger(AccountCryptography::class.java).error("Protected account unavailable row={}", row)
        }

    @Bean
    fun keyRegistry(dataSource: DataSource): KeyRegistry = KeyRegistry(dataSource)

    @Bean
    fun nonceAllocator(dataSource: DataSource): NonceAllocator = NonceAllocator(dataSource)

    @Bean
    fun accountCryptography(
        registry: KeyRegistry,
        keys: MountedKeys,
        nonces: NonceAllocator,
        alert: ProtectionAlert,
    ): AccountCryptography = AccountCryptography(registry, keys, nonces, AeadCipher(), alert)

    @Bean
    fun protectedAccounts(
        dataSource: DataSource,
        registry: KeyRegistry,
        crypto: AccountCryptography,
        alert: ProtectionAlert,
    ): ProtectedAccounts = ProtectedAccounts(dataSource, registry, crypto, AccountPayload(), alert)

    @Bean
    fun passwordHasher(): PasswordHasher = PasswordHasher(Argon2Engine())

    @Bean
    fun identityRules(): IdentityRules = IdentityRules()
}
