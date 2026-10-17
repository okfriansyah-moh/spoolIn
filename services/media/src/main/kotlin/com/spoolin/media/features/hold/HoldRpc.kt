@file:Suppress("ThrowsCount")

package com.spoolin.media.features.hold

import com.spoolin.media.features.asset.AssetSql
import com.spoolin.media.features.asset.AssetState
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.receipts.committed
import io.grpc.Status
import io.grpc.stub.StreamObserver
import spoolin.v1.Spoolin.Attachment
import spoolin.v1.Spoolin.Hold
import java.util.UUID
import javax.sql.DataSource

class HoldRpc(
    private val dataSource: DataSource,
) {
    fun reserve(
        request: Hold,
        response: StreamObserver<Attachment>,
    ) {
        try {
            val holdId = parseUuid(request.holdId)
            val assetId = parseUuid(request.assetId)
            val titleId = parseUuid(request.titleId)
            ActorRpc.current() ?: throw Status.PERMISSION_DENIED.asRuntimeException()
            dataSource.committed { connection ->
                val asset =
                    AssetSql.findLocked(connection, assetId)
                        ?: throw Status.NOT_FOUND.withDescription("ASSET_NOT_FOUND").asRuntimeException()
                if (asset.state != AssetState.READY) {
                    throw Status.FAILED_PRECONDITION.withDescription("ASSET_NOT_READY").asRuntimeException()
                }
                if (HoldSql.insertReserved(connection, holdId, assetId, titleId) != 1) {
                    throw Status.ALREADY_EXISTS.asRuntimeException()
                }
            }
            response.onNext(attachmentProto(holdId, "RESERVED", request))
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        }
    }

    fun confirm(
        request: Hold,
        response: StreamObserver<Attachment>,
    ) {
        try {
            val holdId = parseUuid(request.holdId)
            val version = request.catalogVersion
            ActorRpc.current() ?: throw Status.PERMISSION_DENIED.asRuntimeException()
            val updated =
                dataSource.committed { connection ->
                    HoldSql.confirmActive(connection, holdId, version)
                }
            if (updated != 1) throw Status.FAILED_PRECONDITION.asRuntimeException()
            response.onNext(attachmentProto(holdId, "ACTIVE", request))
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        }
    }

    fun release(
        request: Hold,
        response: StreamObserver<Attachment>,
    ) {
        try {
            val holdId = parseUuid(request.holdId)
            ActorRpc.current() ?: throw Status.PERMISSION_DENIED.asRuntimeException()
            dataSource.committed { connection -> HoldSql.release(connection, holdId) }
            response.onNext(attachmentProto(holdId, "RELEASED", request))
            response.onCompleted()
        } catch (e: io.grpc.StatusRuntimeException) {
            response.onError(e)
        }
    }

    private fun attachmentProto(
        holdId: UUID,
        state: String,
        request: Hold,
    ): Attachment =
        Attachment
            .newBuilder()
            .setHoldId(holdId.toString())
            .setState(state)
            .setAssetId(request.assetId)
            .setTitleId(request.titleId)
            .also { b ->
                if (request.hasCatalogVersion()) b.catalogVersion = request.catalogVersion
            }.build()

    private fun parseUuid(value: String): UUID =
        runCatching { UUID.fromString(value).also { require(it.toString() == value) } }
            .getOrElse { throw Status.INVALID_ARGUMENT.asRuntimeException() }
}
