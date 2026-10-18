package com.spoolin.support.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.UnsynchronizedAppenderBase
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.OutputStream

/** Installed on the root logger, including framework diagnostics and exception-bearing events. */
class BoundedJsonAppender internal constructor(
    private val streams: () -> Pair<OutputStream, OutputStream>,
) : UnsynchronizedAppenderBase<ILoggingEvent>() {
    constructor() : this({ FileOutputStream(FileDescriptor.out) to FileOutputStream(FileDescriptor.err) })

    var serviceName: String = ""
    var serviceVersion: String = "0.0.0-foundation"
    var serviceEnvironment: String = "local"
    private var envelope: DiagnosticEnvelope? = null
    private var sink: BoundedDiagnosticSink? = null

    override fun start() {
        envelope = DiagnosticEnvelope(serviceName, serviceVersion, serviceEnvironment)
        val (primary, alternate) = streams()
        sink = BoundedDiagnosticSink(serviceName, primary, alternate, DiagnosticDropMetrics.shared)
        super.start()
    }

    override fun append(event: ILoggingEvent) {
        val encoder = checkNotNull(envelope)
        val level =
            when (event.level) {
                Level.TRACE, Level.DEBUG -> DiagnosticLevel.DEBUG
                Level.INFO -> DiagnosticLevel.INFO
                Level.WARN -> DiagnosticLevel.WARNING
                Level.ERROR -> DiagnosticLevel.ERROR
                else -> null
            }
        val bytes =
            if (level == null) {
                DiagnosticDropMetrics.shared.record(serviceName, DiagnosticLevel.ERROR, DiagnosticDrop.CONTRACT_INVALID)
                encoder.invalid(event.timeStamp)
            } else {
                encoder.encode(level, event.timeStamp, event.loggerName)
            }
        checkNotNull(sink).submit(bytes, level ?: DiagnosticLevel.ERROR)
    }

    override fun stop() {
        super.stop()
        sink?.close()
    }
}
