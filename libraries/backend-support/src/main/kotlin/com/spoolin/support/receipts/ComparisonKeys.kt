package com.spoolin.support.receipts

import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.security.MessageDigest

private const val KEY_BYTES = 32
private const val KEY_BYTES_LONG = 32L
private const val KEY_CORRECTION =
    "spoolin.operations.comparison-keys: Supply independent protected " +
        "32-byte key mounts and matching active selectors."

object ComparisonKeys {
    fun load(
        paths: Map<String, String>,
        activeId: String,
        activeFile: String,
        otherPurposeFiles: List<String> = emptyList(),
    ): IntentHashes {
        require(paths.isNotEmpty() && paths.keys.all { it.matches(Regex("[a-zA-Z0-9_-]{1,64}")) }) { KEY_CORRECTION }
        require(paths[activeId] == activeFile) { KEY_CORRECTION }
        val keys = paths.mapValues { protectedBytes(it.value) }
        val other = otherPurposeFiles.map(::protectedBytes)
        try {
            requireIndependent(keys.values.toList() + other)
            return IntentHashes(keys, activeId)
        } finally {
            keys.values.forEach { it.fill(0) }
            other.forEach { it.fill(0) }
        }
    }

    private fun protectedBytes(value: String): ByteArray =
        try {
            val path = Path.of(value)
            require(Files.isRegularFile(path, NOFOLLOW_LINKS))
            require(Files.getPosixFilePermissions(path, NOFOLLOW_LINKS) == PosixFilePermissions.fromString("rw-------"))
            require(Files.size(path) == KEY_BYTES_LONG && Files.isReadable(path))
            Files.readAllBytes(path).also { require(it.size == KEY_BYTES) }
        } catch (_: Exception) {
            throw IllegalArgumentException(KEY_CORRECTION)
        }

    private fun requireIndependent(keys: List<ByteArray>) {
        keys.indices.forEach { first ->
            require(
                (first + 1 until keys.size).none { MessageDigest.isEqual(keys[first], keys[it]) },
            ) { KEY_CORRECTION }
        }
    }
}
