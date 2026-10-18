package com.spoolin.core.features.draft

import com.spoolin.core.boundary.graphql.PolicyGraphqlProviders
import com.spoolin.core.features.account.persistence.AccountProtectionConfiguration
import com.spoolin.core.operations.receipts.ReceiptConfiguration
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.UnsatisfiedDependencyException
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader
import org.springframework.context.annotation.ConfigurationClassPostProcessor
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment

class DraftConfigurationRulesTest {
    @Test
    fun `actual configuration registration retains drafts before or after prerequisite configurations`() {
        val prerequisites = listOf(ReceiptConfiguration::class.java, AccountProtectionConfiguration::class.java)
        listOf(listOf(DraftConfiguration::class.java) + prerequisites, prerequisites + DraftConfiguration::class.java)
            .forEach { configurations ->
                val beans = definitions(configurations)
                listOf("draftGraphql", "saveDraft", "discardDraft", "protectedDrafts", "mountedKeys", "ownerReceipts")
                    .forEach { name -> assertTrue(beans.containsBeanDefinition(name), "Required bean absent: $name") }
            }
    }

    @Test
    fun `actual Spring constructor binding refuses GraphQL providers without the required draft resolver`() {
        val beans = DefaultListableBeanFactory()
        val failure =
            assertThrows(UnsatisfiedDependencyException::class.java) {
                beans.createBean(PolicyGraphqlProviders::class.java)
            }
        assertTrue(failure.message.orEmpty().contains(DraftGraphql::class.java.name))
    }

    private fun definitions(configurations: List<Class<*>>): DefaultListableBeanFactory {
        val environment = StandardEnvironment()
        environment.propertySources.addFirst(
            MapPropertySource(
                "enabled-production-features",
                mapOf("spoolin.operations.enabled" to "true", "spoolin.account-protection.enabled" to "true"),
            ),
        )
        val beans = DefaultListableBeanFactory()
        val reader = AnnotatedBeanDefinitionReader(beans, environment)
        reader.register(*configurations.toTypedArray())
        val processor = ConfigurationClassPostProcessor()
        processor.setEnvironment(environment)
        // Inspect actual Spring bean-definition registration without opening databases or replacing owner beans.
        processor.postProcessBeanDefinitionRegistry(beans)
        return beans
    }
}
