package com.spoolin.media.features.asset

import com.google.protobuf.ByteString
import com.spoolin.media.features.processing.ProcessingBounds
import io.grpc.Context
import io.grpc.Status
import io.grpc.stub.ServerCallStreamObserver
import spoolin.v1.Spoolin.AssetReadChunk
import spoolin.v1.Spoolin.AssetReadHeader
import java.nio.ByteBuffer

/** Demand-driven chunks: no unbounded file buffering, and cancellation closes the open inode. */
internal class AssetStream(
    private val file: AssetFile,
    private val response: ServerCallStreamObserver<AssetReadChunk>,
    private val context: Context,
) {
    private var headerSent = false
    private var closed = false
    private var sent = 0L
    private val buffer = ByteBuffer.allocate(ProcessingBounds.CHUNK_BYTES)

    fun start() {
        response.setOnCancelHandler { synchronized(this) { close() } }
        response.setOnReadyHandler(::drain)
        drain()
    }

    @Synchronized
    private fun drain() {
        if (closed) return
        try {
            while (response.isReady && !closed) {
                if (response.isCancelled || context.isCancelled) { close(); return }
                if (!headerSent) {
                    response.onNext(header())
                    headerSent = true
                } else {
                    emitChunk()
                }
            }
        } catch (_: Exception) {
            close()
            if (!response.isCancelled) response.onError(Status.UNAVAILABLE.withDescription("ASSET_STREAM_FAILED")
                .asRuntimeException())
        }
    }

    private fun emitChunk() {
        buffer.clear()
        val size = file.channel.read(buffer)
        if (size == -1) {
            check(sent == file.row.outputBytes)
            close()
            response.onCompleted()
            return
        }
        sent += size
        check(sent <= requireNotNull(file.row.outputBytes))
        buffer.flip()
        response.onNext(AssetReadChunk.newBuilder().setData(ByteString.copyFrom(buffer)).build())
    }

    private fun header(): AssetReadChunk {
        val row = file.row
        val header = AssetReadHeader.newBuilder().setFormat(requireNotNull(row.format)).setOutputBytes(requireNotNull(row.outputBytes))
            .setWidth(requireNotNull(row.width)).setHeight(requireNotNull(row.height))
            .setContentHash(ByteString.copyFrom(requireNotNull(row.contentHash))).build()
        return AssetReadChunk.newBuilder().setHeader(header).build()
    }

    private fun close() {
        if (closed) return
        closed = true
        file.close()
    }
}
