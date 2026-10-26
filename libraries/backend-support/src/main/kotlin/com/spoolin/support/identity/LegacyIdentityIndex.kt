package com.spoolin.support.identity

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.StandardOpenOption.READ
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.PosixFilePermission
import java.security.MessageDigest
import java.util.HexFormat
import java.util.UUID

private const val HEADER_BYTES = 16
private const val KEY_BYTES = 32
private const val RECORD_BYTES = 48
private const val MAX_BUILTIN_BYTES = 2 * 1024 * 1024
private const val DIGEST_BUFFER_BYTES = 65536
private val MAGIC = "SPIDIDX1".toByteArray(Charsets.US_ASCII)

/** Read-only compatibility records, keyed by the complete SHA-256 namespace/name digest. */
internal interface LegacyIdentityIndex : AutoCloseable {
    val sha256: String

    fun find(key: ByteArray): UUID?

    fun requireUnchanged()

    companion object {
        fun builtin(input: InputStream): LegacyIdentityIndex =
            input.use {
                val bytes = it.readNBytes(MAX_BUILTIN_BYTES + 1)
                check(bytes.size <= MAX_BUILTIN_BYTES) { "Built-in identity index exceeds its size limit" }
                MemoryIdentityIndex(bytes)
            }

        fun external(path: Path): LegacyIdentityIndex = FileIdentityIndex.open(path)

        fun requireReady(
            path: Path,
            expected: String,
        ) = FileIdentityIndex.requireReady(path, expected)
    }
}

private fun recordCount(
    header: ByteArray,
    size: Long,
): Long {
    check(header.size == HEADER_BYTES && header.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
        "Identity index header is invalid"
    }
    val count = ByteBuffer.wrap(header).getLong(MAGIC.size)
    check(count >= 0 && count <= (Long.MAX_VALUE - HEADER_BYTES) / RECORD_BYTES) { "Identity index count is invalid" }
    check(size == HEADER_BYTES + count * RECORD_BYTES) { "Identity index size is invalid" }
    return count
}

private fun compareKey(
    left: ByteArray,
    right: ByteArray,
    rightOffset: Int = 0,
): Int {
    for (index in 0 until KEY_BYTES) {
        val compared = (left[index].toInt() and 255).compareTo(right[rightOffset + index].toInt() and 255)
        if (compared != 0) return compared
    }
    return 0
}

private fun identity(
    record: ByteArray,
    offset: Int = KEY_BYTES,
): UUID {
    val values = ByteBuffer.wrap(record)
    return UUID(values.getLong(offset), values.getLong(offset + Long.SIZE_BYTES))
}

private class MemoryIdentityIndex(
    private val bytes: ByteArray,
) : LegacyIdentityIndex {
    private val count = recordCount(bytes.take(HEADER_BYTES).toByteArray(), bytes.size.toLong())
    override val sha256: String = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))

    init {
        var previous: ByteArray? = null
        repeat(count.toInt()) { index ->
            val offset = HEADER_BYTES + index * RECORD_BYTES
            val key = bytes.copyOfRange(offset, offset + KEY_BYTES)
            check(previous == null || compareKey(requireNotNull(previous), key) < 0) {
                "Identity index keys are not strictly ordered"
            }
            previous = key
        }
    }

    override fun find(key: ByteArray): UUID? {
        require(key.size == KEY_BYTES)
        var lower = 0L
        var upper = count - 1
        while (lower <= upper) {
            val middle = lower + (upper - lower) / 2
            val offset = HEADER_BYTES + middle.toInt() * RECORD_BYTES
            when (compareKey(key, bytes, offset)) {
                0 -> return identity(bytes, offset + KEY_BYTES)
                in Int.MIN_VALUE..-1 -> upper = middle - 1
                else -> lower = middle + 1
            }
        }
        return null
    }

    override fun requireUnchanged() = Unit

    override fun close() = Unit
}

private class FileIdentityIndex private constructor(
    private val path: Path,
    private val channel: FileChannel,
    private val original: BasicFileAttributes,
) : LegacyIdentityIndex {
    private val count = recordCount(read(0, HEADER_BYTES), original.size())
    override val sha256: String

    init {
        validateOrder()
        sha256 = digest()
        requireUnchanged()
    }

    override fun requireUnchanged() {
        val current = attributes(path)
        check(
            channel.isOpen && current.fileKey() == original.fileKey() &&
                current.size() == original.size() && current.lastModifiedTime() == original.lastModifiedTime(),
        ) { "Identity index changed after validation" }
    }

    override fun find(key: ByteArray): UUID? {
        require(key.size == KEY_BYTES)
        requireUnchanged()
        val result = search(key)
        requireUnchanged()
        return result
    }

    private fun search(key: ByteArray): UUID? {
        var lower = 0L
        var upper = count - 1
        while (lower <= upper) {
            val middle = lower + (upper - lower) / 2
            val record = read(HEADER_BYTES + middle * RECORD_BYTES, RECORD_BYTES)
            when (compareKey(key, record)) {
                0 -> return identity(record)
                in Int.MIN_VALUE..-1 -> upper = middle - 1
                else -> lower = middle + 1
            }
        }
        return null
    }

    private fun validateOrder() {
        var previous: ByteArray? = null
        var index = 0L
        while (index < count) {
            val key = read(HEADER_BYTES + index * RECORD_BYTES, KEY_BYTES)
            check(previous == null || compareKey(requireNotNull(previous), key) < 0) {
                "Identity index keys are not strictly ordered"
            }
            previous = key
            index++
        }
    }

    private fun digest(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteBuffer.allocate(DIGEST_BUFFER_BYTES)
        var position = 0L
        while (position < original.size()) {
            buffer.clear()
            buffer.limit(minOf(buffer.capacity().toLong(), original.size() - position).toInt())
            val count = channel.read(buffer, position)
            check(count > 0) { "Identity index was truncated during validation" }
            digest.update(buffer.array(), 0, count)
            position += count
        }
        return HexFormat.of().formatHex(digest.digest())
    }

    private fun read(
        position: Long,
        size: Int,
    ): ByteArray {
        val buffer = ByteBuffer.allocate(size)
        while (buffer.hasRemaining()) {
            val count = channel.read(buffer, position + buffer.position())
            check(count > 0) { "Identity index record is truncated" }
        }
        return buffer.array()
    }

    override fun close() {
        channel.close()
    }

    companion object {
        fun open(path: Path): LegacyIdentityIndex {
            val before = attributes(path)
            val channel = FileChannel.open(path, READ, NOFOLLOW_LINKS)
            return try {
                val after = attributes(path)
                check(
                    before.fileKey() == after.fileKey() && before.size() == after.size() &&
                        before.lastModifiedTime() == after.lastModifiedTime(),
                ) { "Identity index changed while opening" }
                FileIdentityIndex(path, channel, before)
            } catch (failure: Throwable) {
                channel.close()
                throw failure
            }
        }

        fun requireReady(
            path: Path,
            expected: String,
        ) {
            val original = attributes(path)
            check(original.size() in 64..65) { "Identity migration readiness marker size is invalid" }
            val bytes =
                FileChannel.open(path, READ, NOFOLLOW_LINKS).use { channel ->
                    val buffer = ByteBuffer.allocate(original.size().toInt())
                    while (buffer.hasRemaining()) {
                        check(channel.read(buffer) > 0) { "Identity migration readiness marker is truncated" }
                    }
                    buffer.array()
                }
            val current = attributes(path)
            check(
                original.fileKey() == current.fileKey() && original.size() == current.size() &&
                    original.lastModifiedTime() == current.lastModifiedTime(),
            ) { "Identity migration readiness marker changed while reading" }
            val text = bytes.toString(Charsets.US_ASCII)
            check(
                text == expected || text == expected + "\n",
            ) { "Identity migration readiness marker differs from registry" }
        }

        private fun attributes(path: Path): BasicFileAttributes {
            val attributes = Files.readAttributes(path, BasicFileAttributes::class.java, NOFOLLOW_LINKS)
            check(
                attributes.isRegularFile && attributes.fileKey() != null,
            ) { "Identity index must be a regular immutable file" }
            val permissions =
                try {
                    Files.getPosixFilePermissions(path, NOFOLLOW_LINKS)
                } catch (_: UnsupportedOperationException) {
                    emptySet()
                }
            check(PosixFilePermission.GROUP_WRITE !in permissions && PosixFilePermission.OTHERS_WRITE !in permissions) {
                "Identity index must not be writable by group or others"
            }
            return attributes
        }
    }
}
