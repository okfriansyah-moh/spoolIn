package com.spoolin.core.operations.alerts

import com.spoolin.core.MigrationRunner
import com.spoolin.core.boundary.bootstrap.OwnerFixture
import com.spoolin.support.notifications.NotificationDispatcher
import com.spoolin.support.notifications.NotificationResult
import com.spoolin.support.notifications.NotificationSettings
import com.spoolin.support.notifications.NotificationStore
import com.spoolin.support.notifications.NotificationTransport
import com.spoolin.support.notifications.SmtpTransport
import com.spoolin.support.notifications.WebhookTransport
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.work.AttemptOutcome
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.BufferedInputStream
import java.io.InputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyFactory
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManagerFactory

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotificationBoundaryTest {
    private lateinit var fixture: OwnerFixture
    private lateinit var ssl: SSLContext

    @BeforeAll
    fun start() {
        fixture = OwnerFixture("t034-notification-boundaries.json").start()
        fixture.membership("core", "grant")
        fixture.pool("core", "migrator").use { MigrationRunner(it).migrate() }
        fixture.grants("core")
        ssl = context(fixture.root)
    }

    @AfterAll
    fun stop() = fixture.close()

    @Test
    fun `actual TLS acceptance persists ACCEPTED while explicit rejection persists FAILED`() {
        listOf("smtp", "slack", "google_chat").forEach { mode ->
            listOf("ACCEPT", "REJECT").forEach { outcome ->
                ProviderServer(ssl, mode, outcome).use { server ->
                    fixture.pool("core").use { pool ->
                        val id = enqueue(mode)
                        NotificationDispatcher(pool, transport(server, mode)).use {
                            assertTrue(it.runOnce())
                            assertFalse(it.runOnce())
                        }
                        row(id, if (outcome == "ACCEPT") "DONE" else "EXHAUSTED", if (outcome == "ACCEPT") "ACCEPTED" else "FAILED")
                        assertEquals(1, server.sends.get())
                        assertFalse(server.messageText().contains("secret-do-not-forward"))
                        if (outcome == "ACCEPT") assertTrue(server.messageText().contains("notificationWorkId"))
                    }
                }
            }
        }
    }

    @Test
    fun `SMTP upgrades verified STARTTLS before AUTH and honors explicit authentication rejection`() {
        listOf("ACCEPT", "AUTH_REJECT").forEach { outcome ->
            ProviderServer(ssl, "smtp", outcome, startTls = true).use { server ->
                fixture.pool("core").use { pool ->
                    val id = enqueue("smtp")
                    val sender =
                        SmtpTransport(
                            "localhost",
                            server.port,
                            "starttls",
                            "ops@example.invalid",
                            "fixed@example.invalid",
                            "fixture-user",
                            "fixture-password",
                            ssl,
                        )
                    NotificationDispatcher(pool, sender).use {
                        assertTrue(it.runOnce())
                        assertFalse(it.runOnce())
                    }
                    row(id, if (outcome == "ACCEPT") "DONE" else "EXHAUSTED", if (outcome == "ACCEPT") "ACCEPTED" else "FAILED")
                    assertEquals(if (outcome == "ACCEPT") 1 else 0, server.sends.get())
                }
            }
        }
    }

    @Test
    fun `lost provider acknowledgment is terminal UNKNOWN without automatic resend`() {
        listOf("smtp", "slack", "google_chat").forEach { mode ->
            ProviderServer(ssl, mode, "TIMEOUT").use { server ->
                fixture.pool("core").use { pool ->
                    val id = enqueue(mode)
                    val store = NotificationStore(pool)
                    val item = requireNotNull(store.claim())
                    val result = transport(server, mode).send(item, 250)
                    assertEquals(AttemptOutcome.UNCERTAIN, result.outcome)
                    assertTrue(store.finish(item.work, result))
                    row(id, "EXHAUSTED", "UNKNOWN")
                    NotificationDispatcher(pool, transport(server, mode)).use { assertFalse(it.runOnce()) }
                    assertEquals(1, server.sends.get())
                }
            }
        }
    }

    @Test
    fun `crash after acceptance retains uncertain finished attempt and never reclaims send`() {
        ProviderServer(ssl, "slack", "ACCEPT").use { server ->
            fixture.pool("core").use { pool ->
                val id = enqueue("slack")
                val store = NotificationStore(pool)
                val item = requireNotNull(store.claim())
                assertEquals(AttemptOutcome.SUCCEEDED, transport(server, "slack").send(item, 2000).outcome)
                fixture.connection("core").use {
                    it.executeBound("UPDATE durable_work SET lease_until=clock_timestamp()-INTERVAL '1 second' WHERE id=?", id)
                }
                assertNull(store.claim())
                row(id, "EXHAUSTED", "UNKNOWN")
                fixture.connection("core").use { connection ->
                    connection
                        .prepareStatement(
                            "SELECT attempt_outcome FROM operational_event WHERE work_id=? AND event_type='ATTEMPT_FINISHED'",
                        ).use {
                            it.setObject(1, id)
                            it.executeQuery().use { rows ->
                                assertTrue(rows.next())
                                assertEquals("UNCERTAIN", rows.getString(1))
                                assertFalse(rows.next())
                            }
                        }
                }
                assertEquals(1, server.sends.get())
            }
        }
    }

    @Test
    fun `429 persists bounded retry schedule while standard TLS rejects hostname mismatch before send`() {
        ProviderServer(ssl, "slack", "RATE").use { server ->
            fixture.pool("core").use { pool ->
                val id = enqueue("slack")
                NotificationDispatcher(pool, transport(server, "slack")).use {
                    assertTrue(it.runOnce())
                    assertFalse(it.runOnce())
                }
                row(id, "EXHAUSTED", "FAILED") // Retry-After 60s cannot fit the saved 60s deadline.
            }
        }
        ProviderServer(ssl, "slack", "ACCEPT").use { server ->
            fixture.pool("core").use { pool ->
                val id = enqueue("slack")
                val store = NotificationStore(pool)
                val item = requireNotNull(store.claim())
                val transport = WebhookTransport("slack", URI("https://127.0.0.1:${server.port}/fixed"), setOf("127.0.0.1"), ssl)
                assertEquals(AttemptOutcome.TRANSIENT, transport.send(item, 2000).outcome)
                assertEquals(0, server.sends.get())
                assertTrue(store.finish(item.work, NotificationResult(AttemptOutcome.PERMANENT, "FIXTURE_END")))
                row(id, "EXHAUSTED", "FAILED")
            }
        }
    }

    @Test
    fun `database mode never creates transport and invalid external endpoint fails closed`() {
        assertNull(NotificationSettings.transport { null })
        assertThrows(IllegalArgumentException::class.java) {
            NotificationSettings.transport {
                if (it == "NOTIFICATION_MODE") "slack" else null
            }
        }
        val file = fixture.root.resolve("secret-webhook")
        Files.writeString(file, "https://foreign.invalid/services/test/test/test")
        Files.setPosixFilePermissions(
            file,
            java.nio.file.attribute.PosixFilePermissions
                .fromString("rw-------"),
        )
        assertThrows(IllegalArgumentException::class.java) {
            NotificationSettings.transport {
                mapOf("NOTIFICATION_MODE" to "slack", "NOTIFICATION_SLACK_WEBHOOK_FILE" to file.toString())[it]
            }
        }
    }

    private fun transport(
        server: ProviderServer,
        mode: String,
    ): NotificationTransport =
        if (mode == "smtp") {
            SmtpTransport("localhost", server.port, "implicit", "ops@example.invalid", "fixed@example.invalid", null, null, ssl)
        } else {
            WebhookTransport(mode, URI("https://localhost:${server.port}/fixed"), setOf("localhost"), ssl)
        }

    private fun enqueue(mode: String): UUID {
        val id = UUID.randomUUID()
        fixture.connection("core").use {
            it.executeBound(
                "INSERT INTO durable_work " +
                    "(id,kind,payload,max_attempts,deadline_at,correlation_id,base_delay_ms,cap_delay_ms,backoff_multiplier," +
                    "notification_key,notification_mode,notification_state,notification_summary) " +
                    "VALUES (?,'NOTIFICATION','{}',3,clock_timestamp()+INTERVAL '60 seconds',?,500,5000,2,?,?,'PENDING',?::jsonb)",
                id,
                UUID.randomUUID(),
                "work:$id:generation:0",
                mode,
                """{"environment":"ci","service":"core","class":"TEST","secret":"secret-do-not-forward"}""",
            )
        }
        return id
    }

    private fun row(
        id: UUID,
        state: String,
        notification: String,
    ) {
        fixture.connection("core").use { connection ->
            connection
                .prepareStatement(
                    "SELECT state,notification_state,attempt_count,provider_reference FROM durable_work WHERE id=?",
                ).use {
                    it.setObject(1, id)
                    it.executeQuery().use { rows ->
                        assertTrue(rows.next())
                        assertEquals(state, rows.getString(1))
                        assertEquals(notification, rows.getString(2))
                        assertEquals(1, rows.getInt(3))
                        if (notification == "ACCEPTED") assertNotNull(rows.getString(4)) else assertNull(rows.getString(4))
                        assertFalse(rows.next())
                    }
                }
        }
    }

    private fun context(root: Path): SSLContext {
        val factory = CertificateFactory.getInstance("X.509")
        val certificate = Files.newInputStream(root.resolve("core.crt")).use { factory.generateCertificate(it) }
        val encoded =
            Files
                .readString(root.resolve("core.key"))
                .lineSequence()
                .filter { !it.startsWith("---") }
                .joinToString("")
        val key = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(encoded)))
        val store =
            KeyStore.getInstance("PKCS12").apply {
                load(null, null)
                setKeyEntry("server", key, CharArray(0), arrayOf(certificate))
            }
        val trust = KeyStore.getInstance("PKCS12").apply { load(null, null) }
        Files.newInputStream(root.resolve("ca.crt")).use { trust.setCertificateEntry("ca", factory.generateCertificate(it)) }
        val km = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply { init(store, CharArray(0)) }
        val tm = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply { init(trust) }
        return SSLContext.getInstance("TLS").apply { init(km.keyManagers, tm.trustManagers, null) }
    }
}

private class ProviderServer(
    private val ssl: SSLContext,
    private val mode: String,
    private val outcome: String,
    private val startTls: Boolean = false,
) : AutoCloseable {
    private val server =
        (if (startTls) java.net.ServerSocket() else ssl.serverSocketFactory.createServerSocket()).apply {
            bind(InetSocketAddress(InetAddress.getLoopbackAddress(), 0))
            soTimeout = 5000
        }
    val port: Int get() = server.localPort
    val sends = AtomicInteger()
    val received = StringBuilder()
    private val executor = Executors.newSingleThreadExecutor()
    private val future =
        executor.submit {
            try {
                server.accept().use { socket ->
                    socket.soTimeout = 3000
                    if (mode == "smtp") smtp(socket) else http(socket)
                }
            } catch (_: java.io.IOException) {
                // Expected TLS rejection or lost acknowledgements.
            }
        }

    private fun http(socket: java.net.Socket) {
        val input = BufferedInputStream(socket.inputStream)
        val headers = mutableListOf<String>()
        while (true) {
            val next = line(input)
            if (next.isEmpty()) break
            headers.add(next)
            require(headers.size <= 64)
        }
        val length =
            headers
                .single { it.startsWith("Content-Length:") }
                .substringAfter(':')
                .trim()
                .toInt()
        require(length in 1..16384)
        received.append(input.readNBytes(length).toString(Charsets.UTF_8))
        sends.incrementAndGet()
        if (outcome == "TIMEOUT") {
            Thread.sleep(1000)
            return
        }
        val status =
            when (outcome) {
                "REJECT" -> 403
                "RATE" -> 429
                else -> 200
            }
        val body = if (mode == "slack") "ok" else """{"name":"spaces/test/messages/msg1"}"""
        socket.outputStream.write(
            ("HTTP/1.1 $status Result\r\nContent-Length: ${body.length}\r\nRetry-After: 60\r\n\r\n" + body).toByteArray(),
        )
        socket.outputStream.flush()
    }

    private fun smtp(socket: java.net.Socket) {
        var input: InputStream = BufferedInputStream(socket.inputStream)
        var output = socket.outputStream
        var secure = !startTls

        fun reply(text: String) {
            output.write((text + "\r\n").toByteArray())
            output.flush()
        }
        reply("220 fixture SMTP")
        while (true) {
            val command = line(input)
            when {
                command.startsWith("EHLO") -> reply("250 fixture")
                command == "STARTTLS" -> {
                    assertFalse(secure)
                    reply("220 upgrade")
                    val upgraded = ssl.socketFactory.createSocket(socket, "localhost", port, true) as SSLSocket
                    upgraded.useClientMode = false
                    upgraded.startHandshake()
                    input = BufferedInputStream(upgraded.inputStream)
                    output = upgraded.outputStream
                    secure = true
                }
                command.startsWith("AUTH PLAIN ") -> {
                    assertTrue(secure)
                    val credentials = Base64.getDecoder().decode(command.substringAfter("AUTH PLAIN ")).toString(Charsets.UTF_8)
                    assertEquals("\u0000fixture-user\u0000fixture-password", credentials)
                    reply(if (outcome == "AUTH_REJECT") "535 rejected" else "235 authenticated")
                    if (outcome == "AUTH_REJECT") return
                }
                command.startsWith("MAIL") || command.startsWith("RCPT") -> {
                    assertTrue(secure)
                    reply("250 accepted")
                }
                command == "DATA" -> {
                    assertTrue(secure)
                    reply("354 send content")
                    while (true) {
                        val next = line(input)
                        if (next ==
                            "."
                        ) {
                            break
                        }
                        received.append(next).append('\n')
                        require(received.length <= 16384)
                    }
                    sends.incrementAndGet()
                    if (outcome == "TIMEOUT") {
                        Thread.sleep(1000)
                        return
                    }
                    reply(if (outcome == "REJECT") "550 rejected" else "250 accepted fixture-message")
                    return
                }
                else -> error("Unexpected SMTP request")
            }
        }
    }

    private fun line(input: InputStream): String {
        val bytes = java.io.ByteArrayOutputStream()
        while (bytes.size() < 16384) {
            val byte = input.read()
            if (byte < 0) throw java.io.EOFException()
            if (byte == 10) return bytes.toString(Charsets.US_ASCII).trimEnd('\r')
            bytes.write(byte)
        }
        error("Provider fixture line exceeded bounds")
    }

    fun messageText(): String =
        if (mode == "smtp") {
            Base64.getMimeDecoder().decode(received.toString().substringAfter("\n\n")).toString(Charsets.UTF_8)
        } else {
            received.toString()
        }

    override fun close() {
        server.close()
        try {
            future.get(5, TimeUnit.SECONDS)
        } finally {
            executor.shutdownNow()
        }
    }
}
