package com.spoolin.core.features.ratelimit

import com.spoolin.core.boundary.http.BrowserAdmission
import com.spoolin.core.features.account.application.AuthenticationAdmission
import com.spoolin.core.operations.receipts.OperationQueries
import com.spoolin.core.requireProtectedFile
import com.spoolin.support.identity.RateProfiles
import com.spoolin.support.policy.ConfigurationSources
import com.spoolin.support.policy.OperationalSettings
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import java.nio.file.Files
import java.nio.file.Path
import javax.sql.DataSource

private const val RATE_SECRET_BYTES = 32L

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = ["spoolin.rate-limit.enabled"], havingValue = "true")
class RateConfiguration {
    @Bean
    fun rateProfiles(environment: Environment): RateProfiles {
        val selected =
            OperationalSettings.rate
                .mapNotNull { setting ->
                    environment.getProperty(setting.name)?.let { setting.name to it }
                }.toMap()
        return RateProfiles(ConfigurationSources(selected))
    }

    @Bean
    fun rateAgreement(
        environment: Environment,
        profiles: RateProfiles,
    ): RateAgreement {
        val secret = secret(environment)
        return try {
            RateAgreement(
                Path.of(environment.getRequiredProperty("spoolin.rate-limit.agreement-file")),
                profiles.revision,
                secret,
            )
        } finally {
            secret.fill(0)
        }
    }

    @Bean
    fun rateAdmission(
        dataSource: DataSource,
        environment: Environment,
        profiles: RateProfiles,
        agreement: RateAgreement,
    ): RateAdmission {
        val secret = secret(environment)
        return try {
            RateAdmission(dataSource, profiles, secret, agreement::check)
        } finally {
            secret.fill(0)
        }
    }

    @Bean
    fun admissionHealth(
        @org.springframework.beans.factory.annotation.Qualifier("maintenanceDataSource") dataSource: DataSource,
        agreement: RateAgreement,
    ): RateMaintenance = RateMaintenance(dataSource, agreement)

    @Bean
    fun admission(trusted: TrustedAdmission): AuthenticationAdmission = AuthenticationAdmission(trusted::check)

    @Bean
    fun browserAdmission(
        trusted: TrustedAdmission,
        operations: ObjectProvider<OperationQueries>,
    ): BrowserAdmission = BrowserAdmission(trusted, operations.ifAvailable)

    @Bean
    fun trustedAdmission(limiter: RateAdmission): TrustedAdmission = TrustedAdmission(limiter)
}

private fun secret(environment: Environment): ByteArray =
    try {
        val path = Path.of(environment.getRequiredProperty("RATE_LIMIT_KEY_FILE"))
        requireProtectedFile(path, RATE_SECRET_BYTES)
        Files.readAllBytes(path).also { require(it.size.toLong() == RATE_SECRET_BYTES) }
    } catch (_: Exception) {
        throw RateUnavailable()
    }
