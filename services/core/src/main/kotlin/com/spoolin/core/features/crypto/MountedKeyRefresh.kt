package com.spoolin.core.features.crypto

import com.spoolin.core.requireProtectedFile
import tools.jackson.core.StreamReadFeature
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Files
import java.nio.file.Path

private const val RELOAD_CONFIGURATION_BYTES = 16384L
private const val MAX_RELOAD_VERSIONS = 64

/** Reads only protected configuration; material validation and atomic publication remain under MountedKeys' monitor. */
internal object MountedKeyRefresh {
    fun read(file: Path): Map<String, KeyMount> {
        requireProtectedFile(file, RELOAD_CONFIGURATION_BYTES)
        val root =
            JsonMapper
                .builder()
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .build()
                .readTree(Files.readAllBytes(file))
        require(root.isObject && root.size() in 2..MAX_RELOAD_VERSIONS)
        return root.properties().associate { entry ->
            require(
                entry.value
                    .properties()
                    .map { it.key }
                    .toSet() == setOf("purpose", "file"),
            )
            val path = Path.of(entry.value["file"].asString()).also { require(it.isAbsolute) }
            entry.key to KeyMount(KeyPurpose.valueOf(entry.value["purpose"].asString()), path)
        }
    }
}
