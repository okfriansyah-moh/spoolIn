package com.spoolin.core.features.connector

import com.spoolin.core.features.session.SessionJdbcFixture
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

class ConnectorOperatorBehaviorTest {
    @TempDir lateinit var root: Path
    private val account = UUID.randomUUID()
    private val credential = UUID.randomUUID()
    private val now = Instant.parse("2026-10-25T10:00:00Z")

    @Test
    fun `issue writes random token only to owned protected file and returns safe metadata`() {
        protectDirectory()
        val output = root.resolve("connector-token")
        val db = SessionJdbcFixture(listOf(listOf(authority()), listOf(mapOf("1" to Timestamp.from(now)))))
        val result =
            ConnectorOperatorCli.execute(
                db.jdbc,
                ConnectorOperatorCommand("issue", account, "CHATGPT", output, ttl = 60),
                operator(),
            )
        assertEquals(true, result["issued"])
        assertEquals("CHATGPT", result["client"])
        assertEquals(now.plusSeconds(60).toString(), result["expiresAt"])
        val raw = Files.readString(output)
        assertEquals(43, raw.length)
        assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(output))
        assertFalse(result.toString().contains(raw))
        assertArrayEquals(ConnectorTokenDigest.sha256(raw.toByteArray()), db.writes.first()[2] as ByteArray)
        assertEquals(1, db.commits)
    }

    @Test
    fun `failed credential persistence removes newly created secret file`() {
        protectDirectory()
        val output = root.resolve("connector-token")
        val db = SessionJdbcFixture(listOf(listOf(authority()), listOf(mapOf("1" to Timestamp.from(now)))))
        db.updateResult = 0
        assertThrows(IllegalStateException::class.java) {
            ConnectorOperatorCli.execute(db.jdbc, ConnectorOperatorCommand("issue", account, "CLAUDE", output), operator())
        }
        assertFalse(Files.exists(output))
        assertEquals(0, db.commits)
    }

    @Test
    fun `issue rejects viewer unsafe directory and existing output without replacing secrets`() {
        protectDirectory()
        val output = root.resolve("connector-token")
        val viewer = SessionJdbcFixture(listOf(listOf(authority() + ("role" to "VIEWER"))))
        assertThrows(IllegalArgumentException::class.java) {
            ConnectorOperatorCli.execute(viewer.jdbc, ConnectorOperatorCommand("issue", account, "CHATGPT", output), operator())
        }
        Files.writeString(output, "preserved")
        val existing = SessionJdbcFixture(listOf(listOf(authority())))
        assertThrows(java.nio.file.FileAlreadyExistsException::class.java) {
            ConnectorOperatorCli.execute(existing.jdbc, ConnectorOperatorCommand("issue", account, "CHATGPT", output), operator())
        }
        assertEquals("preserved", Files.readString(output))
        Files.delete(output)
        Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("rwxr-xr-x"))
        val unsafe = SessionJdbcFixture(listOf(listOf(authority())))
        assertThrows(IllegalArgumentException::class.java) {
            ConnectorOperatorCli.execute(unsafe.jdbc, ConnectorOperatorCommand("issue", account, "CHATGPT", output), operator())
        }
    }

    @Test
    fun `list omits token material and represents current revocation while revoke writes safe activity`() {
        val db =
            SessionJdbcFixture(
                listOf(
                    listOf(authority()),
                    listOf(
                        mapOf(
                            "id" to credential,
                            "expires_at" to Timestamp.from(now),
                            "revoked_at" to Timestamp.from(now),
                        ),
                    ),
                ),
            )
        val listing = ConnectorOperatorCli.execute(db.jdbc, ConnectorOperatorCommand("list", account, "CLAUDE"), operator())
        val row = (listing["credentials"] as List<*>).single() as Map<*, *>
        assertEquals(mapOf("id" to credential.toString(), "expiresAt" to now.toString(), "revoked" to true), row)
        db.enqueue(authority())
        val revoked =
            ConnectorOperatorCli.execute(
                db.jdbc,
                ConnectorOperatorCommand("revoke", account, "CLAUDE", credential = credential),
                operator(),
            )
        assertEquals(mapOf("id" to credential.toString(), "revoked" to true), revoked)
        assertFalse(db.writes.toString().contains("token"))
        assertEquals(2, db.commits)
    }

    @Test
    fun `oversized listing missing revocation target and malformed operator fail explicitly`() {
        val oversized =
            SessionJdbcFixture(
                listOf(
                    listOf(authority()),
                    List(101) {
                        mapOf(
                            "id" to UUID.randomUUID(),
                            "expires_at" to Timestamp.from(now),
                            "revoked_at" to null,
                        )
                    },
                ),
            )
        assertThrows(IllegalArgumentException::class.java) {
            ConnectorOperatorCli.execute(oversized.jdbc, ConnectorOperatorCommand("list", account, "CHATGPT"), operator())
        }
        val missing = SessionJdbcFixture(listOf(listOf(authority())))
        missing.updateResult = 0
        assertThrows(IllegalArgumentException::class.java) {
            ConnectorOperatorCli.execute(
                missing.jdbc,
                ConnectorOperatorCommand("revoke", account, "CHATGPT", credential = credential),
                operator(),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            ConnectorOperatorCli.execute(missing.jdbc, ConnectorOperatorCommand("list", account, "CHATGPT"), "remote-admin")
        }
    }

    private fun authority(): Map<String, Any?> = mapOf("role" to "MAINTAINER", "auth_epoch" to 3L)

    private fun protectDirectory() {
        Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("rwx------"))
    }

    private fun operator() = "local-uid:${Files.getAttribute(root, "unix:uid")}"
}
