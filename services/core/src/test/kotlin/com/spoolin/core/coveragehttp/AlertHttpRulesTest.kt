package com.spoolin.core.coveragehttp

import com.spoolin.core.features.alerts.AlertMode
import com.spoolin.core.features.alerts.AlertPolicy
import com.spoolin.core.features.alerts.AlertRecorder
import com.spoolin.core.features.alerts.AlertWebhook
import com.spoolin.management.alerts.AlertIngress
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import org.springframework.mock.web.MockHttpServletRequest
import java.lang.reflect.Proxy
import java.math.BigInteger
import java.security.Principal
import java.security.PublicKey
import java.security.cert.X509Certificate
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Statement
import java.time.Instant
import java.util.Date
import javax.sql.DataSource

internal class AlertJdbc(
    vararg results: Any,
) {
    private val results = ArrayDeque(results.toList())
    val writes = mutableListOf<List<Any?>>()
    var commits = 0
    var rollbacks = 0
    val source: DataSource = httpProxy { name, _ -> if (name == "getConnection") connection() else null }

    private fun connection(): Connection =
        httpProxy { name, _ ->
            when (name) {
                "createStatement" -> httpProxy<Statement> { method, _ -> if (method == "execute") true else null }
                "prepareStatement" -> statement()
                "commit" -> {
                    commits++
                    null
                }
                "rollback" -> {
                    rollbacks++
                    null
                }
                else -> null
            }
        }

    private fun statement(): PreparedStatement {
        val params = sortedMapOf<Int, Any?>()
        return httpProxy { name, args ->
            when {
                name.startsWith("set") && args.firstOrNull() is Int -> {
                    params[args[0] as Int] = args.getOrNull(1)
                    null
                }
                name == "executeUpdate" -> {
                    writes.add(params.values.toList())
                    val response = results.removeFirstOrNull() ?: 1
                    if (response is Throwable) throw response
                    response
                }
                else -> null
            }
        }
    }
}

internal inline fun <reified T> httpProxy(crossinline invoke: (String, Array<out Any?>) -> Any?): T =
    Proxy.newProxyInstance(
        T::class.java.classLoader,
        arrayOf(T::class.java),
    ) { _, method, args -> invoke(method.name, args ?: emptyArray()) } as T

internal class AlertCertificate(
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

class AlertHttpRulesTest {
    private val bytes =
        """
        {
          "version": "4",
          "status": "firing",
          "alerts": [
            {
              "status": "firing",
              "fingerprint": "0123456789abcdef",
              "startsAt": "2026-01-01T12:00:00Z",
              "endsAt": "2026-01-01T12:05:00Z",
              "labels": {
                "service": "core",
                "severity": "warning",
                "alertname": "ServiceUnavailable"
              },
              "annotations": {
                "summary": "Selected service unavailable",
                "description": "private caller detail"
              }
            }
          ]
        }
        """.trimIndent().toByteArray()
    private val environment =
        StandardEnvironment().also {
            it.propertySources.addFirst(
                MapPropertySource(
                    "unit",
                    mapOf("local.management.port" to "9001"),
                ),
            )
        }

    private fun request(body: ByteArray = bytes) =
        MockHttpServletRequest("POST", "/internal/alerts").also {
            it.isSecure = true
            it.localPort = 9001
            it.setContent(body)
            it.setAttribute("jakarta.servlet.request.X509Certificate", arrayOf(AlertCertificate()))
        }

    @Test fun `authorized alert records bounded allowlisted summary and returns private accepted response`() {
        val db = AlertJdbc()
        val result = AlertIngress(environment, AlertRecorder(db.source), AlertPolicy()).accept(request())
        assertEquals(202, result.statusCode.value())
        assertEquals(mapOf("status" to "RECORDED"), result.body)
        assertEquals("no-store", result.headers.getFirst("Cache-Control"))
        assertEquals("nosniff", result.headers.getFirst("X-Content-Type-Options"))
        assertEquals(1, db.commits)
        assertTrue(db.writes.any { "DONE" in it && "RECORDED" in it && "database" in it })
        val summaries =
            db.writes
                .flatten()
                .filterIsInstance<String>()
                .filter { it.contains("alertname") }
        assertTrue(summaries.isNotEmpty())
        assertTrue(summaries.all { !it.contains("private caller detail") })
    }

    @Test fun `ingress requires exact peer identity TLS and private listener port`() {
        val db = AlertJdbc()
        val ingress = AlertIngress(environment, AlertRecorder(db.source), AlertPolicy())
        val requests =
            listOf(
                request().also { it.isSecure = false },
                request().also { it.localPort = 8080 },
                request().also { it.removeAttribute("jakarta.servlet.request.X509Certificate") },
                request().also { it.setAttribute("jakarta.servlet.request.X509Certificate", arrayOf(AlertCertificate(listOf("core")))) },
                request().also {
                    it.setAttribute(
                        "jakarta.servlet.request.X509Certificate",
                        arrayOf(AlertCertificate(listOf("alertmanager", "core"))),
                    )
                },
            )
        for (request in requests) assertEquals(403, ingress.accept(request).statusCode.value())
        assertTrue(db.writes.isEmpty())
    }

    @Test fun `ingress rejects oversized encoded malformed payloads and masks persistence outage`() {
        val db = AlertJdbc()
        val ingress = AlertIngress(environment, AlertRecorder(db.source), AlertPolicy())
        assertEquals(413, ingress.accept(request(ByteArray(65537))).statusCode.value())
        assertEquals(400, ingress.accept(request().also { it.addHeader("Content-Encoding", "gzip") }).statusCode.value())
        assertEquals(400, ingress.accept(request("invalid-json".toByteArray())).statusCode.value())
        val broken = AlertJdbc(SQLException("private database details"))
        val unavailable = AlertIngress(environment, AlertRecorder(broken.source), AlertPolicy()).accept(request())
        assertEquals(503, unavailable.statusCode.value())
        assertEquals(mapOf("status" to "UNAVAILABLE"), unavailable.body)
        assertEquals(1, broken.rollbacks)
    }

    @Test fun `recorder deduplicates existing episodes and queues external mode only for new work`() {
        val episodes = AlertWebhook(AlertPolicy()).parse(bytes)
        val duplicate = AlertJdbc(0)
        assertEquals(0, AlertRecorder(duplicate.source).recordBatch(episodes, AlertPolicy()))
        assertEquals(1, duplicate.writes.size)
        val external = AlertJdbc()
        assertEquals(1, AlertRecorder(external.source).recordBatch(episodes, AlertPolicy(mode = AlertMode.SLACK)))
        assertTrue(external.writes.any { "READY" in it && "PENDING" in it && "slack" in it })
        val reconciliation = episodes.map { it.copy(summary = it.summary + ("alertname" to "NotificationUnresolved")) }
        val reserved = AlertJdbc()
        AlertRecorder(reserved.source).recordBatch(reconciliation, AlertPolicy(mode = AlertMode.SLACK))
        assertTrue(reserved.writes.any { "DONE" in it && "database" in it })
        assertThrows(IllegalArgumentException::class.java) { AlertRecorder(AlertJdbc().source).recordBatch(emptyList(), AlertPolicy()) }
    }
}
