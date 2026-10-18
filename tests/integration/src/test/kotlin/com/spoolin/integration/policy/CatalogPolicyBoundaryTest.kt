package com.spoolin.integration.policy

import com.google.protobuf.FieldMask
import com.spoolin.catalog.boundary.grpc.CatalogActors
import com.spoolin.catalog.boundary.policy.CatalogWritePolicy
import com.spoolin.catalog.features.credit.CreditWrites
import com.spoolin.catalog.features.movie.MovieWrites
import com.spoolin.catalog.features.person.write.PersonWrites
import com.spoolin.catalog.features.taxonomy.roles.RoleWrites
import com.spoolin.catalog.operations.receipts.OwnerReceipts
import com.spoolin.integration.identity.ScopedActorTest
import com.spoolin.integration.identity.SignedReceiptFixture
import com.spoolin.support.identity.ActorInterceptor
import com.spoolin.support.identity.ActorRpc
import com.spoolin.support.identity.ActorVerifier
import com.spoolin.support.policy.ConfigurationSources
import com.spoolin.support.policy.PolicySettings
import com.spoolin.support.policy.PublicPolicy
import com.spoolin.support.policy.InputViolation
import com.spoolin.support.receipts.IntentHashes
import com.spoolin.support.receipts.OperationIntent
import com.spoolin.support.receipts.SafeResult
import com.spoolin.support.receipts.committed
import com.spoolin.support.receipts.executeBound
import com.spoolin.support.receipts.queryBound
import io.grpc.Metadata
import io.grpc.StatusRuntimeException
import io.grpc.stub.MetadataUtils
import io.grpc.stub.StreamObserver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.IdentityServiceGrpc
import spoolin.v1.Spoolin
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.Executors

/** Calls actual write handlers through signed mTLS and live Core identity, then reads separately committed PostgreSQL rows. */
class CatalogPolicyBoundaryTest {
    @Test
    fun `effective person policy rejects fresh invalid fields while replay and unchanged legacy text survive tightening`() {
        CatalogPolicyFixture().use { fixture ->
            fixture.policy = configured(mapOf("PERSON_NAME_MIN_CODE_POINTS" to "2", "PERSON_NAME_MAX_CODE_POINTS" to "3"))
            val invalid = fixture.person("x")
            assertEquals("FIELD_INVALID", fixture.person(invalid).problem.code)
            assertEquals(0, fixture.count("SELECT count(*) FROM person"))
            val original = fixture.person(" Zo\u0308 ")
            val created = fixture.person(original)
            assertEquals("SUCCEEDED", created.state)
            fixture.policy = configured(mapOf("PERSON_NAME_MIN_CODE_POINTS" to "4", "PERSON_NAME_MAX_CODE_POINTS" to "5",
                "BIOGRAPHY_MIN_CODE_POINTS" to "3", "BIOGRAPHY_MAX_CODE_POINTS" to "3"))
            assertEquals(created, fixture.person(original))
            val stale = fixture.person("\uD800").toBuilder().setContext(fixture.context(original.context.policyRevision)).build()
            assertEquals("POLICY_CHANGED", assertThrows(StatusRuntimeException::class.java) { fixture.person(stale) }.status.description)
            val patch = fixture.person("x").toBuilder().setId(created.resultId).setExpectedVersion(0)
                .setUpdateMask(FieldMask.newBuilder().addPaths("values.biography")).build()
            assertEquals(1, fixture.person(patch).version)
            assertEquals("Zo\u0308", fixture.text("SELECT name FROM person WHERE id=?", created.resultId))
            assertEquals(2, fixture.count("SELECT count(*) FROM catalog_audit_event WHERE subject_kind='PERSON'"))
            assertEquals(0, fixture.count("SELECT count(*) FROM operation_receipt WHERE state='PENDING'"))
        }
    }

    @Test
    fun `effective movie policy preserves unchanged title and rejects changed values without partial commit`() {
        CatalogPolicyFixture().use { fixture ->
            fixture.policy = configured(mapOf("TITLE_MIN_CODE_POINTS" to "2", "TITLE_MAX_CODE_POINTS" to "4"))
            val original = fixture.movie("Long")
            val created = fixture.movie(original)
            assertEquals("SUCCEEDED", created.state)
            fixture.policy = configured(mapOf("TITLE_MIN_CODE_POINTS" to "2", "TITLE_MAX_CODE_POINTS" to "2"))
            assertEquals(created, fixture.movie(original))
            val patch = fixture.movie("x").toBuilder().setId(created.resultId).setExpectedVersion(0)
                .setUpdateMask(FieldMask.newBuilder().addPaths("values.synopsis")).build()
            assertEquals(1, fixture.movie(patch).version)
            assertEquals("Long", fixture.text("SELECT display_title FROM title WHERE id=?", created.resultId))
            val invalid = fixture.movie("x").toBuilder().setId(created.resultId).setExpectedVersion(1)
                .setUpdateMask(FieldMask.newBuilder().addPaths("values.title")).build()
            assertEquals("FIELD_INVALID", fixture.movie(invalid).problem.code)
            assertEquals("Long", fixture.text("SELECT display_title FROM title WHERE id=?", created.resultId))
            assertEquals(2, fixture.count("SELECT count(*) FROM catalog_audit_event WHERE subject_kind='TITLE'"))
        }
    }

    @Test
    fun `effective roles and ordered characters use owner policy through real writes and quoted audit stays valid`() {
        CatalogPolicyFixture().use { fixture ->
            fixture.policy = configured(mapOf("CREDIT_ROLE_MIN_CODE_POINTS" to "2", "CREDIT_ROLE_MAX_CODE_POINTS" to "3",
                "CHARACTER_NAME_MIN_CODE_POINTS" to "2", "CHARACTER_NAME_MAX_CODE_POINTS" to "3"))
            assertEquals("FIELD_INVALID", assertThrows(StatusRuntimeException::class.java) { fixture.role("x") }.status.description)
            val custom = fixture.role("a\"b")
            assertEquals("SUCCEEDED", custom.state)
            assertEquals("a\"b", fixture.text("SELECT label FROM credit_role WHERE id=?", custom.resultId))
            val title = fixture.movie(fixture.movie("Movie"))
            val person = fixture.person(fixture.person("Person"))
            val role = fixture.characterRole()
            val invalid = fixture.credit(title.resultId, person.resultId, role, "x")
            assertEquals("FIELD_INVALID", fixture.credit(invalid).problem.code)
            assertEquals(0, fixture.count("SELECT count(*) FROM credit"))
            val created = fixture.credit(fixture.credit(title.resultId, person.resultId, role, "e\u0301"))
            assertEquals("SUCCEEDED", created.state)
            assertEquals("e\u0301", fixture.text("SELECT characters[1] FROM credit WHERE id=?", created.resultId))
            assertEquals(1, fixture.count("SELECT count(*) FROM credit"))
        }
    }

    @Test
    fun `frozen proposal publisher uses the same effective owner validator inside its transaction`() {
        CatalogPolicyFixture().use { fixture ->
            fixture.policy = configured(mapOf("TITLE_MIN_CODE_POINTS" to "2", "TITLE_MAX_CODE_POINTS" to "2"))
            assertThrows(InputViolation::class.java) { fixture.publish("Long") }
            assertEquals(0, fixture.count("SELECT count(*) FROM title"))
            assertEquals(0, fixture.count("SELECT count(*) FROM catalog_audit_event"))
            val result = fixture.publish("Ok")
            assertEquals("Ok", fixture.text("SELECT display_title FROM title WHERE id=?", result.resultId.toString()))
            assertEquals(1, fixture.count("SELECT count(*) FROM catalog_audit_event"))
        }
    }

    @Test
    fun `fresh request rechecks policy after entering Catalog transaction lock`() {
        CatalogPolicyFixture().use { fixture ->
            val request = fixture.person("Existing")
            val executor = Executors.newSingleThreadExecutor()
            val gate = fixture.gate()
            try {
                val response = executor.submit<StatusRuntimeException> {
                    assertThrows(StatusRuntimeException::class.java) { fixture.person(request) }
                }
                val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3)
                while (!fixture.waiting() && System.nanoTime() < deadline) Thread.sleep(10)
                assertTrue(fixture.waiting())
                fixture.policy = configured(mapOf("PERSON_NAME_MAX_CODE_POINTS" to "2"))
                gate.commit()
                assertEquals("POLICY_CHANGED", response.get(10, TimeUnit.SECONDS).status.description)
                assertEquals(0, fixture.count("SELECT count(*) FROM person"))
                assertEquals(0, fixture.count("SELECT count(*) FROM operation_receipt WHERE state='PENDING'"))
            } finally {
                gate.close()
                executor.shutdownNow()
            }
        }
    }

    companion object {
        private fun configured(values: Map<String, String>): PublicPolicy = PolicySettings.resolve(ConfigurationSources(emptyMap(), values))
    }
}

private class CatalogPolicyFixture : AutoCloseable {
    private val proof = SignedReceiptFixture("catalog-effective-policy-boundary.json")
    private val pool = proof.fixture.owner.pool("catalog")
    @Volatile var policy: PublicPolicy = PolicySettings.resolve(ConfigurationSources(emptyMap(), emptyMap()))
    private val provider = CatalogWritePolicy { policy }
    private val receipts = OwnerReceipts(pool, IntentHashes(mapOf("v1" to ByteArray(32) { 42 }), "v1"))
    private val movies = MovieWrites(receipts, policy = provider)
    private val people = PersonWrites(receipts, policy = provider)
    private val credits = CreditWrites(receipts, policy = provider)
    private val roles = RoleWrites(receipts, policy = provider)
    private val service = object : CatalogServiceGrpc.CatalogServiceImplBase() {
        override fun writeMovie(request: Spoolin.MovieWrite, response: StreamObserver<Spoolin.Outcome>) =
            movies.writeMovie(request, response)
        override fun writePerson(request: Spoolin.PersonWrite, response: StreamObserver<Spoolin.Outcome>) =
            people.writePerson(request, response)
        override fun writeCredit(request: Spoolin.CreditWrite, response: StreamObserver<Spoolin.Outcome>) =
            credits.writeCredit(request, response)
        override fun createRole(request: Spoolin.RoleWrite, response: StreamObserver<Spoolin.Outcome>) = roles.createRole(request, response)
    }
    private val transport = ScopedActorTest()
    private val live = CatalogActors(IdentityServiceGrpc.newBlockingStub(proof.identity))
    private val actors = ActorInterceptor("catalog", CatalogActors.protectedMethods,
        ActorVerifier(mapOf("key_A" to proof.pair.public)), live::check)
    private val server = transport.server(proof.root, "catalog", service, com.spoolin.catalog.ServiceIdentityInterceptor(actors))
    private val channel = transport.channel(proof.root, "core", "catalog", server.port)
    private val genre = UUID.randomUUID().also { id -> pool.committed {
        it.executeBound("INSERT INTO genre(id,label,normalized_label) VALUES (?,'Policy proof','policy proof')", id)
    } }

    fun context(revision: String = policy.revision): Spoolin.Context = Spoolin.Context.newBuilder()
        .setOperationId(UUID.randomUUID().toString()).setCorrelationId(UUID.randomUUID().toString()).setPolicyRevision(revision).build()

    fun person(name: String): Spoolin.PersonWrite = Spoolin.PersonWrite.newBuilder().setContext(context())
        .setValues(Spoolin.PersonValues.newBuilder().setName(name)).build()
    fun person(request: Spoolin.PersonWrite): Spoolin.Outcome = client("WritePerson", request.context).writePerson(request)
    fun movie(title: String): Spoolin.MovieWrite = Spoolin.MovieWrite.newBuilder().setContext(context())
        .setValues(Spoolin.MovieValues.newBuilder().setTitle(title).setSynopsis("Synopsis").setSynopsisLanguage("en")
            .addGenreIds(genre.toString()).setRelease(Spoolin.PartialDate.newBuilder().setYear(2024))).build()
    fun movie(request: Spoolin.MovieWrite): Spoolin.Outcome = client("WriteMovie", request.context).writeMovie(request)
    fun role(label: String): Spoolin.Outcome {
        val request = Spoolin.RoleWrite.newBuilder().setContext(context()).setCategory("CAST").setLabel(label).build()
        return client("CreateRole", request.context).createRole(request)
    }
    fun characterRole(): String = UUID.randomUUID().also { role -> pool.committed {
        it.executeBound("INSERT INTO credit_role(id,category,label,normalized_label,allows_characters) " +
            "VALUES (?,'CAST','Policy actor','policy actor',true)", role)
    } }.toString()
    fun credit(title: String, person: String, role: String, character: String): Spoolin.CreditWrite =
        Spoolin.CreditWrite.newBuilder().setContext(context()).setTitleId(title).setPersonId(person).setRoleId(role)
            .setExpectedVersion(0).addCharacters(character).build()
    fun credit(request: Spoolin.CreditWrite): Spoolin.Outcome = client("WriteCredit", request.context).writeCredit(request)
    fun count(sql: String): Int = pool.connection.use { it.queryBound(sql, emptyList()) { row -> row.getInt(1) }.single() }
    fun text(sql: String, id: String): String = pool.connection.use {
        it.queryBound(sql, listOf(UUID.fromString(id))) { row -> row.getString(1) }.single()
    }

    // This calls the actual post-approval owner publisher; it does not claim MCP host or approval transport proof.
    fun publish(title: String): SafeResult {
        val request = movie(title)
        val operation = UUID.fromString(request.context.operationId)
        val correlation = UUID.fromString(request.context.correlationId)
        val intent = OperationIntent(operation, "account:${proof.account}", "ProposalPublishPolicyProof", policy.revision,
            listOf("title" to title))
        return requireNotNull(receipts.execute(intent, correlation, true) {
            movies.publishFromProposal(it, request.values, proof.account, operation, correlation)
        }.result)
    }

    fun gate(): java.sql.Connection = pool.connection.also {
        it.autoCommit = false
        it.queryBound("SELECT pg_advisory_xact_lock(41001,1)", emptyList()) { 1 }
    }
    fun waiting(): Boolean = count("SELECT count(*) FROM pg_locks WHERE locktype='advisory' AND classid=41001 " +
        "AND objid=1 AND NOT granted") > 0

    private fun client(method: String, context: Spoolin.Context): CatalogServiceGrpc.CatalogServiceBlockingStub {
        val now = Instant.now().epochSecond
        val claims = proof.claims.copy(method = "/spoolin.v1.CatalogService/$method", operationId = UUID.fromString(context.operationId),
            correlationId = UUID.fromString(context.correlationId), issuedAt = now, expiresAt = now + 30)
        val headers = Metadata().apply { put(ActorRpc.metadata, proof.signer.sign(claims)) }
        return CatalogServiceGrpc.newBlockingStub(channel).withDeadlineAfter(10, TimeUnit.SECONDS)
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))
    }

    override fun close() {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
        server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
        pool.close()
        proof.close()
    }
}
