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

    fun getAsset(
        request: Id,
        response: StreamObserver<Asset>,
    ) = respond(response) {
        val row = authorized(parseId(request.id))
        Asset
            .newBuilder()
            .setId(row.id.toString())
            .setState(row.state.name)
            .also { builder ->
                row.format?.let { builder.format = it }
                row.contentHash?.let { builder.contentHash = java.util.Base64.getEncoder().encodeToString(it) }
                row.width?.let { builder.width = it }
                row.height?.let { builder.height = it }
                row.outputBytes?.let { builder.outputBytes = it }
                row.errorCode?.let {
                    builder.problem =
                        spoolin.v1.Spoolin.Problem
                            .newBuilder()
                            .setCode("FIELD_INVALID")
                            .build()
                }
            }.build()
    }

    fun receiptScope(operation: UUID): String {
        val row =
            dataSource.connection.use { connection ->
                connection
                    .queryBound(
                        "SELECT * FROM asset WHERE upload_operation_id=?",
                        listOf(operation),
                    ) { AssetRow.decode(it) }
                    .singleOrNull()
            } ?: return holdReceiptScope(operation)
        authorize(row)
        return "account:${row.actorId}"
    }

    private fun holdReceiptScope(operation: UUID): String {
        val stored =
            dataSource.connection.use { connection ->
                connection
                    .queryBound(
                        "SELECT a.*,h.id AS receipt_hold,h.title_id AS receipt_title FROM operation_receipt r " +
                            "JOIN hold_operation_binding b ON b.operation_id=r.id " +
                            "JOIN asset_reference h ON h.id=b.hold_id " +
                            "JOIN asset a ON a.id=h.asset_id WHERE r.id=? AND r.kind='HOLD_'||b.verb " +
                            "AND b.actor_id=a.actor_id AND r.actor_scope='account:'||a.actor_id::text " +
                            "AND r.state='SUCCEEDED'",
                        listOf(operation),
                    ) {
                        Triple(
                            AssetRow.decode(it),
                            it.getObject("receipt_hold", UUID::class.java),
                            it.getObject("receipt_title", UUID::class.java),
                        )
                    }.singleOrNull()
            }
                ?: throw Status.NOT_FOUND.asRuntimeException()
        val (asset, hold, title) = stored
        val allowed =
            when (MediaPeer.current()) {
                "core" -> ActorRpc.current()?.accountId == asset.actorId
                "catalog" -> catalogReceipt(asset, hold, title)
                else -> false
            }
        if (!allowed) throw Status.NOT_FOUND.asRuntimeException()
        return "account:${asset.actorId}"
    }

    private fun catalogReceipt(
        asset: AssetRow,
        hold: UUID,
        title: UUID,
    ): Boolean {
        val current = access.current(hold)
        return current.holdId == hold.toString() && current.assetId == asset.id.toString() &&
            current.titleId == title.toString() && current.hasActorId() &&
            current.actorId == asset.actorId.toString() && current.state in setOf("PREPARING", "ACTIVE", "RELEASED")
    }

    fun readAsset(
        request: AssetReadRequest,
        response: StreamObserver<AssetReadChunk>,
    ) = protect(response) {
        if (MediaPeer.current() != "core") throw Status.PERMISSION_DENIED.asRuntimeException()
        val opened = openAsset(request)
        val stream = response as? ServerCallStreamObserver<AssetReadChunk>
        if (stream == null) {
            opened.close()
            throw Status.INTERNAL.asRuntimeException()
        }
        AssetStream(opened, stream, io.grpc.Context.current()).start()
    }

    fun readMoviePoster(request: spoolin.v1.Spoolin.MoviePosterReadRequest, response: StreamObserver<spoolin.v1.Spoolin.MoviePosterImage>) = respond(response) {
        if (MediaPeer.current() != "catalog") throw Status.PERMISSION_DENIED.asRuntimeException()
        val remote = access.current(parseId(request.refId))
        if (remote.state !in setOf("PREPARING","ACTIVE")) throw Status.NOT_FOUND.asRuntimeException()
        val row = readable(AssetSql.find(dataSource,parseId(remote.assetId)))
        if (!remote.hasActorId() || remote.actorId != row.actorId.toString()) throw Status.PERMISSION_DENIED.asRuntimeException()
        if (request.hasAssetId() && request.assetId != row.id.toString()) throw Status.NOT_FOUND.asRuntimeException()
        if (remote.state == "ACTIVE") dataSource.committed { conn ->
            access.checkRead(access.hold(conn,parseId(remote.holdId)),remote,row.id,parseId(remote.holdId),remote.catalogVersion)
        }
        AssetFile.open(store.outputDirectory(),row).use { file ->
            PreparedPosterThumbnail.render(store.outputDirectory(),file.row)
        }
    }


    private fun openAsset(request: AssetReadRequest): AssetFile {
        val asset = parseId(request.assetId)
        val hold = parseId(request.holdId)
        val remote = access.current(hold)
        return dataSource.committed { connection ->
            val row = readable(AssetSql.findLocked(connection, asset))
            access.checkRead(access.hold(connection, hold), remote, asset, hold, request.catalogVersion)
            AssetFile.open(store.outputDirectory(), row)
        }
    }

    private fun authorized(id: UUID): AssetRow {
        val row = AssetSql.find(dataSource, id) ?: throw Status.NOT_FOUND.asRuntimeException()
        authorize(row)
        return row
    }

    private fun authorize(row: AssetRow) {
        val allowed =
            when (MediaPeer.current()) {
                "core" -> ActorRpc.current()?.accountId == row.actorId
                "catalog" -> access.catalogMetadata(row)
                else -> false
            }
        if (!allowed) throw Status.NOT_FOUND.asRuntimeException()
    }
}

private fun readable(row: AssetRow?): AssetRow {
    if (row == null || row.state != AssetState.READY) throw Status.NOT_FOUND.asRuntimeException()
    return row
}

private fun <T> respond(
    response: StreamObserver<T>,
    result: () -> T,
) = protect(response) {
    response.onNext(result())
    response.onCompleted()
}

private fun <T> protect(
    response: StreamObserver<T>,
    operation: () -> Unit,
) {
    try {
        operation()
    } catch (
        failure: StatusRuntimeException,
    ) {
        response.onError(failure)
    } catch (
        _: java.sql.SQLException,
    ) {
        unavailable(response)
    } catch (
        _: java.io.IOException,
    ) {
        unavailable(response)
    } catch (
        _: IllegalArgumentException,
    ) {
        unavailable(response)
    } catch (
        _: IllegalStateException,
    ) {
        unavailable(response)
    } catch (
        _: SecurityException,
    ) {
        unavailable(response)
    } catch (
        _: java.security.GeneralSecurityException,
    ) {
        unavailable(response)
    } catch (_: UnsupportedOperationException) {
        unavailable(response)
    }
}

private fun <T> unavailable(response: StreamObserver<T>) =
    response.onError(
        Status.UNAVAILABLE.withDescription("ASSET_UNAVAILABLE").asRuntimeException(),
    )

private fun parseId(value: String): UUID =
    runCatching {
        UUID.fromString(value).also { require(it.toString() == value) }
    }.getOrElse { throw Status.INVALID_ARGUMENT.asRuntimeException() }
