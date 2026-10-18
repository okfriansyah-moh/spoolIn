package com.spoolin.support.logging

import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

private const val POLL_MS = 100L
private const val SHUTDOWN_MS = 1000L

/** One daemon writer isolates slow pipes from request threads; full queues discard complete records. */
internal class BoundedDiagnosticSink(
    private val service: String,
    private val primary: OutputStream,
    private val alternate: OutputStream,
    private val metrics: DiagnosticDropMetrics,
    capacity: Int = DIAGNOSTIC_QUEUE,
) : AutoCloseable {
    private data class Record(val bytes: ByteArray, val level: DiagnosticLevel)
    private val queue = ArrayBlockingQueue<Record>(capacity)
    private val running = AtomicBoolean(true)
    private val worker = Thread(::drain, "diagnostic-writer").apply { isDaemon = true; start() }

    @Synchronized
    fun submit(bytes: ByteArray, level: DiagnosticLevel) {
        val reason = when {
            bytes.size > DIAGNOSTIC_BYTES -> DiagnosticDrop.CONTRACT_INVALID
            !running.get() || !queue.offer(Record(bytes, level)) -> DiagnosticDrop.QUEUE_FULL
            else -> null
        }
        if (reason != null) metrics.record(service, level, reason)
    }

    private fun drain() {
        while (running.get() || queue.isNotEmpty()) {
            val record = try { queue.poll(POLL_MS, TimeUnit.MILLISECONDS) } catch (_: InterruptedException) { null }
            if (record != null) deliver(record)
        }
    }

    private fun deliver(record: Record) {
        if (!write(primary, record.bytes) && !write(alternate, record.bytes)) {
            metrics.record(service, record.level, DiagnosticDrop.SINK_FAILED)
        }
    }

    private fun write(output: OutputStream, bytes: ByteArray): Boolean = try {
        output.write(bytes)
        output.flush()
        true
    } catch (_: IOException) { false }

    @Synchronized
    override fun close() {
        running.set(false)
        worker.join(SHUTDOWN_MS)
        if (worker.isAlive) {
            worker.interrupt()
            var discarded = queue.poll()
            while (discarded != null) {
                metrics.record(service, discarded.level, DiagnosticDrop.SINK_FAILED)
                discarded = queue.poll()
            }
        }
    }
}
