package com.spoolin.management.media

import com.spoolin.media.ListenerBoundary
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextType
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.core.Ordered
import org.springframework.core.env.Environment

/** Registers identity checks and owner collection in the actual separate management servlet context. */
@ManagementContextConfiguration(value = ManagementContextType.CHILD, proxyBeanMethods = false)
class OwnerManagementConfiguration {
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
}
