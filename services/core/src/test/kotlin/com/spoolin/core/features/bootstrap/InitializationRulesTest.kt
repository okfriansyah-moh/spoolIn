package com.spoolin.core.features.bootstrap

import com.spoolin.core.features.account.persistence.AccountRole
import com.spoolin.core.features.account.persistence.IdentityRules
import com.spoolin.core.features.crypto.KeyMount
import com.spoolin.core.features.crypto.KeyPurpose
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.security.MessageDigest
import java.util.UUID

class InitializationRulesTest {
    @TempDir
    lateinit var directory: Path

    private val rules = IdentityRules()

    @Test
    fun `checksum ignores password locations and ordering but binds identity role and setup version`() {
        val first = configuration()
        val moved =
            first.accounts.reversed().map {
                InitializationAccount(it.id, it.identity, it.role, Path.of("/new-secret"))
            }
        val movedKeys =
            first.keys.entries
                .reversed()
                .associate { (id, mount) -> id to KeyMount(mount.purpose, Path.of("/new-key")) }
        assertArrayEquals(first.checksum(), InitializationConfiguration("v1", moved, movedKeys).checksum())
        assertFalse(
            MessageDigest.isEqual(
                first.checksum(),
                InitializationConfiguration("v2", first.accounts, first.keys).checksum(),
            ),
        )
        val renamed =
            first.accounts.map {
                InitializationAccount(
                    it.id,
                    rules.identity("Changed name", it.identity.emailDisplay),
                    it.role,
                    it.passwordFile,
                )
            }
        assertFalse(
            MessageDigest.isEqual(first.checksum(), InitializationConfiguration("v1", renamed, first.keys).checksum()),
        )
    }

    @Test
    fun `reject duplicate roles identities and incomplete purpose keys`() {
        val source = configuration()
        assertThrows(IllegalArgumentException::class.java) {
            InitializationConfiguration("v1", listOf(source.accounts[0], source.accounts[0]), source.keys)
        }
        val duplicateIdentity =
            source.accounts.map {
                InitializationAccount(it.id, source.accounts[0].identity, it.role, it.passwordFile)
            }
        assertThrows(IllegalArgumentException::class.java) {
            InitializationConfiguration("v1", duplicateIdentity, source.keys)
        }
        assertThrows(IllegalArgumentException::class.java) {
            InitializationConfiguration("v1", source.accounts, source.keys - "draft")
        }
        assertThrows(IllegalArgumentException::class.java) {
            InitializationConfiguration("invalid version", source.accounts, source.keys)
        }
    }

    @Test
    fun `manifest parser rejects unprotected files duplicates and unrecognized password values`() {
        val manifest = directory.resolve("accounts.json")
        Files.writeString(manifest, "{}")
        Files.setPosixFilePermissions(manifest, PosixFilePermissions.fromString("rw-r--r--"))
        assertThrows(IllegalArgumentException::class.java) { InitializationConfiguration.load(manifest, rules) }
        Files.setPosixFilePermissions(manifest, PosixFilePermissions.fromString("rw-------"))
        Files.writeString(manifest, """{"setupVersion":"v1","setupVersion":"v2","accounts":[],"keys":{}}""")
        assertThrows(Exception::class.java) { InitializationConfiguration.load(manifest, rules) }
        Files.writeString(manifest, """{"setupVersion":"v1","accounts":[],"keys":{},"password":"forbidden"}""")
        assertThrows(IllegalArgumentException::class.java) { InitializationConfiguration.load(manifest, rules) }
    }

    @Test
    fun `protected manifest loads identity references without reading passwords`() {
        val config = configuration()
        val value =
            mapOf(
                "setupVersion" to config.version,
                "accounts" to
                    config.accounts.map {
                        mapOf(
                            "id" to it.id.toString(),
                            "name" to it.identity.name,
                            "email" to it.identity.emailDisplay,
                            "role" to it.role.name,
                            "passwordFile" to it.passwordFile.toString(),
                        )
                    },
                "keys" to
                    config.keys.mapValues { (_, mount) ->
                        mapOf("purpose" to mount.purpose.name, "file" to mount.file.toString())
                    },
            )
        val path = directory.resolve("valid.json")
        Files.writeString(path, JsonMapper.builder().build().writeValueAsString(value))
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"))
        val loaded = InitializationConfiguration.load(path, rules)
        assertEquals("v1", loaded.version)
        assertArrayEquals(config.checksum(), loaded.checksum())
        val symlink = directory.resolve("linked.json")
        Files.createSymbolicLink(symlink, path)
        assertThrows(IllegalArgumentException::class.java) { InitializationConfiguration.load(symlink, rules) }
    }

    @Test
    fun `diagnostic strings exclude names emails and protected file locations`() {
        val config = configuration()
        val diagnostic = config.toString() + config.accounts.joinToString()
        assertFalse(diagnostic.contains("example.invalid"))
        assertFalse(diagnostic.contains("/private/"))
        assertFalse(diagnostic.contains("Viewer account"))
    }

    private fun configuration(): InitializationConfiguration {
        val accounts =
            listOf(
                InitializationAccount(
                    UUID.fromString("00000000-0000-4000-8000-000000000001"),
                    rules.identity("Viewer account", "viewer@example.invalid"),
                    AccountRole.VIEWER,
                    Path.of("/private/viewer"),
                ),
                InitializationAccount(
                    UUID.fromString("00000000-0000-4000-8000-000000000002"),
                    rules.identity("Maintainer account", "maintainer@example.invalid"),
                    AccountRole.MAINTAINER,
                    Path.of("/private/maintainer"),
                ),
            )
        val keys = KeyPurpose.entries.associate { it.name.lowercase() to KeyMount(it, Path.of("/private/" + it.name)) }
        return InitializationConfiguration("v1", accounts, keys)
    }
}
