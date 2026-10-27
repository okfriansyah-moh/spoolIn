package com.spoolin.catalog.unit

import com.spoolin.catalog.ListenerBoundary
import com.spoolin.catalog.certificateIdentity
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import java.math.BigInteger
import java.security.Principal
import java.security.PublicKey
import java.security.cert.X509Certificate
import java.util.Date

private class CatalogPeerCertificate(
    private val names: List<String> = listOf("alertmanager"),
) : X509Certificate() {
    override fun getSubjectAlternativeNames(): MutableCollection<MutableList<*>> = names.map { mutableListOf<Any>(2, it) }.toMutableList()

    override fun checkValidity() = Unit

    override fun checkValidity(date: Date) = Unit

    override fun getVersion() = 3

    override fun getSerialNumber(): BigInteger = BigInteger.ONE

    override fun getIssuerDN(): Principal = Principal { "CN=unit" }

    override fun getSubjectDN(): Principal = Principal { "CN=unit" }

    override fun getNotBefore(): Date = Date(0)

    override fun getNotAfter(): Date = Date(Long.MAX_VALUE)

    override fun getTBSCertificate() = byteArrayOf()

    override fun getSignature() = byteArrayOf()

    override fun getSigAlgName() = "Ed25519"

    override fun getSigAlgOID() = "1.3.101.112"

    override fun getSigAlgParams(): ByteArray? = null

    override fun getIssuerUniqueID(): BooleanArray? = null

    override fun getSubjectUniqueID(): BooleanArray? = null

    override fun getKeyUsage(): BooleanArray? = null

    override fun getBasicConstraints() = -1

    override fun getEncoded() = byteArrayOf()

    override fun verify(key: PublicKey) = Unit

    override fun verify(
        key: PublicKey,
        provider: String,
    ) = Unit

    override fun getPublicKey(): PublicKey? = null

    override fun hasUnsupportedCriticalExtension() = false

    override fun getCriticalExtensionOIDs(): MutableSet<String>? = null

    override fun getNonCriticalExtensionOIDs(): MutableSet<String>? = null

    override fun getExtensionValue(oid: String): ByteArray? = null

    override fun toString() = "Unit peer certificate"
}

class ListenerPeerBranchesTest {
    private val environment =
        StandardEnvironment().also {
            it.propertySources.addFirst(MapPropertySource("unit", mapOf("local.management.port" to 9091)))
        }

    @Test
    fun `certificate identity requires exactly one recognized service DNS name`() {
        assertEquals("core", certificateIdentity(CatalogPeerCertificate(listOf("core"))))
        assertNull(certificateIdentity(CatalogPeerCertificate(emptyList())))
        assertNull(certificateIdentity(CatalogPeerCertificate(listOf("unknown"))))
        assertNull(certificateIdentity(CatalogPeerCertificate(listOf("core", "media"))))
    }

    @Test
    fun `management listener admits service health and restricts metrics to prometheus`() {
        for ((path, peer, port, secure, expected) in listOf(
            listOf("/actuator/health", "core", 9091, true, 200),
            listOf("/actuator/health", "operator", 9091, true, 200),
            listOf("/actuator/prometheus", "prometheus", 9091, true, 503),
            listOf("/actuator/prometheus", "core", 9091, true, 403),
            listOf("/actuator/health", "core", 8080, true, 403),
            listOf("/actuator/health", "core", 9091, false, 403),
            listOf("/actuator/health", "unknown", 9091, true, 403),
        )) {
            val request =
                proxy<HttpServletRequest> { name, args ->
                    when (name) {
                        "getRequestURI", "getServletPath" -> path
                        "isSecure" -> secure
                        "getLocalPort" -> port
                        "getDispatcherType" -> jakarta.servlet.DispatcherType.REQUEST
                        "getAttribute" ->
                            if (args[0] ==
                                "jakarta.servlet.request.X509Certificate"
                            ) {
                                arrayOf(CatalogPeerCertificate(listOf(peer as String)))
                            } else {
                                null
                            }
                        else -> null
                    }
                }
            var status = 200
            val headers = mutableMapOf<String, String>()
            val response =
                proxy<HttpServletResponse> { name, args ->
                    when (name) {
                        "sendError" -> {
                            status = args[0] as Int
                            null
                        }
                        "setHeader" -> {
                            headers[args[0] as String] = args[1] as String
                            null
                        }
                        "isCommitted" -> false
                        else -> null
                    }
                }
            var reached = false
            ListenerBoundary(environment).doFilter(request, response, FilterChain { _, _ -> reached = true })
            assertEquals(expected, status)
            assertEquals(expected == 200, reached)
            if (expected == 200) assertEquals("no-store", headers["Cache-Control"])
        }
    }
}
