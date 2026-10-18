package com.spoolin.core.features.bootstrap

import com.spoolin.core.features.crypto.KeyMount
import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.requireProtectedFile
import tools.jackson.core.StreamReadFeature
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

private const val READ_KEY_FILE_LIMIT = 16384L
private const val READ_KEY_COUNT_LIMIT = 64
private const val READ_KEY_BYTES = 32

internal object InitializationReadKeys {
    fun merge(
        original: Map<String, KeyMount>,
        path: Path?,
    ): Map<String, KeyMount> {
        if (path == null) return original
        requireProtectedFile(path, READ_KEY_FILE_LIMIT)
        val root =
            JsonMapper
                .builder()
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .build()
                .readTree(Files.readAllBytes(path))
        require(root.isObject && root.size() in 1..READ_KEY_COUNT_LIMIT)
        val additional =
            root.properties().associate { entry ->
                val node = entry.value
                require(entry.key.matches(Regex("[A-Za-z0-9_-]{1,64}")))
                require(node.isObject && node.properties().map { it.key }.toSet() == setOf("purpose", "file"))
                require(node["purpose"].isString && node["file"].isString)
                val mount = KeyMount(KeyPurpose.valueOf(node["purpose"].asString()), Path.of(node["file"].asString()))
                require(mount.file.isAbsolute)
                original[entry.key]?.let { existing ->
                    require(existing.purpose == mount.purpose)
                    require(sameMaterial(existing.file, mount.file)) { "Initialization read key conflicts" }
                }
                entry.key to mount
            }
        return original + additional
    }

    private fun sameMaterial(
        first: Path,
        second: Path,
    ): Boolean {
        requireProtectedFile(first, READ_KEY_BYTES.toLong())
        requireProtectedFile(second, READ_KEY_BYTES.toLong())
        val left = Files.readAllBytes(first)
        val right = Files.readAllBytes(second)
        return try {
            left.size == READ_KEY_BYTES && right.size == READ_KEY_BYTES && MessageDigest.isEqual(left, right)
        } finally {
            left.fill(0)
            right.fill(0)
        }
    }
}
