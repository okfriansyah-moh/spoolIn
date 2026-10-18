package com.spoolin.media.features.asset

import com.spoolin.media.boundary.grpc.MediaPeer
import com.spoolin.media.features.upload.QuarantineStore
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.receipts.committed
import com.spoolin.support.receipts.queryBound
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.ServerCallStreamObserver
import io.grpc.stub.StreamObserver
import spoolin.v1.Spoolin.Asset
import spoolin.v1.Spoolin.AssetReadChunk
import spoolin.v1.Spoolin.AssetReadRequest
import spoolin.v1.Spoolin.Id
import java.util.UUID
import javax.sql.DataSource

class AssetRpc(
    private val dataSource: DataSource,
    private val store: QuarantineStore,
    catalog: CatalogAttachments,
) {
    private val access = AssetAccess(dataSource, catalog)

    fun getAsset(request: Id, response: StreamObserver<Asset>) = respond(response) {
        val row = authorized(parseId(request.id))
        Asset.newBuilder().setId(row.id.toString()).setState(row.state.name).also { builder ->
            row.format?.let { builder.format = it }
            row.width?.let { builder.width = it }
            row.height?.let { builder.height = it }
            row.outputBytes?.let { builder.outputBytes = it }
            row.errorCode?.let { builder.problem = spoolin.v1.Spoolin.Problem.newBuilder().setCode("FIELD_INVALID").build() }
        }.build()
    }

    fun receiptScope(operation: UUID): String {
        val row = dataSource.connection.use { connection -> connection.queryBound(
            "SELECT * FROM asset WHERE upload_operation_id=?", listOf(operation),
        ) { AssetRow.decode(it) }.singleOrNull() } ?: return holdReceiptScope(operation)
        authorize(row)
        return "account:${row.actorId}"
    }

    private fun holdReceiptScope(operation: UUID): String {
        val stored = dataSource.connection.use { connection -> connection.queryBound(
            "SELECT a.*,h.id AS receipt_hold,h.title_id AS receipt_title FROM operation_receipt r " +
                "JOIN asset_reference h ON h.id::text=r.result->>'resultId' JOIN asset a ON a.id=h.asset_id " +
                "WHERE r.id=? AND r.kind IN ('HOLD_RESERVE','HOLD_CONFIRM','HOLD_RELEASE') " +
                "AND r.actor_scope='account:'||a.actor_id::text AND r.state='SUCCEEDED'",
            listOf(operation),
        ) { Triple(AssetRow.decode(it), it.getObject("receipt_hold", UUID::class.java),
            it.getObject("receipt_title", UUID::class.java)) }.singleOrNull() }
            ?: throw Status.NOT_FOUND.asRuntimeException()
        val (asset, hold, title) = stored
        val allowed = when (MediaPeer.current()) {
            "core" -> ActorRpc.current()?.accountId == asset.actorId
            "catalog" -> {
                val current = access.current(hold)
                current.holdId == hold.toString() && current.assetId == asset.id.toString() &&
                    current.titleId == title.toString() && current.hasActorId() &&
                    current.actorId == asset.actorId.toString() && current.state in setOf("PREPARING", "ACTIVE", "RELEASED")
            }
            else -> false
        }
        if (!allowed) throw Status.NOT_FOUND.asRuntimeException()
        return "account:${asset.actorId}"
    }

    fun readAsset(request: AssetReadRequest, response: StreamObserver<AssetReadChunk>) {
        try {
            if (MediaPeer.current() != "core") throw Status.PERMISSION_DENIED.asRuntimeException()
            val asset = parseId(request.assetId)
            val hold = parseId(request.holdId)
            val remote = access.current(hold)
            val opened = dataSource.committed { connection ->
                val row = AssetSql.findLocked(connection, asset) ?: throw Status.NOT_FOUND.asRuntimeException()
                if (row.state != AssetState.READY) throw Status.NOT_FOUND.asRuntimeException()
                access.checkRead(access.hold(connection, hold), remote, asset, hold, request.catalogVersion)
                AssetFile.open(store.outputDirectory(), row)
            }
            val stream = response as? ServerCallStreamObserver<AssetReadChunk>
            if (stream == null) { opened.close(); throw Status.INTERNAL.asRuntimeException() }
            AssetStream(opened, stream, io.grpc.Context.current()).start()
        } catch (failure: Exception) {
            response.onError(safeStatus(failure))
        }
    }

    private fun authorized(id: UUID): AssetRow {
        val row = AssetSql.find(dataSource, id) ?: throw Status.NOT_FOUND.asRuntimeException()
        authorize(row)
        return row
    }

    private fun authorize(row: AssetRow) {
        val allowed = when (MediaPeer.current()) {
            "core" -> ActorRpc.current()?.accountId == row.actorId
            "catalog" -> access.catalogMetadata(row)
            else -> false
        }
        if (!allowed) throw Status.NOT_FOUND.asRuntimeException()
    }

    private fun <T> respond(response: StreamObserver<T>, result: () -> T) {
        try { response.onNext(result()); response.onCompleted() } catch (failure: Exception) {
            response.onError(safeStatus(failure))
        }
    }

    private fun safeStatus(failure: Exception): StatusRuntimeException =
        if (failure is StatusRuntimeException) failure else Status.UNAVAILABLE.withDescription("ASSET_UNAVAILABLE")
            .asRuntimeException()

    private fun parseId(value: String): UUID = runCatching {
        UUID.fromString(value).also { require(it.toString() == value) }
    }.getOrElse { throw Status.INVALID_ARGUMENT.asRuntimeException() }
}
