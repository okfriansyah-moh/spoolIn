package com.spoolin.core.coveragehttp

import com.spoolin.core.AdmissionReadinessConfiguration
import com.spoolin.core.ListenerBoundary
import com.spoolin.core.ObservabilityConfiguration
import com.spoolin.core.ReadinessLifecycle
import com.spoolin.core.RuntimeConfiguration
import com.spoolin.core.SchemaReadiness
import com.spoolin.core.ServiceIdentityInterceptor
import com.spoolin.core.certificateIdentity
import com.spoolin.support.observability.OwnerMetricRuntime
import com.spoolin.support.observability.OwnerMetrics
import io.grpc.Attributes
import io.grpc.Grpc
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.Status
import io.grpc.health.v1.HealthCheckRequest
import io.grpc.health.v1.HealthCheckResponse
import io.grpc.health.v1.HealthGrpc
import io.grpc.protobuf.services.HealthStatusManager
import io.grpc.stub.StreamObserver
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.support.StaticListableBeanFactory
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import spoolin.v1.IdentityServiceGrpc
import java.sql.SQLException
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.SSLSession
import javax.sql.DataSource

class ManagementBoundaryRulesTest {
    @Test
    fun `management listener requires secure configured port and exact certificate purpose`() {
        val cases =
            listOf(
                Triple("/actuator/health", "operator", 200),
                Triple("/actuator/health", "core", 200),
                Triple("/actuator/health", "unknown", 403),
                Triple("/actuator/prometheus", "prometheus", 503),
                Triple("/actuator/prometheus", "operator", 403),
                Triple("/internal/alerts", "alertmanager", 200),
                Triple("/internal/alerts", "prometheus", 403),
            )
        for ((path, identity, status) in cases) {
            val request = management(path, identity)
            val response = MockHttpServletResponse()
            var calls = 0
            ListenerBoundary(environment()).doFilter(request, response, FilterChain { _, _ -> calls++ })
            assertEquals(status, response.status)
            assertEquals(if (status == 200) 1 else 0, calls)
        }
        for (request in listOf(
            management("/actuator/health", "operator").also { it.isSecure = false },
            management("/actuator/health", "operator").also {
                it.localPort =
                    8080
            },
            management("/actuator/health", "operator").also {
                it.setAttribute("jakarta.servlet.request.X509Certificate", arrayOf(AlertCertificate(listOf("operator", "core"))))
            },
        )) {
            val response = MockHttpServletResponse()
            ListenerBoundary(environment()).doFilter(request, response, FilterChain { _, _ -> error("Denied request dispatched") })
            assertEquals(403, response.status)
        }
        assertNull(certificateIdentity(AlertCertificate(listOf("unknown"))))
        assertNull(certificateIdentity(AlertCertificate(listOf("core", "catalog"))))
        assertEquals("mcp", certificateIdentity(AlertCertificate(listOf("mcp"))))
    }

    @Test
    fun `failed metrics collection refuses scrape while preserving public response protections`() {
        val registry = SimpleMeterRegistry()
        try {
            val metrics = OwnerMetrics(registry, unavailableDatabase(), "core", OwnerMetricRuntime({ 0 }, { 8 }, { true }))
            val response = MockHttpServletResponse()
            ListenerBoundary(environment(), metrics).doFilter(
                management("/actuator/prometheus", "prometheus"),
                response,
                FilterChain {
                    _,
                    _,
                    ->
                    error("Failed scrape dispatched")
                },
            )
            assertEquals(503, response.status)
            assertEquals("no-store", response.getHeader("Cache-Control"))
            assertEquals("nosniff", response.getHeader("X-Content-Type-Options"))
        } finally {
            registry.close()
        }
    }

    @Test
    fun `grpc method authorization uses presented peer identity and denies absent ambiguous or unverified peers`() {
        for ((names, allowed) in listOf(
            listOf("catalog") to true,
            listOf("core") to false,
            listOf("catalog", "core") to false,
            emptyList<String>() to false,
        )) {
            val interceptor = ServiceIdentityInterceptor()
            val call = PeerCall(IdentityServiceGrpc.getCheckActorMethod(), names)
            var dispatched = false
            interceptor.interceptCall(
                call,
                Metadata(),
                ServerCallHandler {
                    _,
                    _,
                    ->
                    dispatched = true
                    object : ServerCall.Listener<SpoolinCheck>() {}
                },
            )
            assertEquals(allowed, dispatched)
            assertEquals(if (allowed) null else Status.Code.PERMISSION_DENIED, call.failure?.code)
            assertEquals(if (allowed) 1L else 0L, interceptor.dispatched.get())
        }
        val invalidPeer = PeerCall(IdentityServiceGrpc.getCheckActorMethod(), null)
        ServiceIdentityInterceptor().interceptCall(
            invalidPeer,
            Metadata(),
            ServerCallHandler {
                _,
                _,
                ->
                error("Unverified peer dispatched")
            },
        )
        assertEquals(Status.Code.PERMISSION_DENIED, invalidPeer.failure!!.code)
    }

    @Test
    fun `schema unavailable fails startup and keeps configured health services nonserving`() {
        val runtime = RuntimeConfiguration()
        val schema = SchemaReadiness(unavailableDatabase(), "1", "2")
        assertFalse(schema.ready())
        assertEquals("DOWN", schema.health().status.code)
        assertThrows(IllegalArgumentException::class.java) { SchemaReadiness(unavailableDatabase(), "3", "2") }
        val manager = runtime.healthStatus()
        val lifecycle = ReadinessLifecycle(schema, manager)
        for (service in listOf(
            "",
            "spoolin.v1.IdentityService",
            "spoolin.v1.McpGatewayService",
        )) {
            assertEquals(HealthCheckResponse.ServingStatus.NOT_SERVING, status(manager, service))
        }
        lifecycle.refresh()
        assertThrows(IllegalStateException::class.java) { lifecycle.run(DefaultApplicationArguments()) }
        lifecycle.stop()
        assertEquals(HealthCheckResponse.ServingStatus.NOT_SERVING, status(manager, ""))
        assertEquals(
            "grpc.health.v1.Health",
            runtime
                .standardHealth(manager)
                .bindService()
                .serviceDescriptor.name,
        )
    }

    @Test
    fun `admission readiness fails closed only when protection or rate admission is configured`() {
        val beans = StaticListableBeanFactory()
        val rates = beans.getBeanProvider(com.spoolin.core.features.ratelimit.RateMaintenance::class.java)
        val config = AdmissionReadinessConfiguration()
        assertEquals("UP", requireNotNull(config.rateReadiness(StandardEnvironment(), rates).health()).status.code)
        for (name in listOf("spoolin.account-protection.enabled", "spoolin.rate-limit.enabled")) {
            val environment = StandardEnvironment().also { it.propertySources.addFirst(MapPropertySource("fixture", mapOf(name to true))) }
            val health = requireNotNull(config.rateReadiness(environment, rates).health())
            assertEquals("DOWN", health.status.code)
            assertEquals("ADMISSION_UNAVAILABLE", health.details["reason"])
        }
        val registration = RuntimeConfiguration().listenerBoundary(environment(), beans.getBeanProvider(OwnerMetrics::class.java))
        assertTrue(registration.urlPatterns.contains("/*"))
        assertTrue(registration.filter is ListenerBoundary)
        assertNull(ObservabilityConfiguration().notificationDispatcher(unavailableDatabase(), StandardEnvironment()))
    }

    private fun status(
        manager: HealthStatusManager,
        service: String,
    ): HealthCheckResponse.ServingStatus {
        var result: HealthCheckResponse? = null
        (manager.healthService as HealthGrpc.HealthImplBase).check(
            HealthCheckRequest.newBuilder().setService(service).build(),
            object : StreamObserver<HealthCheckResponse> {
                override fun onNext(value: HealthCheckResponse) {
                    result = value
                }

                override fun onError(error: Throwable): Unit = throw error

                override fun onCompleted() = Unit
            },
        )
        return requireNotNull(result).status
    }

    private fun environment() =
        StandardEnvironment().also {
            it.propertySources.addFirst(
                MapPropertySource(
                    "fixture",
                    mapOf("local.management.port" to 9090),
                ),
            )
        }

    private fun management(
        path: String,
        identity: String,
    ) = MockHttpServletRequest("GET", path).also {
        it.isSecure = true
        it.localPort = 9090
        it.setAttribute("jakarta.servlet.request.X509Certificate", arrayOf(AlertCertificate(listOf(identity))))
    }

    private fun unavailableDatabase(): DataSource =
        httpProxy { method, _ ->
            if (method ==
                "getConnection"
            ) {
                throw SQLException("Unit database unavailable")
            } else {
                null
            }
        }

    private class PeerCall(
        method: MethodDescriptor<SpoolinCheck, spoolin.v1.Spoolin.Actor>,
        private val names: List<String>?,
    ) : ServerCall<SpoolinCheck, spoolin.v1.Spoolin.Actor>() {
        private val descriptor = method
        var failure: Status? = null

        override fun getMethodDescriptor() = descriptor

        override fun getAttributes(): Attributes {
            val session: SSLSession =
                httpProxy { method, _ ->
                    if (method == "getPeerCertificates") {
                        if (names == null) throw SSLPeerUnverifiedException("Unit peer unavailable")
                        arrayOf(AlertCertificate(names))
                    } else {
                        null
                    }
                }
            return Attributes.newBuilder().set(Grpc.TRANSPORT_ATTR_SSL_SESSION, session).build()
        }

        override fun request(count: Int) = Unit

        override fun sendHeaders(headers: Metadata) = Unit

        override fun sendMessage(message: spoolin.v1.Spoolin.Actor) = Unit

        override fun close(
            status: Status,
            trailers: Metadata,
        ) {
            failure = status
        }

        override fun isCancelled() = false
    }
}

private typealias SpoolinCheck = spoolin.v1.Spoolin.CheckActorRequest
