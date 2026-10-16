package com.spoolin.core.recovery.crypto

import com.spoolin.core.features.account.persistence.AccountProtectionConfiguration
import com.spoolin.core.features.account.persistence.IdentityRules
import com.spoolin.core.features.account.persistence.ProtectedAccounts
import com.spoolin.core.features.crypto.PasswordHasher
import com.spoolin.core.features.policy.PolicyConfiguration
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.env.MapPropertySource
import javax.sql.DataSource

class ConfigurationIntegrationTest {
    @Test
    fun enabledProductionConfigurationUsesConstructorWiringAndValidatedMountedKeys() =
        CryptoFixture().use { fixture ->
            val settings = mutableMapOf<String, Any>("spoolin.account-protection.enabled" to "true")
            fixture.mounts.forEach { (id, mount) ->
                settings["spoolin.account-protection.keys.$id.purpose"] = mount.purpose.name
                settings["spoolin.account-protection.keys.$id.file"] = mount.file.toString()
            }
            AnnotationConfigApplicationContext().use { context ->
                context.environment.propertySources.addFirst(MapPropertySource("fixture", settings))
                context.registerBean("dataSource", DataSource::class.java, java.util.function.Supplier { fixture.pool })
                context.register(AccountProtectionConfiguration::class.java, PolicyConfiguration::class.java)
                context.refresh()
                assertNotNull(context.getBean(ProtectedAccounts::class.java))
                assertNotNull(context.getBean(PasswordHasher::class.java).hashNew("synthetic valid credential"))
                assertNotNull(context.getBean(IdentityRules::class.java))
            }
        }

    @Test
    fun disabledCapabilityHasNoCredentialBeansAndEnabledMissingKeysCannotStart() {
        AnnotationConfigApplicationContext().use { context ->
            context.register(AccountProtectionConfiguration::class.java, PolicyConfiguration::class.java)
            context.refresh()
            assertFalse(context.containsBean("passwordHasher"))
        }
        AnnotationConfigApplicationContext().use { context ->
            context.environment.propertySources.addFirst(
                MapPropertySource(
                    "fixture",
                    mapOf("spoolin.account-protection.enabled" to "true"),
                ),
            )
            context.register(AccountProtectionConfiguration::class.java, PolicyConfiguration::class.java)
            assertThrows(org.springframework.beans.factory.BeanCreationException::class.java) { context.refresh() }
        }
    }
}
