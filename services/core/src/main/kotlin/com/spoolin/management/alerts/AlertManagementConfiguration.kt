package com.spoolin.management.alerts

import com.spoolin.core.ListenerBoundary
import com.spoolin.core.features.alerts.AlertPolicy
import com.spoolin.core.features.alerts.AlertRecorder
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextType
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.core.Ordered
import org.springframework.core.env.Environment

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

    @Bean
    fun alertIngress(
        environment: Environment,
        recorder: AlertRecorder,
    ): AlertIngress = AlertIngress(environment, recorder, AlertPolicy.fromEnvironment(environment))
}
