package com.spoolin.catalog.unit

import com.spoolin.catalog.CatalogWriteConfiguration
import com.spoolin.catalog.OwnerServiceConfiguration
import com.spoolin.catalog.RuntimeConfiguration
import com.spoolin.catalog.boundary.grpc.CatalogServiceRpc
import com.spoolin.catalog.features.lifecycle.ImpactSnapshot
import io.grpc.Status
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.support.StaticListableBeanFactory
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import spoolin.v1.Spoolin
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions

class OwnerWiringRulesTest {
    @TempDir lateinit var directory: Path
    private fun env() = StandardEnvironment()
    private fun factory() = StaticListableBeanFactory()

    @Test fun `read only service wiring preserves public reads and leaves optional writes unavailable`() {
        val db = JdbcRows(emptyList())
        val beans = factory()
        val config = OwnerServiceConfiguration()
        val rpc = config.catalogService(db.source, config.catalogCursorKey(), env(),
            beans.getBeanProvider(com.spoolin.catalog.features.movie.MovieWrites::class.java),
            beans.getBeanProvider(com.spoolin.catalog.features.movie.ManagedMovieReads::class.java),
            beans.getBeanProvider(com.spoolin.catalog.features.person.write.PersonWrites::class.java),
            beans.getBeanProvider(com.spoolin.catalog.features.person.read.ManagedPersonReads::class.java),
            beans.getBeanProvider(com.spoolin.catalog.features.taxonomy.roles.RoleWrites::class.java),
            beans.getBeanProvider(com.spoolin.catalog.features.credit.CreditWrites::class.java),
            beans.getBeanProvider(com.spoolin.catalog.features.credit.ManagedCreditReads::class.java),
            beans.getBeanProvider(com.spoolin.catalog.features.artwork.ArtworkRpc::class.java),
            beans.getBeanProvider(com.spoolin.catalog.features.lifecycle.MovieLifecycle::class.java),
            beans.getBeanProvider(com.spoolin.catalog.features.lifecycle.PersonLifecycle::class.java),
            beans.getBeanProvider(com.spoolin.catalog.features.lifecycle.DeletionImpact::class.java),
            beans.getBeanProvider(com.spoolin.catalog.features.proposal.ProposalWrites::class.java)) as CatalogServiceRpc
        val missing = RpcResult<Spoolin.Person>()
        rpc.getPerson(Spoolin.Id.newBuilder().setId(unitActor.toString()).build(), missing)
        assertEquals("", missing.value().id)
        val unavailable = RpcResult<Spoolin.Outcome>()
        rpc.writeMovie(Spoolin.MovieWrite.getDefaultInstance(), unavailable)
        assertEquals(Status.Code.UNAVAILABLE, Status.fromThrowable(unavailable.failure).code)
        val denied = RpcResult<Spoolin.Outcome>()
        rpc.getOperation(Spoolin.Id.newBuilder().setId(unitOperation.toString()).build(), denied)
        assertEquals(Status.Code.PERMISSION_DENIED, Status.fromThrowable(denied.failure).code)
    }

    @Test fun `configured writers reject missing identity while configured reads share cursor and policy wiring`() {
        val db = JdbcRows()
        val config = CatalogWriteConfiguration()
        val policy = config.catalogWritePolicy(env())
        val receipts = ownerReceipts(db)
        val outcomes = listOf<(RpcResult<Spoolin.Outcome>) -> Unit>(
            { config.movieWrites(receipts, policy).writeMovie(Spoolin.MovieWrite.getDefaultInstance(), it) },
            { config.personWrites(receipts, policy).writePerson(Spoolin.PersonWrite.getDefaultInstance(), it) },
            { config.creditWrites(receipts, policy).writeCredit(Spoolin.CreditWrite.getDefaultInstance(), it) },
            { config.roleWrites(receipts, policy).createRole(Spoolin.RoleWrite.getDefaultInstance(), it) },
            { config.artworkRpc(db.source, receipts, env()).beginArtwork(Spoolin.Hold.getDefaultInstance(), it) },
            { config.movieLifecycle(receipts, env()).apply(Spoolin.Lifecycle.getDefaultInstance(), it) },
        )
        for (run in outcomes) {
            val result = RpcResult<Spoolin.Outcome>(); run(result)
            assertEquals(Status.Code.PERMISSION_DENIED, Status.fromThrowable(result.failure).code)
        }
        withMaintainer {
            val key = ByteArray(32) { 1 }
            val person = RpcResult<Spoolin.Person>()
            config.managedPersonReads(db.source, key, env()).managedPerson(Spoolin.Id.newBuilder().setId("bad").build(), person)
            assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(person.failure).code)
            val title = RpcResult<Spoolin.TitlePage>()
            config.managedMovieReads(db.source, key, env()).managedTitles(Spoolin.PageRequest.getDefaultInstance(), title)
            assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(title.failure).code)
            val credit = RpcResult<Spoolin.CreditPage>()
            config.managedCreditReads(db.source, key, env()).managedCredits(Spoolin.PageRequest.getDefaultInstance(), credit)
            assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(credit.failure).code)
        }
    }

    @Test fun `configured protected key loads receipt and impact signers with same selector`() {
        val key = directory.resolve("key")
        Files.write(key, ByteArray(32) { 1 })
        Files.setPosixFilePermissions(key, PosixFilePermissions.fromString("rw-------"))
        val environment = env().also { it.propertySources.addFirst(MapPropertySource("unit", mapOf(
            "CATALOG_HMAC_KEY_ID" to "test", "CATALOG_HMAC_KEY_FILE" to key.toString()))) }
        val config = CatalogWriteConfiguration()
        val source = JdbcRows(emptyList()).source
        val receipts = config.catalogOwnerReceipts(source, environment)
        assertNull(receipts.find(unitOperation, "account:$unitActor"))
        val tokens = config.impactToken(environment)
        val snapshot = ImpactSnapshot.fromCredits(unitActor, 7, 3, "ARCHIVED", emptyList())
        val impact = config.deletionImpact(source, tokens).project(snapshot)
        assertTrue(impact.token.startsWith("test."))
        val denied = RpcResult<Spoolin.Outcome>()
        config.personLifecycle(source, receipts, tokens, environment).apply(Spoolin.Lifecycle.getDefaultInstance(), denied)
        assertEquals(Status.Code.PERMISSION_DENIED, Status.fromThrowable(denied.failure).code)
        assertNull(config.proposalMetrics(factory().getBeanProvider(io.micrometer.core.instrument.MeterRegistry::class.java), source))
    }

    @Test fun `runtime health starts nonserving and rejects missing schema before startup`() {
        val config = RuntimeConfiguration()
        val environment = env().also { it.propertySources.addFirst(MapPropertySource("unit", mapOf(
            "spoolin.schema.minimum" to "1", "spoolin.schema.maximum" to "2"))) }
        val schema = config.schema(environment, JdbcRows().source)
        val manager = config.healthStatus()
        assertSame(manager.healthService, config.standardHealth(manager))
        val lifecycle = config.readinessLifecycle(schema, manager)
        lifecycle.refresh()
        assertThrows(IllegalStateException::class.java) { lifecycle.run(org.springframework.boot.DefaultApplicationArguments()) }
        lifecycle.stop()
    }
}
