package com.spoolin.core.boundary.http

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration(proxyBeanMethods = false)
class BrowserConfiguration {
    @Bean
    fun browserBoundary(environment: Environment): BrowserBoundary {
        val port = environment.getProperty("HTTP_PORT", "8443")
        return BrowserBoundary(environment.getProperty("BROWSER_ORIGIN", "https://localhost:$port"))
    }
}
