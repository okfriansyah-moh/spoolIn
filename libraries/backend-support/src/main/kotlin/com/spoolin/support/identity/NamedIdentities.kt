package com.spoolin.support.identity

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.file.Path
import java.security.MessageDigest
import java.sql.Connection
import java.util.UUID

private const val UUID_BYTES = 16
private const val UUID_VERSION_INDEX = 6
private const val UUID_VARIANT_INDEX = 8

/** Configured once per process; compatibility records preserve existing persisted identities. */
object NamedIdentities {
    private val resolver by lazy {
        val configured = System.getenv("SPOOLIN_IDENTITY_REGISTRY")
        check(configured == null || configured.isNotBlank()) { "Identity registry path is empty" }
        NamedIdentityResolver(configured?.let(Path::of)) {
            NamedIdentities::class.java.getResourceAsStream("/identity/frozen-v5.bin")
        }
    }

    fun id(
        namespace: UUID,
        name: ByteArray,
    ): UUID = resolver.id(namespace, name)

    fun verify(connection: Connection) = resolver.verify(connection)
}

/** Separates deployment inputs from UUID generation so startup and migration cases can be verified directly. */
class NamedIdentityResolver(
    private val externalPath: Path?,
    builtin: () -> InputStream?,
) : AutoCloseable {
    private val external = externalPath?.let(LegacyIdentityIndex::external)
    private val frozen =
        try {
            LegacyIdentityIndex.builtin(checkNotNull(builtin()) { "Built-in identity compatibility index is missing" })
        } catch (failure: Throwable) {
            external?.close()
            throw failure
        }

    @Volatile private var closed = false

    fun id(
        namespace: UUID,
        name: ByteArray,
    ): UUID {
        check(!closed) { "Identity resolver is closed" }
        return try {
            val digest = digest(namespace, name)
            external?.find(digest) ?: frozen.find(digest) ?: strongIdentity(digest)
        } catch (failure: Throwable) {
            close()
            throw failure
        }
    }

    fun verify(connection: Connection) {
        check(!closed) { "Identity resolver is closed" }
        try {
            external?.requireUnchanged()
            connection
                .prepareStatement(
                    "SELECT legacy_required,registry_sha256 FROM appdata.identity_hash_migration WHERE singleton=true",
                ).use { query ->
                    query.executeQuery().use { rows ->
                        check(rows.next()) { "Identity migration guard is missing" }
                        val required = rows.getBoolean("legacy_required")
                        check(!rows.wasNull()) { "Identity migration requirement is missing" }
                        val expected = rows.getString("registry_sha256")
                        check(!rows.next()) { "Identity migration guard has duplicate rows" }
                        verifyRegistry(required, expected)
                    }
                }
        } catch (failure: Throwable) {
            close()
            throw failure
        }
    }

    private fun verifyRegistry(
        required: Boolean,
        expected: String?,
    ) {
        check(!required || expected != null) { "Required identity registry digest is missing" }
        if (expected == null) return
        check(expected.matches(Regex("[0-9a-f]{64}"))) { "Identity registry digest is invalid" }
        val registry = checkNotNull(external) { "Required identity registry is not configured" }
        check(
            MessageDigest.isEqual(
                expected.toByteArray(Charsets.US_ASCII),
                registry.sha256.toByteArray(Charsets.US_ASCII),
            ),
        ) {
            "Identity registry digest differs from the database guard"
        }
        registry.requireUnchanged()
        val path = requireNotNull(externalPath)
        LegacyIdentityIndex.requireReady(path.resolveSibling(path.fileName.toString() + ".ready"), expected)
    }

    override fun close() {
        closed = true
        external?.close()
        frozen.close()
    }

    private fun digest(
        namespace: UUID,
        name: ByteArray,
    ): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        val prefix =
            ByteBuffer
                .allocate(UUID_BYTES)
                .putLong(namespace.mostSignificantBits)
                .putLong(namespace.leastSignificantBits)
                .array()
        digest.update(prefix)
        return digest.digest(name)
    }

    private fun strongIdentity(digest: ByteArray): UUID {
        digest[UUID_VERSION_INDEX] = ((digest[UUID_VERSION_INDEX].toInt() and 15) or 128).toByte()
        digest[UUID_VARIANT_INDEX] = ((digest[UUID_VARIANT_INDEX].toInt() and 63) or 128).toByte()
        return ByteBuffer.wrap(digest).let { UUID(it.long, it.long) }
    }
}
