package com.spoolin.core.features.account.persistence

import com.spoolin.core.features.crypto.AccountCryptography
import com.spoolin.core.features.crypto.AeadCipher
import com.spoolin.core.features.crypto.Argon2Engine
import com.spoolin.core.features.crypto.KeyMount
import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.KeyRegistry
import com.spoolin.core.features.crypto.MountedKeys
import com.spoolin.core.features.crypto.NewPasswordPolicy
import com.spoolin.core.features.crypto.NonceAllocator
import com.spoolin.core.features.crypto.PasswordHasher
import com.spoolin.core.features.crypto.ProtectionAlert
import com.spoolin.core.features.crypto.ProtectionMetrics
import com.spoolin.core.features.policy.PolicyProvider
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
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
    fun protectionMetrics(registry: MeterRegistry): ProtectionMetrics = ProtectionMetrics(registry)

    @Bean
    fun protectionAlert(metrics: ProtectionMetrics): ProtectionAlert =
        metrics.observing(
            ProtectionAlert { row ->
                LoggerFactory.getLogger(AccountCryptography::class.java)
                    .error("Protected account unavailable row={}", row)
            },
        )

    @Bean
    fun keyRegistry(dataSource: DataSource): KeyRegistry = KeyRegistry(dataSource)

    @Bean
    fun nonceAllocator(dataSource: DataSource): NonceAllocator = NonceAllocator(dataSource)

    @Bean
    fun accountCryptography(
        registry: KeyRegistry,
        keys: MountedKeys,
        nonces: NonceAllocator,
        @Qualifier("protectionAlert") alert: ProtectionAlert,
    ): AccountCryptography = AccountCryptography(registry, keys, nonces, AeadCipher(), alert)

    @Bean
    fun protectedAccounts(
        dataSource: DataSource,
        registry: KeyRegistry,
        crypto: AccountCryptography,
        @Qualifier("protectionAlert") alert: ProtectionAlert,
        rules: IdentityRules,
    ): ProtectedAccounts = ProtectedAccounts(dataSource, registry, crypto, AccountPayload(), alert, rules)

    @Bean
    fun passwordHasher(provider: PolicyProvider): PasswordHasher {
        val bound = provider.policy.textBounds.getValue("password")
        return PasswordHasher(Argon2Engine(), NewPasswordPolicy(bound.minCodePoints, bound.maxCodePoints))
    }

    @Bean
    fun identityRules(provider: PolicyProvider): IdentityRules {
        val bound = provider.policy.textBounds.getValue("registrationName")
        return IdentityRules(
            bound.minCodePoints,
            bound.maxCodePoints,
            provider.policy.textBounds
                .getValue("email")
                .maxCodePoints,
        )
    }
}
