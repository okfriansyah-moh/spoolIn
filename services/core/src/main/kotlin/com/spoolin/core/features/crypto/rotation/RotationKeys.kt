package com.spoolin.core.features.crypto.rotation

import com.spoolin.core.features.crypto.KeyMount
import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.MountedKeys
import com.spoolin.core.features.crypto.committed
import com.spoolin.core.features.crypto.executeBound
import com.spoolin.core.features.crypto.queryBound
import com.spoolin.core.requireProtectedFile
import tools.jackson.core.StreamReadFeature
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Files
import java.nio.file.Path
import javax.sql.DataSource

private const val ROTATION_CONFIGURATION_BYTES = 16384L
private const val MAX_ROTATION_VERSIONS = 64

object RotationKeys {
    fun load(file: Path): MountedKeys {
        requireProtectedFile(file, ROTATION_CONFIGURATION_BYTES)
        val root =
            JsonMapper
                .builder()
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .build()
                .readTree(Files.readAllBytes(file))
        require(root.isObject && root.size() in 2..MAX_ROTATION_VERSIONS) { "Rotation key configuration is invalid" }
        return MountedKeys(
            root.properties().associate { entry ->
                val node = entry.value
                require(node.isObject && node.properties().map { it.key }.toSet() == setOf("purpose", "file"))
                val path = Path.of(node["file"].asString()).also { require(it.isAbsolute) }
                entry.key to KeyMount(KeyPurpose.valueOf(node["purpose"].asString()), path)
            },
        )
    }

    fun registerTarget(
        dataSource: DataSource,
        keys: MountedKeys,
        id: String,
        purpose: KeyPurpose,
    ) {
        require(id.matches(Regex("[A-Za-z0-9_-]{1,64}")))
        require(keys.key(id, purpose)?.also { it.fill(0) } != null) { "Target key material is unavailable" }
        dataSource.committed { connection ->
            connection.executeBound(
                "INSERT INTO appdata.crypto_key_version(id,purpose,state) VALUES(?,?,'READ') " +
                    "ON CONFLICT(id) DO NOTHING",
                id,
                purpose.name,
            )
            val registered =
                connection
                    .queryBound(
                        "SELECT purpose,state FROM appdata.crypto_key_version WHERE id=? FOR UPDATE",
                        listOf(id),
                    ) { it.getString("purpose") to it.getString("state") }
                    .single()
            require(registered.first == purpose.name && registered.second in setOf("READ", "WRITE")) {
                "Target key registration is incompatible"
            }
        }
    }

    fun validateSource(
        dataSource: DataSource,
        keys: MountedKeys,
        id: String,
        purpose: KeyPurpose,
    ) {
        require(keys.key(id, purpose)?.also { it.fill(0) } != null) { "Source key material is unavailable" }
        RotationSourceCanary(dataSource, keys, id, purpose).verify()
    }

    fun validate(
        dataSource: DataSource,
        keys: MountedKeys,
        id: String,
        purpose: KeyPurpose,
    ) {
        RotationTargetCanary(dataSource, keys, id, purpose).verify()
    }
}
