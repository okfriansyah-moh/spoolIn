package com.spoolin.support.identity

import com.spoolin.support.fixtures.JdbcBoundary
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.security.MessageDigest
import java.util.Arrays
import java.util.HexFormat
import java.util.UUID

class NamedIdentitiesRulesTest {
    @TempDir lateinit var root: Path
    private val namespace = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8")
    private val legacy = UUID.fromString("9c489390-3db8-5146-99e4-71c3c8712d66")

    @Test
    fun `unrecorded names follow independent SHA256 UUID version eight vector and preserve exact bytes`() {
        resolver().use { identities ->
            val name = "www.example.com".toByteArray()
            val result = identities.id(namespace, name)
            assertEquals(UUID.fromString("5c146b14-3c52-8afd-938a-375d0df1fbf6"), result)
            assertEquals(8, result.version())
            assertEquals(2, result.variant())
            assertEquals(result, identities.id(namespace, name))
            assertNotEquals(result, identities.id(namespace, "WWW.example.com".toByteArray()))
            assertNotEquals(result, identities.id(namespace, "www.example.com ".toByteArray()))
            assertNotEquals(result, identities.id(UUID(0, 0), name))
        }
    }

    @Test
    fun `recorded work identity survives while unrecorded work gets strong identity`() {
        val parent = UUID.fromString("95c65556-47e7-4150-a739-9ad8d065495e")
        val bytes = index(listOf(key(parent, "followup".toByteArray()) to legacy))
        resolver(builtin = bytes).use { identities ->
            assertEquals(legacy, identities.id(parent, "followup".toByteArray()))
            assertEquals(8, identities.id(parent, "other".toByteArray()).version())
        }
    }

    @Test
    fun `external records override builtin collisions and missing external keys still use builtin compatibility`() {
        val name = "recorded".toByteArray()
        val builtin = index(listOf(key(namespace, name) to legacy, key(namespace, "builtin".toByteArray()) to legacy))
        val externalId = UUID.fromString("00000000-0000-5000-8000-000000000001")
        val external = file(index(listOf(key(namespace, name) to externalId)))
        resolver(external, builtin).use { identities ->
            assertEquals(externalId, identities.id(namespace, name))
            assertEquals(legacy, identities.id(namespace, "builtin".toByteArray()))
            assertEquals(8, identities.id(namespace, "unrecorded".toByteArray()).version())
        }
    }

    @Test
    fun `external binary search locates first middle last and absent keys across unsigned ordering`() {
        val entries =
            (0 until 257).map { index ->
                val name = "record-$index".toByteArray()
                key(namespace, name) to UUID(0x5000, Long.MIN_VALUE + index)
            }
        val external = file(index(entries))
        resolver(external).use { identities ->
            for (index in listOf(0, 1, 128, 255, 256)) {
                assertEquals(
                    UUID(0x5000, Long.MIN_VALUE + index),
                    identities.id(namespace, "record-$index".toByteArray()),
                )
            }
            assertEquals(8, identities.id(namespace, "not-recorded".toByteArray()).version())
        }
    }

    @Test
    fun `startup verifies required and explicitly pinned external digest and refuses missing mismatch or malformed guards`() {
        val external = file(index(emptyList()))
        val digest = digest(Files.readAllBytes(external))
        ready(external, digest + "\n")
        for (required in listOf(true, false)) {
            resolver(external).use { identities ->
                identities.verify(guard(required, digest).connection)
                assertEquals(8, identities.id(namespace, "fresh".toByteArray()).version())
            }
        }
        resolver().use { it.verify(guard(false, null).connection) }
        val cases = listOf(true to null, true to "0".repeat(64), false to "0".repeat(64), false to "invalid")
        for ((required, expected) in cases) {
            val identities = resolver(external)
            assertThrows(IllegalStateException::class.java) { identities.verify(guard(required, expected).connection) }
            assertThrows(IllegalStateException::class.java) { identities.id(namespace, "fresh".toByteArray()) }
        }
        for (required in listOf(true, false)) {
            resolver().use { identities ->
                assertThrows(
                    IllegalStateException::class.java,
                ) { identities.verify(guard(required, digest).connection) }
            }
        }
    }

    @Test
    fun `startup refuses absent duplicate or null requirement guard and closes resolver`() {
        val rows =
            listOf(
                emptyList(),
                listOf(mapOf("legacy_required" to null, "registry_sha256" to null)),
                List(2) {
                    mapOf("legacy_required" to false, "registry_sha256" to null)
                },
            )
        for (records in rows) {
            val db = JdbcBoundary().apply { queries.add(records) }
            val identities = resolver()
            assertThrows(IllegalStateException::class.java) { identities.verify(db.connection) }
            assertThrows(IllegalStateException::class.java) { identities.id(namespace, byteArrayOf()) }
        }
    }

    @Test
    fun `missing truncated or invalid header count ordering and duplicate indexes fail rather than fallback`() {
        val low = ByteArray(32)
        val high = ByteArray(32) { 255.toByte() }
        val malformed =
            listOf(
                byteArrayOf(),
                index(emptyList()).copyOf(15),
                index(emptyList()).also { it[0] = 0 },
                header(-1),
                header(Long.MAX_VALUE),
                header(1),
                index(emptyList()) + byteArrayOf(0),
                rawIndex(listOf(high to legacy, low to legacy)),
                rawIndex(listOf(low to legacy, low to legacy)),
            )
        for (bytes in malformed) {
            assertThrows(IllegalStateException::class.java) { resolver(builtin = bytes) }
            assertThrows(IllegalStateException::class.java) { resolver(file(bytes), index(emptyList())) }
        }
        assertThrows(java.nio.file.NoSuchFileException::class.java) { resolver(root.resolve("missing")) }
        assertThrows(IllegalStateException::class.java) { resolver(root) }
        assertThrows(IllegalStateException::class.java) { NamedIdentityResolver(null) { null } }
        assertThrows(IllegalStateException::class.java) { resolver(builtin = ByteArray(2 * 1024 * 1024 + 1)) }
    }

    @Test
    fun `external modification truncation replacement symlink and unsafe permissions fail closed`() {
        val builtin = index(listOf(key(namespace, "recorded".toByteArray()) to legacy))
        val path = file(index(emptyList()))
        val identities = resolver(path, builtin)
        Files.write(path, byteArrayOf(0))
        assertThrows(IllegalStateException::class.java) { identities.id(namespace, "recorded".toByteArray()) }
        val replaced = file(index(emptyList()))
        resolver(replaced, builtin).use { opened ->
            val replacement = file(index(emptyList()))
            Files.move(replacement, replaced, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            assertThrows(IllegalStateException::class.java) { opened.id(namespace, "recorded".toByteArray()) }
        }
        val original = file(index(emptyList()))
        val link = root.resolve("linked")
        Files.createSymbolicLink(link, original)
        assertThrows(IllegalStateException::class.java) { resolver(link) }
        Files.setPosixFilePermissions(original, PosixFilePermissions.fromString("rw-rw-r--"))
        assertThrows(IllegalStateException::class.java) { resolver(original) }
    }

    @Test
    fun `coordinated cutover marker is mandatory and exact whenever database pins a registry`() {
        val external = file(index(emptyList()))
        val hash = digest(Files.readAllBytes(external))
        resolver(external).use { identities ->
            assertThrows(
                java.nio.file.NoSuchFileException::class.java,
            ) { identities.verify(guard(true, hash).connection) }
        }
        for (text in listOf("0".repeat(64), hash.uppercase(), hash + "\r\n", hash + "\n\n")) {
            ready(external, text)
            resolver(external).use { identities ->
                assertThrows(IllegalStateException::class.java) { identities.verify(guard(false, hash).connection) }
            }
        }
        ready(external, hash)
        resolver(external).use { it.verify(guard(true, hash).connection) }
        val marker = external.resolveSibling(external.fileName.toString() + ".ready")
        Files.setPosixFilePermissions(marker, PosixFilePermissions.fromString("rw-rw-r--"))
        resolver(external).use { identities ->
            assertThrows(IllegalStateException::class.java) { identities.verify(guard(true, hash).connection) }
        }
        Files.delete(marker)
        val target = file(hash.toByteArray())
        Files.createSymbolicLink(marker, target)
        resolver(external).use { identities ->
            assertThrows(IllegalStateException::class.java) { identities.verify(guard(true, hash).connection) }
        }
    }

    private fun ready(
        index: Path,
        text: String,
    ) {
        val marker = index.resolveSibling(index.fileName.toString() + ".ready")
        Files.writeString(marker, text)
        Files.setPosixFilePermissions(marker, PosixFilePermissions.fromString("rw-------"))
    }

    private fun resolver(
        path: Path? = null,
        builtin: ByteArray = index(emptyList()),
    ) = NamedIdentityResolver(path) {
        ByteArrayInputStream(builtin)
    }

    private fun guard(
        required: Boolean,
        hash: String?,
    ) = JdbcBoundary().rows(
        mapOf(
            "legacy_required" to required,
            "registry_sha256" to hash,
        ),
    )

    private fun file(bytes: ByteArray): Path =
        Files.createTempFile(root, "index", ".bin").also {
            Files.write(it, bytes)
            Files.setPosixFilePermissions(it, PosixFilePermissions.fromString("rw-------"))
        }

    private fun header(count: Long): ByteArray =
        ByteBuffer
            .allocate(16)
            .put("SPIDIDX1".toByteArray(Charsets.US_ASCII))
            .putLong(count)
            .array()

    private fun index(entries: List<Pair<ByteArray, UUID>>) =
        rawIndex(entries.sortedWith { left, right -> Arrays.compareUnsigned(left.first, right.first) })

    private fun rawIndex(entries: List<Pair<ByteArray, UUID>>): ByteArray =
        ByteBuffer
            .allocate(16 + entries.size * 48)
            .apply {
                put(header(entries.size.toLong()))
                entries.forEach { (key, uuid) ->
                    put(key)
                    putLong(uuid.mostSignificantBits)
                    putLong(uuid.leastSignificantBits)
                }
            }.array()

    private fun key(
        namespace: UUID,
        name: ByteArray,
    ): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(
            ByteBuffer
                .allocate(
                    16,
                ).putLong(namespace.mostSignificantBits)
                .putLong(namespace.leastSignificantBits)
                .array() +
                name,
        )

    private fun digest(bytes: ByteArray): String =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))
}
