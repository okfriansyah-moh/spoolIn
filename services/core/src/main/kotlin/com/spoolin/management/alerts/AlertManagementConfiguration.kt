package com.spoolin.management.alerts

import com.spoolin.core.features.alerts.AlertPolicy
import com.spoolin.core.features.alerts.AlertRecorder
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextType
import org.springframework.context.annotation.Bean
import org.springframework.core.annotation.Order
import org.springframework.core.env.Environment
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

/** Imported only into the separate management child context. */
@ManagementContextConfiguration(value = ManagementContextType.CHILD, proxyBeanMethods = false)
class AlertManagementConfiguration {
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
