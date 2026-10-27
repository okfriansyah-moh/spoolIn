package com.spoolin.media.features.asset

import com.spoolin.media.fixtures.JdbcBoundary
import com.spoolin.media.fixtures.assetId
import com.spoolin.media.fixtures.assetRecord
import io.grpc.Context
import io.grpc.Status
import io.grpc.stub.ServerCallStreamObserver
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import spoolin.v1.Spoolin.AssetReadChunk
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

class AssetStreamRulesTest {
    @TempDir lateinit var root: Path

    private fun file(bytes: ByteArray): AssetFile {
        val row =
            requireNotNull(AssetSql.find(JdbcBoundary().rows(assetRecord()).source, assetId))
                .copy(outputBytes = bytes.size.toLong(), contentHash = MessageDigest.getInstance("SHA-256").digest(bytes))
        Files.write(root.resolve("$assetId.png"), bytes)
        return AssetFile.open(root, row)
    }

    private class Observer : ServerCallStreamObserver<AssetReadChunk>() {
        val values = mutableListOf<AssetReadChunk>()
        var ready = true
        var cancelled = false
        var complete = false
        var error: Throwable? = null
        var cancelHandler: Runnable? = null
        var readyHandler: Runnable? = null
        var failWrite = false

        override fun onNext(value: AssetReadChunk) {
            if (failWrite) throw IllegalStateException("transport closed")
            values.add(value)
        }

        override fun onError(value: Throwable) {
            error = value
        }

        override fun onCompleted() {
            complete = true
        }

        override fun isReady() = ready

        override fun isCancelled() = cancelled

        override fun setOnCancelHandler(handler: Runnable) {
            cancelHandler = handler
        }

        override fun setOnReadyHandler(handler: Runnable) {
            readyHandler = handler
        }

        override fun setCompression(value: String) {}

        override fun disableAutoInboundFlowControl() {}

        override fun request(count: Int) {}

        override fun setMessageCompression(value: Boolean) {}
    }

    @Test fun `asset stream sends metadata and bounded chunks then closes the inode`() {
        val bytes = ByteArray(130000) { (it % 251).toByte() }
        val file = file(bytes)
        val observer = Observer()
        AssetStream(file, observer, Context.current()).start()
        assertTrue(observer.complete)
        assertNull(observer.error)
        assertEquals(
            "PNG",
            observer.values
                .first()
                .header.format,
        )
        assertEquals(
            bytes.size.toLong(),
            observer.values
                .first()
                .header.outputBytes,
        )
        assertArrayEquals(
            bytes,
            observer.values
                .drop(1)
                .flatMap { it.data.toByteArray().asIterable() }
                .toByteArray(),
        )
        assertTrue(observer.values.drop(1).all { it.data.size() <= 65536 })
        assertFalse(file.channel.isOpen)
        observer.readyHandler!!.run()
        assertEquals(3, observer.values.size)
    }

    @Test fun `read demand cancellation and transport failure release file descriptors`() {
        val paused = file(byteArrayOf(1))
        val observer = Observer().apply { ready = false }
        AssetStream(paused, observer, Context.current()).start()
        assertTrue(observer.values.isEmpty())
        assertTrue(paused.channel.isOpen)
        observer.cancelled = true
        observer.cancelHandler!!.run()
        assertFalse(paused.channel.isOpen)
        val failed = file(byteArrayOf(2))
        val broken = Observer().apply { failWrite = true }
        AssetStream(failed, broken, Context.current()).start()
        assertEquals(Status.Code.UNAVAILABLE, Status.fromThrowable(broken.error!!).code)
        assertFalse(failed.channel.isOpen)
        val cancelled = file(byteArrayOf(3))
        val context = Context.current().withCancellation()
        context.cancel(null)
        AssetStream(cancelled, Observer(), context).start()
        assertFalse(cancelled.channel.isOpen)
    }
}
