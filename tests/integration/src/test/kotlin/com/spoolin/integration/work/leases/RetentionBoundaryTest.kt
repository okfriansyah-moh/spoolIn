package com.spoolin.integration.work.leases

import com.spoolin.core.boundary.bootstrap.OwnerFixture
import com.spoolin.support.maintenance.MaintenanceDataSources
import com.spoolin.support.maintenance.RetentionMaintenance
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import javax.sql.DataSource

class RetentionBoundaryTest {
    @Test
    fun `core expiry batches preserve live rows and seven day session reconciliation without decrypting drafts`() {
        com.spoolin.core.recovery.crypto.CryptoFixture("core-retention.json").use { fixture ->
            val account = retentionAccount(fixture)
            seedExpiredIdentityRows(fixture.pool, account)
            val source =
                MaintenanceDataSources.open(
                    "core",
                    fixture.owner.url("core"),
                    fixture.owner.passwords.getValue("core-maintenance"),
                )
            val counts = RetentionMaintenance(source, "core").hourly()
            for (key in listOf("sessions", "credentials", "drafts", "rateWindows")) {
                assertEquals(100, counts.getValue(key))
            }
            assertEquals(2, count(fixture.pool, "browser_session"))
            assertEquals(1, count(fixture.pool, "mcp_credential"))
            assertEquals(1, count(fixture.pool, "form_draft"))
            assertEquals(1, count(fixture.pool, "request_rate_window"))
        }
    }

    @Test
    fun `catalog receipt retention uses isolated maintenance role and preserves request identities`() =
        fixture("catalog") { pool, maintenance, _ ->
            repeat(101) { receipt(pool) }
            assertEquals(100, maintenance.hourly().getValue("receiptBodies"))
            assertEquals(101, count(pool, "operation_receipt"))
            assertEquals(1, maintenance.hourly().getValue("receiptBodies"))
        }

    @Test
    fun `hourly receipt body purge limits each batch and retains identity and outcome evidence`() =
        fixture { pool, maintenance, _ ->
            repeat(101) { receipt(pool) }
            assertEquals(100, maintenance.hourly().getValue("receiptBodies"))
            assertEquals(101, count(pool, "operation_receipt"))
            assertEquals(1, scalar(pool, "SELECT count(*) FROM operation_receipt WHERE result IS NOT NULL"))
            assertEquals(1, maintenance.hourly().getValue("receiptBodies"))
            assertEquals(101, count(pool, "operation_receipt"))
        }

    @Test
    fun `daily terminal work preserves ninety day notification summaries and seven day ordinary payload boundary`() =
        fixture { pool, maintenance, _ ->
            val old = work(pool, "DONE", 91)
            val recent = work(pool, "DONE", 8)
            val fresh = work(pool, "DONE", 6)
            val notification = work(pool, "DONE", 8, notification = true)
            val result = maintenance.daily()
            assertEquals(1, result.getValue("work"))
            assertEquals(2, result.getValue("payloads"))
            assertEquals(0, scalar(pool, "SELECT count(*) FROM durable_work WHERE id='$old'"))
            assertEquals(
                1,
                scalar(pool, "SELECT count(*) FROM durable_work WHERE id='$recent' AND payload='{}'::jsonb"),
            )
            assertEquals(
                1,
                scalar(pool, "SELECT count(*) FROM durable_work WHERE id='$fresh' AND payload<>'{}'::jsonb"),
            )
            assertEquals(
                1,
                scalar(
                    pool,
                    "SELECT count(*) FROM durable_work WHERE id='$notification' AND notification_summary IS NOT NULL",
                ),
            )
        }

    @Test
    fun `unresolved sibling pending receipt and unmatched uncertain attempts retain all evidence`() =
        fixture { pool, maintenance, _ ->
            val operation = receipt(pool)
            val sibling = work(pool, "EXHAUSTED", 100, operation)
            val blocked = work(pool, "DONE", 100, operation)
            val pending = receipt(pool, "PENDING")
            val waiting = work(pool, "DONE", 100, pending)
            val uncertain = work(pool, "DONE", 100)
            attempts(pool, uncertain, "UNCERTAIN")
            val unmatched = work(pool, "DONE", 100)
            attempts(pool, unmatched, null)
            assertEquals(0, maintenance.daily().getValue("work"))
            for (id in listOf(sibling, blocked, waiting, uncertain, unmatched)) {
                assertEquals(
                    1,
                    scalar(pool, "SELECT count(*) FROM durable_work WHERE id='$id' AND payload<>'{}'::jsonb"),
                )
            }
            assertEquals(3, count(pool, "operational_event"))
        }

    @Test
    fun `resolved phases purge together and asset output hold and permanent receipt survive metadata cleanup`() =
        fixture { pool, maintenance, _ ->
            val operation = receipt(pool)
            val work = work(pool, "DONE", 100, operation)
            attempts(pool, work, "SUCCEEDED")
            val asset = UUID.randomUUID()
            pool.connection.use {
                it.executeBound(
                    """
                    INSERT INTO asset(id,upload_operation_id,actor_id,state,input_bytes,policy_revision,storage_key,
                      content_hash,format,output_bytes,width,height,processing_work_id,processing_staging_key,
                      processing_input_hash,processing_created_at)
                    VALUES (?,?,?,'READY',3,'v1',?,decode(repeat('01',32),'hex'),'PNG',3,1,1,?,
                      'quarantine/test',decode(repeat('02',32),'hex'),CURRENT_TIMESTAMP-INTERVAL '100 days')
                    """.trimIndent(),
                    asset,
                    operation,
                    UUID.randomUUID(),
                    "artwork/$asset",
                    work,
                )
                it.executeBound(
                    "INSERT INTO asset_reference(id,asset_id,title_id,state,catalog_version) VALUES (?,?,?,'ACTIVE',1)",
                    UUID.randomUUID(),
                    asset,
                    UUID.randomUUID(),
                )
            }
            val result = maintenance.daily()
            assertEquals(1, result.getValue("work"))
            assertEquals(2, result.getValue("attemptEvents"))
            assertEquals(1, count(pool, "operation_receipt"))
            assertEquals(1, count(pool, "asset_reference"))
            assertEquals(
                1,
                scalar(pool, "SELECT count(*) FROM asset WHERE processing_work_id IS NULL AND storage_key IS NOT NULL"),
            )
        }

    @Test
    fun `Media waits on asset before work and rechecks changed eligibility without deleting evidence`() =
        fixture { pool, maintenance, _ ->
            val operation = receipt(pool)
            val work = work(pool, "DONE", 100, operation)
            attempts(pool, work, "SUCCEEDED")
            val asset = retainedAsset(pool, work, operation)
            blockedRetention(pool, maintenance, work, asset)
            assertEquals(2, count(pool, "operational_event"))
            assertEquals(
                1,
                scalar(pool, "SELECT count(*) FROM asset WHERE processing_work_id='$work' AND storage_key IS NOT NULL"),
            )
            assertEquals(1, scalar(pool, "SELECT count(*) FROM durable_work WHERE id='$work' AND payload<>'{}'::jsonb"))
        }

    @Test
    fun `maintenance role cannot modify permanent identities or issue DDL`() =
        fixture { pool, maintenance, maintenanceSource ->
            receipt(pool)
            assertThrows(java.sql.SQLException::class.java) {
                maintenanceSource.connection.use {
                    it.createStatement().executeUpdate("DELETE FROM operation_receipt")
                }
            }
            assertThrows(java.sql.SQLException::class.java) {
                maintenanceSource.connection.use {
                    it.createStatement().executeUpdate("CREATE TABLE appdata.not_permitted(id integer)")
                }
            }
            assertEquals(1, maintenance.hourly().getValue("receiptBodies"))
        }

    private fun receipt(
        pool: DataSource,
        state: String = "SUCCEEDED",
    ): UUID =
        UUID.randomUUID().also { id ->
            pool.connection.use {
                it.executeBound(
                    """
                    INSERT INTO operation_receipt(
                      id,actor_scope,kind,request_hash,state,result,correlation_id,expires_at)
                    VALUES (?,'SERVICE:test','TEST',decode(repeat('00',32),'hex'),?,'{"ok":true}'::jsonb,?,
                      CURRENT_TIMESTAMP-INTERVAL '1 day')
                    """.trimIndent(),
                    id,
                    state,
                    UUID.randomUUID(),
                )
            }
        }

    private fun work(
        pool: DataSource,
        state: String,
        days: Int,
        operation: UUID = receipt(pool),
        notification: Boolean = false,
    ): UUID =
        UUID.randomUUID().also { id ->
            pool.connection.use {
                it.executeBound(
                    """
                    INSERT INTO durable_work(id,operation_id,kind,payload,state,max_attempts,deadline_at,correlation_id,
                      base_delay_ms,cap_delay_ms,backoff_multiplier,updated_at,notification_key,notification_mode,
                      notification_state,notification_summary)
                    VALUES (?,?,?,'{"subject":"test"}'::jsonb,?,3,CURRENT_TIMESTAMP,?,100,1000,2,
                      CURRENT_TIMESTAMP-(? * INTERVAL '1 day'),?,?,?,?::jsonb)
                    """.trimIndent(),
                    id,
                    operation,
                    if (notification) "NOTIFICATION" else "TEST",
                    state,
                    UUID.randomUUID(),
                    days,
                    if (notification) id.toString() else null,
                    if (notification) "database" else null,
                    if (notification) "RECORDED" else null,
                    if (notification) "{}" else null,
                )
            }
        }

    private fun attempts(
        pool: DataSource,
        id: UUID,
        outcome: String?,
    ) {
        pool.connection.use { connection ->
            connection.executeBound(
                """
                INSERT INTO operational_event(id,actor_kind,action,subject_kind,correlation_id,created_at,event_type,
                  work_id,generation,attempt_no,lease_epoch)
                VALUES (?,'SERVICE','WORK_ATTEMPT','WORK',?,CURRENT_TIMESTAMP-INTERVAL '100 days',
                  'ATTEMPT_STARTED',?,0,1,1)
                """.trimIndent(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                id,
            )
            if (outcome != null) {
                connection.executeBound(
                    """
                    INSERT INTO operational_event(
                      id,actor_kind,action,subject_kind,correlation_id,created_at,event_type,
                      work_id,generation,attempt_no,lease_epoch,outcome,attempt_outcome)
                    VALUES (?,'SERVICE','WORK_ATTEMPT','WORK',?,CURRENT_TIMESTAMP-INTERVAL '100 days',
                      'ATTEMPT_FINISHED',?,0,1,1,?,?)
                    """.trimIndent(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    id,
                    if (outcome == "SUCCEEDED") "SUCCEEDED" else "FAILED",
                    outcome,
                )
            }
        }
    }

    private fun retentionAccount(fixture: com.spoolin.core.recovery.crypto.CryptoFixture): UUID {
        val account = UUID.randomUUID()
        fixture.accounts.insert(
            account,
            com.spoolin.core.features.account.persistence
                .IdentityRules()
                .identity("Retention test", "retention@example.invalid"),
            com.spoolin.core.features.crypto.Argon2Engine().hash(
                com.spoolin.core.features.crypto.PasswordInput
                    .exactBytes("Retention test password 123!"),
            ),
            com.spoolin.core.features.account.persistence.AccountRole.VIEWER,
        )
        return account
    }

    private fun seedExpiredIdentityRows(
        pool: DataSource,
        account: UUID,
    ) {
        pool.connection.use { connection ->
            connection.executeBound(
                """
                INSERT INTO browser_session(token_hash,id,account_id,auth_epoch,csrf_hash,expires_at,last_seen_at)
                SELECT decode(md5(i::text)||md5(i::text),'hex'),md5(i::text)::uuid,?,0,
                  decode(repeat('01',32),'hex'),CURRENT_TIMESTAMP-INTERVAL '8 days',CURRENT_TIMESTAMP-INTERVAL '8 days'
                FROM generate_series(1,101) i
                """.trimIndent(),
                account,
            )
            connection.executeBound(
                """
                INSERT INTO mcp_credential(id,token_hash,account_id,client_kind,auth_epoch,scope,expires_at)
                SELECT md5(i::text)::uuid,decode(md5(i::text)||md5(i::text),'hex'),?,'CLAUDE',0,
                  'movie:propose movie:approve',CURRENT_TIMESTAMP-INTERVAL '1 day' FROM generate_series(1,101) i
                """.trimIndent(),
                account,
            )
            connection.executeBound(
                """
                INSERT INTO form_draft(
                  id,account_id,target_kind,policy_revision,body_key_id,body_nonce,body_cipher,expires_at)
                SELECT md5(i::text)::uuid,?,'MOVIE','v1','draft-write',
                  substring(decode(md5(i::text),'hex') FROM 1 FOR 12),
                  decode(repeat('00',16),'hex'),CURRENT_TIMESTAMP-INTERVAL '1 day' FROM generate_series(1,101) i
                """.trimIndent(),
                account,
            )
            connection.createStatement().executeUpdate(
                """
                INSERT INTO request_rate_window(bucket_key,window_start,hits,expires_at)
                SELECT decode(md5(i::text)||md5(i::text),'hex'),CURRENT_TIMESTAMP,1,CURRENT_TIMESTAMP-INTERVAL '1 day'
                FROM generate_series(1,101) i
                """.trimIndent(),
            )
            connection.executeBound(
                """
                INSERT INTO browser_session(token_hash,id,account_id,auth_epoch,csrf_hash,expires_at,last_seen_at)
                VALUES (decode(repeat('ff',32),'hex'),?,?,0,decode(repeat('01',32),'hex'),
                  CURRENT_TIMESTAMP-INTERVAL '1 day',CURRENT_TIMESTAMP-INTERVAL '1 day')
                """.trimIndent(),
                UUID.randomUUID(),
                account,
            )
        }
    }

    private fun retainedAsset(
        pool: DataSource,
        work: UUID,
        operation: UUID,
    ): UUID {
        val asset = UUID.randomUUID()
        pool.connection.use {
            it.executeBound(
                """
                INSERT INTO asset(id,upload_operation_id,actor_id,state,input_bytes,policy_revision,storage_key,
                  content_hash,format,output_bytes,width,height,processing_work_id,processing_staging_key,
                  processing_input_hash,processing_created_at)
                VALUES (?,?,?,'READY',3,'v1',?,decode(repeat('01',32),'hex'),'PNG',3,1,1,?,
                  'quarantine/test',decode(repeat('02',32),'hex'),CURRENT_TIMESTAMP-INTERVAL '100 days')
                """.trimIndent(),
                asset,
                operation,
                UUID.randomUUID(),
                "artwork/$asset",
                work,
            )
        }
        return asset
    }

    private fun blockedRetention(
        pool: DataSource,
        maintenance: RetentionMaintenance,
        work: UUID,
        asset: UUID,
    ) {
        val executor =
            java.util.concurrent.Executors
                .newSingleThreadExecutor()
        try {
            pool.connection.use { operator ->
                operator.autoCommit = false
                lockRetentionRow(operator, "SELECT id FROM asset WHERE id=? FOR UPDATE", asset)
                val pending = executor.submit<Map<String, Int>> { maintenance.daily() }
                awaitRetentionWaiter(pool)
                lockRetentionRow(operator, "SELECT id FROM durable_work WHERE id=? FOR UPDATE NOWAIT", work)
                operator.executeBound("UPDATE durable_work SET state='EXHAUSTED' WHERE id=?", work)
                operator.commit()
                assertEquals(0, pending.get(5, java.util.concurrent.TimeUnit.SECONDS).getValue("work"))
            }
        } finally {
            executor.shutdownNow()
        }
    }

    private fun awaitRetentionWaiter(pool: DataSource) {
        val deadline =
            System.nanoTime() +
                java.util.concurrent.TimeUnit.SECONDS
                    .toNanos(1)
        while (
            scalar(pool, "SELECT count(*) FROM pg_locks WHERE NOT granted") == 0 &&
            System.nanoTime() < deadline
        ) {
            Thread.sleep(5)
        }
        assertTrue(scalar(pool, "SELECT count(*) FROM pg_locks WHERE NOT granted") > 0)
    }

    private fun lockRetentionRow(
        connection: java.sql.Connection,
        sql: String,
        id: UUID,
    ) {
        connection.prepareStatement(sql).use { statement ->
            statement.setObject(1, id)
            statement.executeQuery().close()
        }
    }

    private fun count(
        pool: DataSource,
        table: String,
    ): Int = scalar(pool, "SELECT count(*) FROM $table")

    private fun scalar(
        pool: DataSource,
        sql: String,
    ): Int =
        pool.connection.use {
            it.queryBound(sql, emptyList()) { row -> row.getInt(1) }.single()
        }

    private fun fixture(
        owner: String = "media",
        block: (DataSource, RetentionMaintenance, DataSource) -> Unit,
    ) {
        OwnerFixture("retention.json").start().use { fixture ->
            fixture.membership(owner, "grant")
            migrateOwner(fixture, owner)
            fixture.grants(owner)
            fixture.membership(owner, "revoke")
            val maintenance =
                MaintenanceDataSources.open(owner, fixture.url(owner), fixture.passwords.getValue("$owner-maintenance"))
            fixture.pool(owner).use { block(it, RetentionMaintenance(maintenance, owner), maintenance) }
        }
    }
}
