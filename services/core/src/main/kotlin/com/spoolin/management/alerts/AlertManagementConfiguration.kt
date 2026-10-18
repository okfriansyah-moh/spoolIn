package com.spoolin.management.alerts

import com.spoolin.core.ListenerBoundary
import com.spoolin.core.features.alerts.AlertPolicy
import com.spoolin.core.features.alerts.AlertRecorder
import jakarta.servlet.DispatcherType
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextType
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.env.Environment
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.FilterChainProxy
import org.springframework.security.web.SecurityFilterChain
import java.util.EnumSet

/** Imported only into the separate management child context. */
@ManagementContextConfiguration(value = ManagementContextType.CHILD, proxyBeanMethods = false)
class AlertManagementConfiguration {
    @Bean
    fun managementListenerBoundary(
        environment: Environment,
        metrics: ObjectProvider<com.spoolin.support.observability.OwnerMetrics>,
    ): FilterRegistrationBean<ListenerBoundary> =
        FilterRegistrationBean(ListenerBoundary(environment, metrics.ifAvailable)).apply {
            setName("managementListenerBoundary")
            order = Ordered.HIGHEST_PRECEDENCE
            addUrlPatterns("/*")
        }

    // A parent-created registration targets its parent's proxy, which cannot see this child's machine chain.
    @Bean(name = ["springSecurityFilterChain"])
    fun managementSecurityProxy(
        @Qualifier("alertMachineSecurity") alerts: SecurityFilterChain,
        @Qualifier("httpSecurity") application: SecurityFilterChain,
    ): FilterChainProxy = FilterChainProxy(listOf(alerts, application))

    @Bean(name = ["securityFilterChainRegistration"])
    fun managementSecurityRegistration(
        @Qualifier("springSecurityFilterChain") proxy: FilterChainProxy,
        properties: SecurityFilterProperties,
    ): FilterRegistrationBean<FilterChainProxy> =
        FilterRegistrationBean(proxy).apply {
            setName("springSecurityFilterChain")
            order = properties.order
            val types = properties.dispatcherTypes.map { DispatcherType.valueOf(it.name) }
            setDispatcherTypes(types.toCollection(EnumSet.noneOf(DispatcherType::class.java)))
        }

    @Bean
    fun alertIngress(
        environment: Environment,
        recorder: AlertRecorder,
    ): AlertIngress = AlertIngress(environment, recorder, AlertPolicy.fromEnvironment(environment))

    @Bean
    @Order(0)
    fun alertMachineSecurity(http: HttpSecurity): SecurityFilterChain {
        http.securityMatcher("/internal/alerts")
        http.csrf { it.ignoringRequestMatchers("/internal/alerts") }
        http.authorizeHttpRequests { it.anyRequest().permitAll() }
        return http.build()
    }
}
