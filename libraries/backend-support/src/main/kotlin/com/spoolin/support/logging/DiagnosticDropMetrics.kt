package com.spoolin.support.logging

import io.micrometer.core.instrument.FunctionCounter
import io.micrometer.core.instrument.MeterRegistry
import java.util.concurrent.atomic.AtomicLong

internal enum class DiagnosticLevel(
    val label: String,
) {
    DEBUG("debug"),
    INFO("info"),
    WARNING("warning"),
    ERROR("error"),
    FATAL("fatal"),
}

internal enum class DiagnosticDrop(
    val label: String,
) {
    QUEUE_FULL("queue_full"),
    SINK_FAILED("sink_failed"),
    CONTRACT_INVALID("contract_invalid"),
}

/** Retains bounded boot-time losses until the private registry becomes available. */
class DiagnosticDropMetrics {
    private data class Key(
        val service: String,
        val level: DiagnosticLevel,
        val reason: DiagnosticDrop,
    )

    private val counts = mutableMapOf<Key, AtomicLong>()
    private val registries = mutableMapOf<String, MeterRegistry>()

    @Synchronized
    fun bind(
        service: String,
        registry: MeterRegistry,
    ) {
        require(service in SERVICES)
        registries[service] = registry
        counts.filterKeys { it.service == service }.forEach { (key, count) -> register(registry, key, count) }
    }

    @Synchronized
    internal fun record(
        service: String,
        level: DiagnosticLevel,
        reason: DiagnosticDrop,
    ) {
        require(service in SERVICES)
        val key = Key(service, level, reason)
        val count = counts.getOrPut(key) { AtomicLong() }
        count.incrementAndGet()
        registries[service]?.let { register(it, key, count) }
    }

    private fun register(
        registry: MeterRegistry,
        key: Key,
        count: AtomicLong,
    ) {
        FunctionCounter
            .builder("spoolin.log.dropped", count) { it.get().toDouble() }
            .tags("service", key.service, "level", key.level.label, "reason", key.reason.label)
            .register(registry)
    }

    companion object {
        val shared = DiagnosticDropMetrics()
        internal val SERVICES = setOf("core", "catalog", "media")
    }
}
