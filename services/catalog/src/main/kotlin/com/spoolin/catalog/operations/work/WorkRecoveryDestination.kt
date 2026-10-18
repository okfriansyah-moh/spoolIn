package com.spoolin.catalog.operations.work

import com.spoolin.support.receipts.WorkRequest
import com.spoolin.support.receipts.queryBound
import com.spoolin.support.work.OperatorAccess
import com.spoolin.support.work.WorkRecoveryInspection
import com.spoolin.support.work.WorkSql
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.netty.GrpcSslContexts
import io.grpc.netty.NettyChannelBuilder
import spoolin.v1.MediaServiceGrpc
import spoolin.v1.Spoolin
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Path
import java.sql.Connection
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val RPC_SECONDS = 5L
private const val MEDIA_RPC_PORT = 9090

/** Read-only destination reconciliation; the worker retains its original child request and repeats live checks. */
class WorkRecoveryDestination(
    private val supplied: MediaServiceGrpc.MediaServiceBlockingStub? = null,
) : com.spoolin.support.work.WorkRecoveryDestination {
    override val owner = "catalog"
    private val channel = lazy { connect() }

    override fun validate(connection: Connection, work: WorkRecoveryInspection) {
        require(work.kind in setOf("ARTWORK_ATTACH", "ARTWORK_CONFIRM", "ARTWORK_RELEASE"))
        quiescent(connection, work)
        val payload = connection.queryBound("SELECT payload::text FROM durable_work WHERE id=?", listOf(work.id)) {
            JsonMapper.builder().build().readTree(it.getString(1))
        }.single()
        val subject = payload["subjectId"].asText()
        val hold = UUID.fromString(subject).also { require(it.toString() == subject) }
        val target = target(connection, hold)
        val step = when (work.kind) {
            "ARTWORK_ATTACH" -> { preparing(connection, work, target); "media-reserve" }
            "ARTWORK_CONFIRM" -> { selected(work, target); "media-confirm" }
            else -> { require(target.state != "PREPARING" && !target.selected); "media-release" }
        }
        val original = WorkRequest(step, "ARTWORK", hold, Instant.MAX).id(hold)
        val expectedVersion = when (work.kind) {
            "ARTWORK_CONFIRM" -> Math.addExact(target.expectedVersion, 1)
            "ARTWORK_RELEASE" -> payload["catalogVersion"].let { version ->
                require(version.isIntegralNumber && version.canConvertToLong())
                version.asLong().also { require(it >= 0) }
            }
            else -> null
        }
        reconcile(original, hold, expectedVersion)
    }

    private fun preparing(connection: Connection, work: WorkRecoveryInspection, target: RecoveryTarget) {
        require(work.operationId == target.id && target.state == "PREPARING")
        require(target.titleStatus == "ACTIVE" && target.currentVersion == target.expectedVersion)
        val pending = connection.queryBound(
            "SELECT count(*) FROM operation_receipt WHERE id=? AND state='PENDING'", listOf(target.id),
        ) { it.getInt(1) }.single()
        require(pending == 1)
        val asset = stub().getAsset(Spoolin.Id.newBuilder().setId(target.asset.toString()).build())
        require(asset.id == target.asset.toString() && asset.state == "READY") { "Accepted asset is not ready" }
    }

    private fun selected(work: WorkRecoveryInspection, target: RecoveryTarget) {
        require(work.operationId == target.id && target.state == "ATTACHED" && target.selected)
        require(target.titleStatus != "DELETED" && target.currentVersion > target.expectedVersion)
    }

    private fun reconcile(operation: UUID, hold: UUID, version: Long?) {
        val receipt = try {
            stub().getOperation(Spoolin.Id.newBuilder().setId(operation.toString()).build())
        } catch (failure: StatusRuntimeException) {
            if (failure.status.code != Status.Code.NOT_FOUND) throw failure
            null
        }
        if (receipt != null) {
            require(receipt.operationId == operation.toString() && receipt.state == "SUCCEEDED")
            require(!receipt.resultExpired && !receipt.hasProblem()) { "Destination receipt cannot be replayed" }
            require(receipt.hasResultId() && receipt.resultId == hold.toString())
            require(if (version == null) !receipt.hasVersion() else receipt.hasVersion() && receipt.version == version)
        }
    }

    private fun quiescent(connection: Connection, work: WorkRecoveryInspection) {
        val last = connection.queryBound(
            "SELECT max(created_at) FROM operational_event WHERE work_id=? AND event_type='ATTEMPT_STARTED'",
            listOf(work.id),
        ) { it.getTimestamp(1)?.toInstant() }.single()
        val now = connection.queryBound("SELECT clock_timestamp()") { it.getTimestamp(1).toInstant() }.single()
        require((last == null && work.attempts == 0) ||
            (last != null && last.plusSeconds(WorkSql.maxEffectDuration).isBefore(now))) {
            "Previous bounded destination effect may still be active"
        }
    }

    private fun target(connection: Connection, id: UUID): RecoveryTarget = connection.queryBound(
        "SELECT a.id,a.asset_id,a.expected_version,a.state,t.status,t.version,t.attachment_id " +
            "FROM artwork_attachment_request a JOIN title t ON t.id=a.title_id WHERE a.id=? FOR UPDATE OF a,t",
        listOf(id),
    ) { row -> RecoveryTarget(
        id, row.getObject("asset_id", UUID::class.java), row.getLong("expected_version"), row.getString("state"),
        row.getString("status"), row.getLong("version"), row.getObject("attachment_id", UUID::class.java) == id,
    ) }.single()

    private fun stub(): MediaServiceGrpc.MediaServiceBlockingStub =
        (supplied ?: MediaServiceGrpc.newBlockingStub(channel.value)).withDeadlineAfter(RPC_SECONDS, TimeUnit.SECONDS)

    private fun connect(): ManagedChannel {
        val key = Path.of(OperatorAccess.required("SERVICE_KEY_FILE"))
        OperatorAccess.protectedFile(key)
        val tls = GrpcSslContexts.forClient().trustManager(Path.of(OperatorAccess.required("SERVICE_CA_FILE")).toFile())
            .keyManager(Path.of(OperatorAccess.required("SERVICE_CERT_FILE")).toFile(), key.toFile()).build()
        return NettyChannelBuilder.forAddress("media", MEDIA_RPC_PORT).sslContext(tls).build()
    }

    override fun close() {
        if (channel.isInitialized()) channel.value.shutdownNow().awaitTermination(RPC_SECONDS, TimeUnit.SECONDS)
    }
}

private data class RecoveryTarget(
    val id: UUID, val asset: UUID, val expectedVersion: Long, val state: String,
    val titleStatus: String, val currentVersion: Long, val selected: Boolean,
)
