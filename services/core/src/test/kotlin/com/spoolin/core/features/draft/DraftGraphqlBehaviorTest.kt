package com.spoolin.core.features.draft

import com.spoolin.core.boundary.http.BrowserContext
import com.spoolin.core.features.account.application.CredentialRejected
import com.spoolin.core.features.crypto.*
import com.spoolin.core.features.session.SessionJdbcFixture
import com.spoolin.core.features.session.SessionPrincipal
import com.spoolin.core.operations.receipts.OwnerReceipts
import com.spoolin.support.receipts.*
import graphql.GraphQLContext
import graphql.schema.DataFetchingEnvironmentImpl
import graphql.schema.idl.RuntimeWiring
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

class DraftGraphqlBehaviorTest {
    @TempDir lateinit var root: Path
    private val account = UUID.randomUUID()
    private val draft = UUID.randomUUID()
    private val operation = UUID.randomUUID()
    private val now = Instant.now()
    private val material = ByteArray(32) { 97 }
    private val hashes = IntentHashes(mapOf("compare" to ByteArray(32) { 1 }), "compare")
    private val values = "{\"name\":\"Person\"}"
    private val saveIntent = OperationIntent(operation, "account:$account", "DRAFT_SAVE", "revision", listOf("draftId" to draft.toString(), "targetKind" to "PERSON", "targetId" to null, "baseVersion" to null, "operationId" to null, "expectedVersion" to null, "values" to values))

    @Test
    fun `save draft uses real encryption quota receipt and remembered browser operation`() = fixture { h ->
        h.db.enqueue(mapOf("id" to "draft")); h.db.enqueue(mapOf("allocated" to 1L)); h.db.enqueue(receipt(saveIntent, "PENDING", null))
        h.db.enqueue(mapOf("id" to "draft")); h.db.enqueue(mapOf("id" to account)); h.db.enqueue(mapOf("1" to 0L))
        h.db.enqueue(receipt(saveIntent, "SUCCEEDED", SafeResult(draft, 0)))
        val result = h.call("Mutation", "saveDraft", mapOf("input" to input())) as Map<*, *>
        val receipt = result["operation"] as Map<*, *>
        assertEquals("SUCCEEDED", receipt["state"])
        assertEquals(draft.toString(), receipt["resultId"])
        assertEquals("0", receipt["version"])
        assertNotNull(h.context.operation(operation, OperationOwner.CORE))
        assertEquals(2, h.db.commits)
    }

    @Test
    fun `saved draft replay returns durable result without overwriting current draft`() = fixture { h ->
        h.db.enqueue(mapOf("id" to "draft")); h.db.enqueue(mapOf("allocated" to 2L))
        h.db.enqueue(receipt(saveIntent, "SUCCEEDED", SafeResult(draft, 4)))
        val result = h.call("Mutation", "saveDraft", mapOf("input" to input())) as Map<*, *>
        assertEquals("4", (result["operation"] as Map<*, *>)["version"])
        assertEquals(1, h.db.updates)
    }

    @Test
    fun `discard draft deletes owned matching version and projects boolean result`() = fixture { h ->
        val intent = OperationIntent(operation, "account:$account", "DRAFT_DISCARD", "discard", listOf("draftId" to draft.toString(), "expectedVersion" to "4"))
        h.db.enqueue(receipt(intent, "PENDING", null)); h.db.enqueue(mapOf("id" to account)); h.db.enqueue(mapOf("version" to 4L)); h.db.enqueue(receipt(intent, "SUCCEEDED", SafeResult(draft, discarded = true)))
        val result = h.call("Mutation", "discardDraft", mapOf("expectedVersion" to "4")) as Map<*, *>
        assertEquals(true, result["discarded"])
        assertEquals("SUCCEEDED", (result["operation"] as Map<*, *>)["state"])
        assertNotNull(h.context.operation(operation, OperationOwner.CORE))
    }

    @Test
    fun `list draft decrypts form and projects public values without envelope fields`() = fixture { h ->
        val envelope = AeadCipher().encrypt("draft", material, ByteArray(12) { 1 }, ProtectionBinding("core", "form_draft", draft, KeyPurpose.DRAFT), values.toByteArray())
        h.db.enqueue(mapOf("id" to draft, "target_kind" to "PERSON", "target_id" to null, "base_version" to null, "operation_id" to null, "body_key_id" to "draft", "body_nonce" to envelope.nonce(), "body_cipher" to envelope.cipher(), "version" to 3L, "expires_at" to Timestamp.from(now.plusSeconds(3600))))
        h.db.enqueue(mapOf("id" to "draft"))
        val result = (h.call("Query", "drafts", emptyMap()) as List<*>).single() as Map<*, *>
        assertEquals(mapOf("name" to "Person"), result["values"])
        assertEquals("3", result["version"])
        assertFalse(result.keys.any { it.toString().contains("cipher", true) })
    }

    @Test
    fun `anonymous malformed version and failed encryption cannot mutate a draft`() = fixture { h ->
        for ((type, field) in listOf("Query" to "drafts", "Mutation" to "saveDraft", "Mutation" to "discardDraft")) {
            assertThrows(CredentialRejected::class.java) { h.call(type, field, emptyMap(), BrowserContext(null, null, null, null)) }
        }
        assertThrows(IllegalArgumentException::class.java) { h.call("Mutation", "saveDraft", mapOf("expectedVersion" to "-1", "input" to input())) }
        assertThrows(IllegalArgumentException::class.java) { h.call("Mutation", "discardDraft", emptyMap()) }
        h.db.enqueue()
        assertThrows(ProtectionUnavailable::class.java) { h.call("Mutation", "saveDraft", mapOf("input" to input())) }
        assertEquals(0, h.db.updates)
    }

    private fun input() = mapOf("targetKind" to "PERSON", "values" to values, "policyRevision" to "revision")
    private fun receipt(intent: OperationIntent, state: String, result: SafeResult?): Map<String, Any?> = mapOf("id" to operation, "state" to state, "expires_at" to Timestamp.from(now.plusSeconds(3600)), "created_at" to Timestamp.from(now), "result" to result?.json(), "error_code" to null, "correlation_id" to UUID.randomUUID(), "actor_scope" to intent.actorScope, "kind" to intent.kind, "request_hash" to hashes.hash(intent, "compare"), "hash_key_id" to "compare", "receipt_type" to "REQUEST")
    private inner class Harness(val db: SessionJdbcFixture, val wiring: RuntimeWiring) {
        val context = BrowserContext(SessionPrincipal(UUID.randomUUID(), com.spoolin.core.identity.AccountAuthority(account, com.spoolin.core.identity.AccountRole.MAINTAINER, 3), now.plusSeconds(3600), "csrf"), null, null, null)
        fun call(type: String, field: String, args: Map<String, Any?>, browser: BrowserContext = context): Any? = wiring.dataFetchers.getValue(type).getValue(field).get(DataFetchingEnvironmentImpl.newDataFetchingEnvironment().arguments(mapOf("operationId" to operation.toString(), "id" to draft.toString()) + args).graphQLContext(GraphQLContext.newContext().of("browser", browser).build()).build())
    }
    private fun fixture(test: (Harness) -> Unit) {
        val path = root.resolve("key"); Files.write(path, material); Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"))
        val db = SessionJdbcFixture(emptyList()); val payload = DraftPayload()
        MountedKeys(mapOf("draft" to KeyMount(KeyPurpose.DRAFT, path)), null).use { keys ->
            val crypto = DraftCryptography(KeyRegistry(db), keys, NonceAllocator(db), AeadCipher())
            val store = ProtectedDrafts(db, crypto, payload, ProtectionAlert { }); val receipts = OwnerReceipts(db, hashes)
            val wiring = RuntimeWiring.newRuntimeWiring(); DraftGraphql(store, SaveDraft(store, crypto, payload, receipts), DiscardDraft(store, receipts)).bind(wiring)
            test(Harness(db, wiring.build()))
        }
    }
}
