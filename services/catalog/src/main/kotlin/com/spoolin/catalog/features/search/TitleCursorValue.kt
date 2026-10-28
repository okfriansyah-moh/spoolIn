package com.spoolin.catalog.features.search

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.UUID

/** Three bytes per original Unicode scalar bound a 255-character title to 765 bytes. */
internal object TitleCursorValue {
    private const val FORMAT = 1
    private const val MAX_SCOPE_LENGTH = 64
    private const val MAX_VALUE_POINTS = 255
    private const val HASH_BYTES = 32
    private const val ANCHOR_MARKER = 65535
    private const val SCALAR_SHIFT = 16
    private const val LOW_SCALAR_MASK = 65535

    fun encode(
        key: CatalogCursor.Keyset,
        scope: String,
        expiry: Long,
    ): ByteArray {
        val points = requireNotNull(key.sortValue).codePoints().toArray()
        require(scope.length <= MAX_SCOPE_LENGTH)
        val output = ByteArrayOutputStream()
        DataOutputStream(output).use { stream ->
            stream.writeByte(FORMAT)
            stream.writeInt(key.releaseYear ?: 0)
            stream.writeByte(key.releaseMonth ?: 0)
            stream.writeByte(key.releaseDay ?: 0)
            stream.writeLong(key.id.mostSignificantBits)
            stream.writeLong(key.id.leastSignificantBits)
            stream.writeUTF(scope)
            stream.writeLong(expiry)
            writeValue(stream, points, requireNotNull(key.sortValue))
        }
        return output.toByteArray()
    }

    fun decode(payload: ByteArray): Map<String, Any> =
        DataInputStream(ByteArrayInputStream(payload)).use { stream ->
            require(stream.readUnsignedByte() == FORMAT)
            val year = stream.readInt()
            val month = stream.readUnsignedByte()
            val day = stream.readUnsignedByte()
            val id = UUID(stream.readLong(), stream.readLong())
            val scope = stream.readUTF()
            require(scope.length <= MAX_SCOPE_LENGTH)
            val expiry = stream.readLong()
            val count = stream.readUnsignedShort()
            val value = readFields(stream, count)
            require(stream.available() == 0)
            buildMap {
                if (year != 0) put("y", year.toLong())
                if (month != 0) put("m", month.toLong())
                if (day != 0) put("d", day.toLong())
                put("u", id.toString())
                put("s", scope)
                put("e", expiry)
                putAll(value)
            }
        }

    private fun readFields(
        stream: DataInputStream,
        count: Int,
    ): Map<String, String> {
        if (count == ANCHOR_MARKER) {
            val bytes = ByteArray(HASH_BYTES).also { stream.readFully(it) }
            return mapOf("h" to java.util.HexFormat.of().formatHex(bytes))
        }
        require(count <= MAX_VALUE_POINTS)
        return mapOf("v" to readValue(stream, count))
    }

    fun hash(value: String): String = java.util.HexFormat.of().formatHex(digest(value))

    private fun digest(value: String): ByteArray =
        java.security.MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))

    private fun writeValue(
        stream: DataOutputStream,
        points: IntArray,
        value: String,
    ) {
        if (points.size > MAX_VALUE_POINTS) {
            stream.writeShort(ANCHOR_MARKER)
            stream.write(digest(value))
        } else {
            stream.writeShort(points.size)
            points.forEach { point ->
                stream.writeByte(point shr SCALAR_SHIFT)
                stream.writeShort(point and LOW_SCALAR_MASK)
            }
        }
    }

    private fun readValue(
        stream: DataInputStream,
        count: Int,
    ): String =
        buildString {
            repeat(count) {
                val point = (stream.readUnsignedByte() shl SCALAR_SHIFT) or stream.readUnsignedShort()
                require(Character.isValidCodePoint(point) && point !in Character.MIN_SURROGATE.code..Character.MAX_SURROGATE.code)
                appendCodePoint(point)
            }
        }
}
