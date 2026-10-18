package com.spoolin.core.features.crypto

import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions

class KeyMount(
    val purpose: KeyPurpose,
    val file: Path,
)

class MountedKeys(
    mounts: Map<String, KeyMount>,
    private val reloadFile: Path? = System.getenv("ROTATION_KEYS_FILE")?.let(Path::of),
) : AutoCloseable {
    private class Material(
        val purpose: KeyPurpose,
        val bytes: ByteArray,
    )

    private val materials =
        mounts
            .mapValues { (id, mount) ->
                require(id.matches(Regex("[a-zA-Z0-9_-]{1,64}"))) { "Invalid key identifier" }
                require(Files.isRegularFile(mount.file, NOFOLLOW_LINKS)) { "Protected key file is unavailable" }
                require(
                    Files.getPosixFilePermissions(mount.file, NOFOLLOW_LINKS) ==
                        PosixFilePermissions.fromString("rw-------"),
                ) {
                    "Protected key permissions are invalid"
                }
                require(Files.size(mount.file) == AES_KEY_BYTES.toLong()) { "Protected key size is invalid" }
                Material(mount.purpose, Files.readAllBytes(mount.file).also { require(it.size == AES_KEY_BYTES) })
            }.toMutableMap()

    init {
        independent(materials.values.toList())
    }

    private var closed = false

    @Synchronized
    fun key(
        id: String,
        purpose: KeyPurpose,
    ): ByteArray? {
        if (closed) return null
        if (id !in materials && reloadFile != null) refresh()
        return materials[id]?.takeIf { it.purpose == purpose }?.bytes?.copyOf()
    }

    private fun refresh() {
        val file = reloadFile ?: return
        try {
            MountedKeys(MountedKeyRefresh.read(file), null).use(::merge)
        } catch (_: Exception) {
            throw ProtectionUnavailable()
        }
    }

    private fun merge(fresh: MountedKeys) {
        fresh.materials.forEach { (id, material) -> immutable(id, material) }
        independent((materials + fresh.materials).values.toList())
        fresh.materials.filterKeys { it !in materials }.forEach { (id, material) ->
            materials[id] = Material(material.purpose, material.bytes.copyOf())
        }
    }

    private fun immutable(id: String, material: Material) {
        val current = materials[id]
        val unchanged = current == null ||
            (current.purpose == material.purpose &&
                java.security.MessageDigest.isEqual(current.bytes, material.bytes))
        require(unchanged) {
            "Existing protection key versions are immutable"
        }
    }

    private fun independent(values: List<Material>) {
        require(values.indices.all { first ->
            (first + 1 until values.size).all { second ->
                !java.security.MessageDigest.isEqual(values[first].bytes, values[second].bytes)
            }
        }) { "Protection keys must be independent" }
    }

    @Synchronized
    override fun close() {
        closed = true
        materials.values.forEach { it.bytes.fill(0) }
    }
}
