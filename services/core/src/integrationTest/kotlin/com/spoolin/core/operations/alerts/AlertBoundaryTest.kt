package com.spoolin.core.operations.alerts

import com.spoolin.core.features.alerts.AlertPolicy
import com.spoolin.core.features.alerts.AlertRecorder
import com.spoolin.core.features.alerts.AlertWebhook
import com.spoolin.core.features.session.IdentityHttpFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.time.Duration
import java.util.concurrent.Executors
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

class AlertBoundaryTest {
    @Test
    fun `real private management listener accepts only Alertmanager mTLS and deduplicates committed episodes`() {
        IdentityHttpFixture(false).use { fixture ->
            val root = fixture.crypto.owner.root
            val port = fixture.context.environment.getRequiredProperty("local.management.port")
            val uri = "https://localhost:$port/internal/alerts"
            val client = alertClient(root)
            assertUnrelatedCsrf(fixture, "https://localhost:$port")
            assertEquals(403, send(fixture.client, uri, payload()).statusCode())
            assertEquals(400, send(client, uri, "{}").statusCode())
            assertEquals(403, send(client, "https://localhost:$port/actuator/health/liveness", "{}").statusCode())
            assertEquals(202, send(client, uri, payload()).statusCode())
            assertEquals(202, send(client, uri, payload()).statusCode())
            assertEquals(400, send(client, uri, payload().replace("core", "foreign")).statusCode())
            assertEquals(413, send(client, uri, " ".repeat(65537)).statusCode())
            assertPublicAlertHidden(fixture, client)
            fixture.crypto.pool.connection.use { connection ->
                connection.createStatement().use { statement ->
                    statement
                        .executeQuery(
                            "SELECT state,notification_state,payload::text,attempt_count,generation," +
                                "lease_epoch,deadline_at=created_at FROM durable_work WHERE kind='NOTIFICATION'",
                        ).use { rows ->
                            assertTrue(rows.next())
                            assertEquals("DONE", rows.getString(1))
                            assertEquals("RECORDED", rows.getString(2))
                            assertEquals("{}", rows.getString(3))
                            assertEquals(0, rows.getInt(4))
                            assertEquals(0, rows.getInt(5))
                            assertEquals(0L, rows.getLong(6))
                            assertTrue(rows.getBoolean(7))
                            assertFalse(rows.next())
                        }
                    statement.executeQuery("SELECT count(*) FROM operational_event WHERE action='ALERT_RECORDED'").use {
                        assertTrue(it.next())
                        assertEquals(1, it.getInt(1))
                    }
                }
            }
            concurrentDuplicate(fixture)
            atomicRollback(fixture)
        }
    }

    private fun assertPublicAlertHidden(
        fixture: IdentityHttpFixture,
        alertmanager: HttpClient,
    ) {
        val uri = fixture.origin + "/internal/alerts"
        assertEquals(404, send(alertmanager, uri, payload()).statusCode())
        assertEquals(404, send(fixture.client, uri, payload()).statusCode())
        val request = HttpRequest.newBuilder(URI(uri)).timeout(Duration.ofSeconds(5)).GET().build()
        assertEquals(404, alertmanager.send(request, HttpResponse.BodyHandlers.discarding()).statusCode())
    }

    private fun assertUnrelatedCsrf(
        fixture: IdentityHttpFixture,
        origin: String,
    ) {
        val uri = origin + "/actuator/health/liveness"
        val healthy =
            fixture.client.send(
                HttpRequest
                    .newBuilder(URI(uri))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.discarding(),
            )
        assertEquals(200, healthy.statusCode())
        assertEquals(403, send(fixture.client, uri, "{}").statusCode())
    }

    private fun concurrentDuplicate(fixture: IdentityHttpFixture) {
        val recorder = AlertRecorder(fixture.crypto.pool)
        val episode = AlertWebhook(AlertPolicy()).parse(payload().replace("0123456789abcdef", "1123456789abcdef").toByteArray())
        val executor = Executors.newFixedThreadPool(4)
        try {
            val futures = (1..8).map { executor.submit<Int> { recorder.recordBatch(episode, AlertPolicy()) } }
            assertEquals(1, futures.sumOf { it.get(5, java.util.concurrent.TimeUnit.SECONDS) })
        } finally {
            executor.shutdownNow()
        }
    }

    private fun atomicRollback(fixture: IdentityHttpFixture) {
        val recorder = AlertRecorder(fixture.crypto.pool)
        val valid = AlertWebhook(AlertPolicy()).parse(payload().replace("0123456789abcdef", "2123456789abcdef").toByteArray()).single()
        val invalid = valid.copy(fingerprint = "3123456789abcdef", summary = mapOf("summary" to "x".repeat(9000)))
        assertThrows(java.sql.SQLException::class.java) { recorder.recordBatch(listOf(valid, invalid), AlertPolicy()) }
        fixture.crypto.pool.connection.use { connection ->
            connection.prepareStatement("SELECT count(*) FROM durable_work WHERE notification_key=?").use {
                it.setString(1, valid.key)
                it.executeQuery().use { rows ->
                    assertTrue(rows.next())
                    assertEquals(0, rows.getInt(1))
                }
            }
        }
    }

    private fun send(
        client: HttpClient,
        uri: String,
        body: String,
    ): HttpResponse<String> =
        client.send(
            HttpRequest
                .newBuilder(URI(uri))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString(),
        )

    private fun alertClient(root: Path): HttpClient {
        command(
            root,
            "openssl",
            "req",
            "-new",
            "-newkey",
            "rsa:2048",
            "-nodes",
            "-keyout",
            "alertmanager.key",
            "-out",
            "alertmanager.csr",
            "-subj",
            "/CN=alertmanager",
        )
        Files.writeString(root.resolve("alertmanager.ext"), "subjectAltName=DNS:alertmanager\nextendedKeyUsage=clientAuth\n")
        command(
            root,
            "openssl",
            "x509",
            "-req",
            "-in",
            "alertmanager.csr",
            "-CA",
            "ca.crt",
            "-CAkey",
            "ca.key",
            "-set_serial",
            "900",
            "-out",
            "alertmanager.crt",
            "-days",
            "2",
            "-extfile",
            "alertmanager.ext",
        )
        command(
            root,
            "openssl",
            "pkcs12",
            "-export",
            "-in",
            "alertmanager.crt",
            "-inkey",
            "alertmanager.key",
            "-out",
            "alertmanager.p12",
            "-passout",
            "pass:synthetic-fixture-only",
        )
        val keys = KeyStore.getInstance("PKCS12")
        val password = "synthetic-fixture-only".toCharArray()
        Files.newInputStream(root.resolve("alertmanager.p12")).use { keys.load(it, password) }
        val km = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply { init(keys, password) }
        val trust = KeyStore.getInstance("PKCS12").apply { load(null, null) }
        Files.newInputStream(root.resolve("ca.crt")).use {
            trust.setCertificateEntry("ca", CertificateFactory.getInstance("X.509").generateCertificate(it))
        }
        val tm = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply { init(trust) }
        val ssl = SSLContext.getInstance("TLS").apply { init(km.keyManagers, tm.trustManagers, null) }
        return HttpClient.newBuilder().sslContext(ssl).build()
    }

    private fun command(
        root: Path,
        vararg args: String,
    ) {
        val process =
            ProcessBuilder(*args)
                .directory(root.toFile())
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start()
        check(process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) { "Certificate fixture timed out" }
        check(process.exitValue() == 0) { "Certificate fixture failed" }
        Files.list(root).use { paths ->
            paths.filter { it.fileName.toString().startsWith("alertmanager") }.forEach {
                Files.setPosixFilePermissions(
                    it,
                    java.nio.file.attribute.PosixFilePermissions
                        .fromString("rw-------"),
                )
            }
        }
    }

    private fun payload() =
        """{"version":"4","status":"firing","alerts":[{"status":"firing",
        "fingerprint":"0123456789abcdef","startsAt":"2026-10-18T01:00:00Z","endsAt":"0001-01-01T00:00:00Z",
        "labels":{"service":"core","severity":"warning","alertname":"WorkQueueBacklog"},
        "annotations":{"summary":"Durable work backlog elevated"}}]}"""
}
