package com.spoolin.core.foundation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.boot.grpc.client.autoconfigure.GrpcClientProperties
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.io.FileSystemResource
import java.nio.file.Path
import java.time.Duration

class GrpcClientConfigurationRulesTest {
    @Test
    fun `actual owner configurations bind target mutual TLS and deadlines to the pinned Boot client`() {
        val root = Path.of(System.getProperty("spoolin.repository-root"))
        val peers =
            mapOf(
                "core" to setOf("catalog", "media"),
                "catalog" to setOf("core", "media"),
                "media" to setOf("catalog"),
            )
        for ((owner, expected) in peers) {
            val environment = StandardEnvironment()
            val resource = FileSystemResource(root.resolve("services/$owner/src/main/resources/application.yaml"))
            YamlPropertySourceLoader().load(owner, resource).reversed().forEach {
                environment.propertySources.addFirst(it)
            }
            val bound = Binder.get(environment).bind("spring.grpc.client", GrpcClientProperties::class.java).get()
            assertEquals(expected, bound.channel.keys, owner)
            for ((peer, channel) in bound.channel) {
                assertEquals("dns:///$peer:9090", channel.target, "$owner to $peer")
                assertEquals(true, channel.ssl.enabled)
                assertEquals("service", channel.ssl.bundle)
                assertFalse(channel.isBypassCertificateValidation)
                val seconds = when {
                    owner == "core" || owner == "media" -> 2L
                    peer == "core" -> 1L
                    else -> 5L
                }
                assertEquals(Duration.ofSeconds(seconds), channel.default.deadline)
            }
        }
    }
}
