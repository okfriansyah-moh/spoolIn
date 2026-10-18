package com.spoolin.core.features.restore

import com.spoolin.core.DatabaseSettings
import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.KeyRegistry
import com.spoolin.core.features.crypto.MountedKeys
import com.spoolin.core.features.crypto.committed
import com.spoolin.core.features.crypto.executeBound
import com.spoolin.core.features.crypto.queryBound
import com.spoolin.core.operations.crypto.KeyRotationPlan
import com.spoolin.core.operations.crypto.RotationKeys
import com.spoolin.core.requireProtectedFile
import org.springframework.core.env.StandardEnvironment
import tools.jackson.core.StreamReadFeature
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.security.MessageDigest
import java.util.UUID
import javax.sql.DataSource
import kotlin.system.exitProcess

private const val RESTORE_FAILURE_EXIT = 4
private const val MAXIMUM_PLAN_BYTES = 65536L

/** Runs only while all application writers are stopped and the installation restore marker is present. */
object RestoreCheckpointCli {
    private var stage = "protected configuration"

    @JvmStatic
    fun main(args: Array<String>) {
        runCatching { restore(args) }.onFailure(::failed)
    }

    private fun failed(failure: Throwable) {
        if (failure is Error) throw failure
        System.err.println(
            "Restore epoch verification failed during $stage (${failure.javaClass.simpleName}); " +
                "writers must remain stopped and restore marker retained",
        )
        exitProcess(RESTORE_FAILURE_EXIT)
    }

    private fun restore(args: Array<String>) {
        require(args.size == 1 && args[0].startsWith("--plan="))
        val planPath = Path.of(args[0].substringAfter('='))
        requireProtectedFile(planPath, MAXIMUM_PLAN_BYTES)
        val directory = requireNotNull(planPath.parent)
        requireProtectedFile(directory.resolve("restore-pending-validation"), MAXIMUM_PLAN_BYTES)
        val mapper =
            JsonMapper
                .builder()
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .build()
        val plan = mapper.readTree(Files.readAllBytes(planPath))
        val checkpointHash = plan["checkpointSha256"].asString()
        require(checkpointHash.matches(Regex("[0-9a-f]{64}")))
        UUID.fromString(plan["epoch"].asString())
        val marker = mapper.readTree(Files.readAllBytes(directory.resolve("restore-pending-validation")))
        require(
            marker["checkpointSha256"].asString() == plan["checkpointSha256"].asString() &&
                marker["epoch"].asString() == plan["epoch"].asString(),
        ) { "Restore plan and retained marker differ" }
        DatabaseSettings.load(StandardEnvironment(), "core", false).pool().use { source ->
            RotationKeys.load(Path.of(plan["keys"].asString())).use { keys ->
                stage = "restored credential revocation"
                revokeRestoredCredentials(source, plan)
                rotate(source, keys, plan)
                stage = "old nonce counter comparison"
                verifyCounters(source, plan)
                verifyRevocation(source)
                stage = "complete protected row and lookup authentication"
                val verified = RestoreVerification.verify(source, keys)
                stage = "protected result persistence"
                val report =
                    mapOf(
                        "checkpointSha256" to plan["checkpointSha256"].asString(),
                        "epoch" to plan["epoch"].asString(),
                        "freshWriteKeys" to true,
                        "oldNonceCountersUnchanged" to true,
                        "credentialsRevoked" to true,
                        "authenticatedRows" to verified,
                    )
                val target = directory.resolve("restore-crypto-result.json")
                Files.write(target, mapper.writeValueAsBytes(report))
                Files.setPosixFilePermissions(target, PosixFilePermissions.fromString("rw-------"))
                println("Restore key epochs, complete protected row authentication and credential revocation verified")
            }
        }
    }

    private fun rotate(
        source: DataSource,
        keys: MountedKeys,
        plan: JsonNode,
    ) {
        val registry = KeyRegistry(source)
        for (purpose in listOf(KeyPurpose.PII, KeyPurpose.DRAFT, KeyPurpose.EMAIL_LOOKUP)) {
            stage = "${purpose.name} write epoch and backfill"
            val from = plan["originals"][purpose.name].asString()
            val to = plan["targets"][purpose.name].asString()
            require(from != to && plan["originalCounters"].get(to) == null) {
                "Restore target must be a new key version"
            }
            RotationKeys.registerTarget(source, keys, to, purpose)
            val result = KeyRotationPlan(from, to, purpose).rotate(source, registry, false, keys)
            check(result.ok && result.remaining == 0L) { "Restore backfill incomplete" }
            check(registry.writeKey(purpose).id == to)
        }
    }

    private fun verifyCounters(
        source: DataSource,
        plan: JsonNode,
    ) {
        val actual =
            source.connection.use { connection ->
                connection
                    .queryBound(
                        "SELECT id,next_nonce FROM appdata.crypto_key_version ORDER BY id",
                    ) { it.getString("id") to it.getLong("next_nonce").toString() }
                    .toMap()
            }
        plan["originalCounters"].properties().forEach { entry ->
            check(actual[entry.key] == entry.value.asString()) {
                "A restored key nonce counter was used for encryption"
            }
        }
    }

    private fun revokeRestoredCredentials(
        source: DataSource,
        plan: JsonNode,
    ) {
        val identity = "restore-revoke:" + plan["checkpointSha256"].asString() + ":" + plan["epoch"].asString()
        val id = UUID.nameUUIDFromBytes(identity.toByteArray(Charsets.UTF_8))
        val hash = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray(Charsets.UTF_8))
        source.committed { connection ->
            connection.queryBound("SELECT pg_advisory_xact_lock(734850221)") { true }
            val existing =
                connection
                    .queryBound("SELECT request_hash,state FROM appdata.operation_receipt WHERE id=?", listOf(id)) {
                        it.getBytes("request_hash") to it.getString("state")
                    }.singleOrNull()
            if (existing != null) {
                check(MessageDigest.isEqual(hash, existing.first) && existing.second == "SUCCEEDED")
                revokeSessions(connection)
                return@committed
            }
            connection.executeBound("UPDATE appdata.user_account SET auth_epoch=auth_epoch+1")
            revokeSessions(connection)
            connection.executeBound(
                "INSERT INTO appdata.operation_receipt " +
                    "(id,actor_scope,kind,request_hash,state,result,correlation_id,expires_at) " +
                    "VALUES (?,'LOCAL_OPERATOR','RESTORE_REVOKE',?,'SUCCEEDED'," +
                    "'{\"revoked\":true}'::jsonb,?,clock_timestamp()+INTERVAL '7 days')",
                id,
                hash,
                id,
            )
            connection.executeBound(
                "INSERT INTO appdata.operational_event " +
                    "(id,operation_id,actor_kind,action,subject_kind,outcome,correlation_id,event_type) " +
                    "VALUES (?,?,'SERVICE','RESTORE_REVOKE','INSTALLATION','SUCCEEDED',?,'ACTIVITY')",
                UUID.randomUUID(),
                id,
                id,
            )
        }
    }

    private fun revokeSessions(connection: java.sql.Connection) {
        connection.executeBound("UPDATE appdata.browser_session SET revoked_at=COALESCE(revoked_at,clock_timestamp())")
        connection.executeBound("UPDATE appdata.mcp_credential SET revoked_at=COALESCE(revoked_at,clock_timestamp())")
    }

    private fun verifyRevocation(source: DataSource) {
        source.connection.use { connection ->
            for (table in listOf("browser_session", "mcp_credential")) {
                val active =
                    connection
                        .queryBound(
                            "SELECT count(*) FROM appdata.$table WHERE revoked_at IS NULL",
                        ) { it.getLong(1) }
                        .single()
                check(active == 0L) { "Restored authentication material remains live" }
            }
        }
    }
}
