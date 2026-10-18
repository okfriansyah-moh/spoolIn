package com.spoolin.support.notifications

import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket

internal class TimedConnection(
    host: String,
    port: Int,
    timeoutMs: Long,
) : AutoCloseable {
    val raw = Socket()
    private val watchdog =
        timer.schedule({
            try {
                raw.close()
            } catch (
                _: Exception,
            ) {
            }
        }, timeoutMs, TimeUnit.MILLISECONDS)
    private val hostName = host

    init {
        var connected = false
        try {
            val address =
                resolver.submit<java.net.InetAddress> {
                    java.net.InetAddress
                        .getAllByName(host)
                        .first()
                }
            val resolved =
                try {
                    address.get(timeoutMs.coerceIn(1, CONNECT_TIMEOUT_MS), TimeUnit.MILLISECONDS)
                } finally {
                    address.cancel(true)
                }
            raw.connect(
                InetSocketAddress(resolved, port),
                timeoutMs.coerceAtMost(CONNECT_TIMEOUT_MS).coerceAtLeast(1).toInt(),
            )
            raw.soTimeout = timeoutMs.coerceIn(1, READ_TIMEOUT_MS).toInt()
            connected = true
        } finally {
            if (!connected) close()
        }
    }

    fun tls(context: SSLContext): SSLSocket =
        (context.socketFactory.createSocket(raw, hostName, raw.port, true) as SSLSocket).also {
            it.sslParameters = it.sslParameters.apply { endpointIdentificationAlgorithm = "HTTPS" }
            it.startHandshake()
        }

    override fun close() {
        watchdog.cancel(false)
        raw.close()
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 3000L
        private const val READ_TIMEOUT_MS = 14000L
        private val resolver =
            java.util.concurrent.ThreadPoolExecutor(
                2,
                2,
                0,
                TimeUnit.MILLISECONDS,
                java.util.concurrent.ArrayBlockingQueue(2),
                java.util.concurrent.ThreadFactory { task ->
                    Thread(task, "notification-dns").apply { isDaemon = true }
                },
                java.util.concurrent.ThreadPoolExecutor
                    .AbortPolicy(),
            )
        private val timer =
            ScheduledThreadPoolExecutor(1) { task ->
                Thread(task, "notification-timeout").apply {
                    isDaemon =
                        true
                }
            }.apply { removeOnCancelPolicy = true }
    }
}
