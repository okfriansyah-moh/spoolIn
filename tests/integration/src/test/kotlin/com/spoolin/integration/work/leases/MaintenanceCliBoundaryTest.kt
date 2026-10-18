package com.spoolin.integration.work.leases

import com.spoolin.core.boundary.bootstrap.OwnerFixture
import com.spoolin.support.maintenance.RetentionMaintenanceCli
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.sql.SQLException
import java.util.UUID

class MaintenanceCliBoundaryTest {
    @Test
    fun `each owner uses maintenance credentials with inspected no effects and executed bounded retention`() {
        for (owner in listOf("core", "catalog", "media")) {
            fixture(owner) { installation -> verifyOwner(installation, owner) }
        }
    }

    private fun verifyOwner(
        fixture: OwnerFixture,
        owner: String,
    ) {
        fixture.connection(owner).use { connection ->
            connection.executeBound(
                """
                INSERT INTO appdata.operation_receipt(
                  id,actor_scope,kind,request_hash,state,result,correlation_id,expires_at)
                SELECT gen_random_uuid(),'SERVICE:test','TEST',decode(repeat('00',32),'hex'),'SUCCEEDED',
                  '{"ok":true}'::jsonb,?,CURRENT_TIMESTAMP-INTERVAL '1 day' FROM generate_series(1,101)
                """.trimIndent(),
                UUID.randomUUID(),
            )
        }
        val inspect = RetentionMaintenanceCli.run(args(owner, "hourly", "inspect"), environment(fixture, owner)::get)
        assertFalse(inspect.getValue("executed") as Boolean)
        assertEquals(emptyMap<String, Int>(), inspect.getValue("counts"))
        assertEquals(101, remaining(fixture, owner))
        val execute = RetentionMaintenanceCli.run(args(owner, "hourly", "execute"), environment(fixture, owner)::get)
        assertEquals(
            mapOf("receiptBodies" to 100),
            (execute.getValue("counts") as Map<*, *>).filterKeys { it == "receiptBodies" },
        )
        assertEquals(1, remaining(fixture, owner))
        val daily = RetentionMaintenanceCli.run(args(owner, "daily", "execute"), environment(fixture, owner)::get)
        assertEquals(0, (daily.getValue("counts") as Map<*, *>)["work"])
        assertEquals(1, remaining(fixture, owner))
    }

    @Test
    fun `strict arguments are rejected before any credential lookup`() {
        val good = args("core", "hourly", "inspect")
        val invalid =
            listOf(
                good + "--extra=value",
                good + "--owner=core",
                args("foreign", "hourly", "inspect"),
                args("core", "vacuum", "execute"),
                args("core", "hourly", "unknown"),
                arrayOf("--owner=core"),
            )
        for (arguments in invalid) {
            assertThrows(IllegalArgumentException::class.java) {
                RetentionMaintenanceCli.run(arguments) { error("Invalid arguments must not read credentials") }
            }
        }
    }

    @Test
    fun `runtime password cannot authenticate the maintenance role and plaintext database TLS is refused`() {
        fixture("catalog") { fixture ->
            val wrong = environment(fixture, "catalog").toMutableMap()
            wrong["CATALOG_DB_MAINTENANCE_PASSWORD_FILE"] = fixture.passwords.getValue("catalog-runtime").toString()
            assertThrows(SQLException::class.java) {
                RetentionMaintenanceCli.run(args("catalog", "hourly", "execute"), wrong::get)
            }
            val insecure = environment(fixture, "catalog").toMutableMap()
            insecure["CATALOG_DB_URL"] = "jdbc:postgresql://localhost:5432/spoolin_catalog?sslmode=disable"
            assertThrows(IllegalArgumentException::class.java) {
                RetentionMaintenanceCli.run(args("catalog", "hourly", "execute"), insecure::get)
            }
        }
    }

    private fun environment(
        fixture: OwnerFixture,
        owner: String,
    ): Map<String, String> {
        val prefix = owner.uppercase(java.util.Locale.ROOT)
        return mapOf(
            "${prefix}_DB_URL" to fixture.url(owner),
            "${prefix}_DB_MAINTENANCE_PASSWORD_FILE" to fixture.passwords.getValue("$owner-maintenance").toString(),
        )
    }

    private fun args(
        owner: String,
        schedule: String,
        mode: String,
    ): Array<String> = arrayOf("--owner=$owner", "--schedule=$schedule", "--mode=$mode")

    private fun remaining(
        fixture: OwnerFixture,
        owner: String,
    ): Int =
        fixture.connection(owner).use { connection ->
            connection
                .queryBound("SELECT count(*) FROM appdata.operation_receipt WHERE result IS NOT NULL", emptyList()) {
                    it.getInt(1)
                }.single()
        }

    private fun fixture(
        owner: String,
        check: (OwnerFixture) -> Unit,
    ) {
        OwnerFixture("maintenance-cli.json").start().use { fixture ->
            fixture.membership(owner, "grant")
            migrateOwner(fixture, owner)
            fixture.grants(owner)
            fixture.membership(owner, "revoke")
            check(fixture)
        }
    }
}
