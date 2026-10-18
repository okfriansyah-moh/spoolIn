package com.spoolin.support.work

import com.spoolin.support.receipts.queryBound
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.security.MessageDigest
import java.sql.Connection
import java.time.Instant
import java.util.UUID

private const val MAX_RECOVERY_INPUT_BYTES = 5242880L

/** Redacted operator evidence; payloads, credentials and private domain fields are excluded. */
data class WorkRecoveryInspection(
    val id: UUID,
    val kind: String,
    val state: String,
    val generation: Int,
    val leaseEpoch: Long,
    val attempts: Int,
    val maximumAttempts: Int,
    val deadline: Instant,
    val error: String?,
    val operationId: UUID?,
    val correlationId: UUID,
    val phaseHistory: List<Map<String, String?>> = emptyList(),
    val historyTruncated: Boolean = false,
)

/** Local operator repair remains restricted to owner-local, repeat-safe processing obligations. */
object WorkRecoveryOperations {
    fun inspect(
        connection: Connection,
        id: UUID,
        lock: Boolean = false,
    ): WorkRecoveryInspection? {
        val row =
            connection
                .queryBound(
                    "SELECT id,kind,state,generation,lease_epoch,attempt_count,max_attempts," +
                        "deadline_at,last_error,operation_id,correlation_id " +
                        "FROM appdata.durable_work WHERE id=?" + if (lock) " FOR UPDATE" else "",
                    listOf(id),
                ) {
                    WorkRecoveryInspection(
                        it.getObject("id", UUID::class.java),
                        it.getString("kind"),
                        it.getString("state"),
                        it.getInt("generation"),
                        it.getLong("lease_epoch"),
                        it.getInt("attempt_count"),
                        it.getInt("max_attempts"),
                        it.getTimestamp("deadline_at").toInstant(),
                        it.getString("last_error"),
                        it.getObject("operation_id", UUID::class.java),
                        it.getObject("correlation_id", UUID::class.java),
                    )
                }.singleOrNull() ?: return null
        val events =
            connection.queryBound(
                "SELECT event_type,generation,attempt_no,lease_epoch,attempt_outcome,error_code,created_at " +
                    "FROM appdata.operational_event WHERE work_id=? ORDER BY created_at,id LIMIT 1001",
                listOf(id),
            ) { event ->
                listOf(
                    "event_type",
                    "generation",
                    "attempt_no",
                    "lease_epoch",
                    "attempt_outcome",
                    "error_code",
                    "created_at",
                ).associateWith { field -> event.getString(field) }
            }
        return row.copy(phaseHistory = events.take(1000), historyTruncated = events.size > 1000)
    }

    fun recover(
        connection: Connection,
        id: UUID,
        expectedGeneration: Int,
        expectedEpoch: Long,
        operatorId: String,
        reason: String,
        mediaRoot: Path,
    ): Boolean {
        require(!connection.autoCommit) { "Operator recovery requires an explicit transaction" }
        val work = requireNotNull(inspect(connection, id, true)) { "Work unavailable" }
        require(
            work.state == "EXHAUSTED" && work.generation == expectedGeneration && work.leaseEpoch == expectedEpoch,
        ) {
            "Work changed since operator inspection"
        }
        require(!work.historyTruncated) { "Complete retained evidence review is required before recovery" }
        require(work.kind == "MEDIA_PROCESS") { "Destination reconciliation unavailable for this work kind" }
        validateMedia(connection, work, mediaRoot)
        val now = connection.queryBound("SELECT clock_timestamp()") { it.getTimestamp(1).toInstant() }.single()
        return WorkSql.recoverWork(connection, id, WorkRecoveryCommit(operatorId, reason, UUID.randomUUID()), now)
    }

    private fun validateMedia(
        connection: Connection,
        work: WorkRecoveryInspection,
        root: Path,
    ) {
        val asset =
            connection.queryBound(
                "SELECT a.id,a.state,a.input_bytes,a.processing_input_hash,a.processing_staging_key," +
                    "r.state AS receipt_state " +
                    "FROM appdata.asset a JOIN appdata.durable_work w ON w.id=a.processing_work_id " +
                    "JOIN appdata.operation_receipt r ON r.id=a.upload_operation_id " +
                    "WHERE w.id=? AND w.operation_id=a.upload_operation_id FOR UPDATE OF a,r",
                listOf(work.id),
            ) { row ->
                require(row.getString("state") == "PROCESSING" && row.getString("receipt_state") == "PENDING") {
                    "Terminal or unavailable destination outcome cannot be blindly retried"
                }
                val id = row.getObject("id", UUID::class.java)
                require(row.getString("processing_staging_key") == "quarantine/$id")
                validateInput(root, id, row.getLong("input_bytes"), row.getBytes("processing_input_hash"))
            }
        require(asset.size == 1) { "Processing destination evidence unavailable" }
        val started =
            connection
                .queryBound(
                    "SELECT max(created_at) FROM appdata.operational_event " +
                        "WHERE work_id=? AND event_type='ATTEMPT_STARTED'",
                    listOf(work.id),
                ) { it.getTimestamp(1)?.toInstant() }
                .single()
        val now = connection.queryBound("SELECT clock_timestamp()") { it.getTimestamp(1).toInstant() }.single()
        require(
            (started == null && work.attempts == 0) ||
                (started != null && started.plusSeconds(WorkSql.maxEffectDuration).isBefore(now)),
        ) {
            "Prior effect has not passed its enforced processing deadline"
        }
    }

    private fun validateInput(
        root: Path,
        id: UUID,
        size: Long,
        expected: ByteArray,
    ) {
        require(size in 1..MAX_RECOVERY_INPUT_BYTES)
        val directory = root.resolve("quarantine")
        require(Files.isDirectory(root, NOFOLLOW_LINKS) && Files.isDirectory(directory, NOFOLLOW_LINKS))
        val input = directory.resolve(id.toString())
        require(Files.isRegularFile(input, NOFOLLOW_LINKS) && Files.size(input) == size)
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(input).use { stream ->
            val buffer = ByteArray(8192)
            var count = stream.read(buffer)
            var total = 0L
            while (count != -1) {
                total += count
                require(total <= size) { "Accepted input changed during inspection" }
                digest.update(buffer, 0, count)
                count = stream.read(buffer)
            }
            require(
                total == size && MessageDigest.isEqual(digest.digest(), expected),
            ) { "Accepted input checksum differs" }
        }
    }
}
