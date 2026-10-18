package com.spoolin.support.observability

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import javax.sql.DataSource

private const val NOTIFICATION_COUNT_COLUMN = 3

/** The management filter must call prepareScrape and return HTTP503 when collection fails. */
class OwnerMetrics(
    registry: MeterRegistry,
    private val dataSource: DataSource,
    private val owner: String,
    private val runtime: OwnerMetricRuntime,
    private val volumes: Map<String, Path> = emptyMap(),
) {
    @Volatile private var measured: Snapshot? = null

    init {
        require(owner in setOf("core", "catalog", "media"))
        require(volumes.keys.all { it in setOf("database", "artwork", "backup") })
        for (state in WORK_STATES) {
            gauge(
                registry,
                "spoolin.work.items",
                "state",
                state,
            ) { snapshot().work.getValue(state) }
        }
        gauge(registry, "spoolin.oldest.due.work.seconds") { snapshot().oldest }
        for (mode in MODES) {
            for (state in NOTIFICATION_STATES) {
                Gauge
                    .builder("spoolin.notification.events") { snapshot().notifications.getValue(mode to state) }
                    .tags("service", owner, "mode", mode, "state", state)
                    .strongReference(true)
                    .register(registry)
            }
        }
        gauge(registry, "spoolin.pool.active.connections") { snapshot().active.toDouble() }
        gauge(registry, "spoolin.pool.max.connections") { snapshot().maximum.toDouble() }
        gauge(registry, "spoolin.service.ready") { if (snapshot().ready) 1.0 else 0.0 }
        for (volume in volumes.keys) {
            gauge(
                registry,
                "spoolin.disk.available.bytes",
                "volume",
                volume,
            ) {
                snapshot()
                    .disks
                    .getValue(volume)
                    .first
                    .toDouble()
            }
            gauge(
                registry,
                "spoolin.disk.capacity.bytes",
                "volume",
                volume,
            ) {
                snapshot()
                    .disks
                    .getValue(volume)
                    .second
                    .toDouble()
            }
        }
    }

    @Synchronized
    fun prepareScrape(): Boolean =
        try {
            val current = dataSource.connection.use(::collectCommitted)
            measured = current
            true
        } catch (_: Exception) {
            measured = null
            false
        }

    private fun collectCommitted(connection: Connection): Snapshot {
        connection.isReadOnly = true
        connection.transactionIsolation = Connection.TRANSACTION_REPEATABLE_READ
        connection.autoCommit = false
        return try {
            val result = collect(connection)
            connection.commit()
            result
        } finally {
            connection.rollback()
        }
    }

    private fun collect(connection: Connection): Snapshot {
        val work = WORK_STATES.associateWith { 0.0 }.toMutableMap()
        query(
            connection,
            """
            SELECT state,count(*) FROM appdata.durable_work
            WHERE kind<>'NOTIFICATION' OR notification_mode<>'database' GROUP BY state
            """.trimIndent(),
        ) { rows -> work[rows.getString(1)] = rows.getDouble(2) }
        val notifications =
            MODES
                .flatMap { mode ->
                    NOTIFICATION_STATES.map { (mode to it) to 0.0 }
                }.toMap()
                .toMutableMap()
        query(
            connection,
            """
            SELECT notification_mode,notification_state,count(*) FROM appdata.durable_work
            WHERE kind='NOTIFICATION' GROUP BY notification_mode,notification_state
            """.trimIndent(),
        ) { rows -> notifications[rows.getString(1) to rows.getString(2)] = rows.getDouble(NOTIFICATION_COUNT_COLUMN) }
        var oldest = 0.0
        query(
            connection,
            """
            SELECT COALESCE(EXTRACT(EPOCH FROM CURRENT_TIMESTAMP-MIN(next_attempt_at)),0)
            FROM appdata.durable_work WHERE state='READY' AND next_attempt_at<=CURRENT_TIMESTAMP
            """.trimIndent(),
        ) { oldest = it.getDouble(1) }
        val active = runtime.activeConnections()
        val maximum = runtime.maximumConnections()
        require(maximum > 0 && active in 0..maximum)
        val disks =
            volumes.mapValues { (_, path) ->
                require(path.isAbsolute && Files.exists(path))
                val store = Files.getFileStore(path)
                (store.usableSpace to store.totalSpace).also { require(it.first >= 0 && it.second > 0) }
            }
        return Snapshot(work, notifications, oldest, active, maximum, runtime.serviceReady(), disks)
    }

    private fun query(
        connection: Connection,
        sql: String,
        row: (java.sql.ResultSet) -> Unit,
    ) {
        connection.createStatement().use { statement ->
            statement.queryTimeout = 2
            statement.executeQuery(sql).use { rows -> readRows(rows, row) }
        }
    }

    private fun readRows(
        rows: java.sql.ResultSet,
        row: (java.sql.ResultSet) -> Unit,
    ) {
        while (rows.next()) row(rows)
    }

    private fun snapshot(): Snapshot = checkNotNull(measured) { "No successful current owner measurement" }

    private fun gauge(
        registry: MeterRegistry,
        name: String,
        label: String? = null,
        value: String? = null,
        sample: () -> Double,
    ) {
        val gauge = Gauge.builder(name) { sample() }
        gauge.tag("service", owner)
        if (label != null) gauge.tag(label, requireNotNull(value))
        gauge.strongReference(true).register(registry)
    }

    private data class Snapshot(
        val work: Map<String, Double>,
        val notifications: Map<Pair<String, String>, Double>,
        val oldest: Double,
        val active: Int,
        val maximum: Int,
        val ready: Boolean,
        val disks: Map<String, Pair<Long, Long>>,
    )

    companion object {
        private val WORK_STATES = listOf("READY", "LEASED", "DONE", "EXHAUSTED", "CANCELLED")
        private val MODES = listOf("database", "smtp", "slack", "google_chat")
        private val NOTIFICATION_STATES =
            listOf("RECORDED", "PENDING", "ACCEPTED", "DELIVERY_CONFIRMED", "UNKNOWN", "FAILED")
    }
}

/** Runtime measurements stay explicit and come from the actual owner pool and readiness state. */
class OwnerMetricRuntime(
    val activeConnections: () -> Int,
    val maximumConnections: () -> Int,
    val serviceReady: () -> Boolean,
)
