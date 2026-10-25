package com.spoolin.core.features.catalogview

import com.spoolin.core.boundary.grpc.ActorIssuer
import com.spoolin.core.boundary.http.BrowserContext
import com.spoolin.core.features.account.application.CredentialRejected
import com.spoolin.core.features.catalogview.credit.ManagedCreditGraphql
import com.spoolin.core.features.catalogview.movie.ManagedCatalogGraphql
import com.spoolin.core.features.catalogview.person.ManagedPersonGraphql
import com.spoolin.core.features.session.SessionPrincipal
import com.spoolin.core.identity.AccountAuthority
import com.spoolin.core.identity.AccountRole
import com.spoolin.support.identity.ActorSigner
import com.spoolin.support.policy.InputViolation
import com.spoolin.support.policy.PolicyEnvironment
import com.spoolin.support.receipts.OperationOwner
import graphql.GraphQLContext
import graphql.schema.DataFetchingEnvironmentImpl
import graphql.schema.idl.RuntimeWiring
import io.grpc.netty.NettyChannelBuilder
import io.grpc.netty.NettyServerBuilder
import io.grpc.stub.StreamObserver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.Spoolin
import java.net.InetSocketAddress
import java.security.KeyPairGenerator
import java.time.Instant
import java.util.UUID

/** Exercises the registered resolvers with a real local transport and an external catalog fixture. */
class ManagedGraphqlBehaviorTest {
    private val id = "aaaaaaaa-0000-0000-0000-000000000001"
    private val operation = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001")
    private val policy = PolicyEnvironment.resolve { null }

    @Test
    fun `person create and patch preserve unicode explicit clearing version and remembered receipts`() =
        fixture { f ->
            val created = f.call("Mutation", "createPerson", mapOf("input" to mapOf("name" to "山田", "biography" to "Writer")))
            assertEquals(
                "山田",
                f.service.personWrite!!
                    .values.name,
            )
            assertEquals(
                "Writer",
                f.service.personWrite!!
                    .values.biography,
            )
            assertEquals(
                operation.toString(),
                f.service.personWrite!!
                    .context.operationId,
            )
            assertEquals(
                "revision",
                f.service.personWrite!!
                    .context.policyRevision,
            )
            assertEquals("SUCCEEDED", (created as Map<*, *>)["operation"].let { it as Map<*, *> }["state"])
            assertEquals("SUCCEEDED", f.context.operation(operation, OperationOwner.CATALOG)!!["state"])
            f.call(
                "Mutation",
                "updatePerson",
                mapOf(
                    "id" to id,
                    "expectedVersion" to "9007199254740993",
                    "patch" to mapOf("name" to "New", "biography" to null),
                ),
            )
            val patch = f.service.personWrite!!
            assertEquals(9007199254740993L, patch.expectedVersion)
            assertEquals(listOf("values.name", "values.biography"), patch.updateMask.pathsList)
            assertEquals("", patch.values.biography)
            assertEquals(id, patch.id)
        }

    @Test
    fun `managed people preserve identity optional biography pagination and absence`() =
        fixture { f ->
            val person = f.call("Query", "managedPerson", mapOf("id" to id)) as Map<*, *>
            assertEquals(id, person["id"])
            assertEquals("Person", person["name"])
            assertNull(person["biography"])
            assertEquals("7", person["version"])
            val page = f.call("Query", "managedPeople", mapOf("state" to "ARCHIVED", "first" to 2, "after" to "cursor")) as Map<*, *>
            assertEquals("ARCHIVED", f.service.page!!.state)
            assertEquals(2, f.service.page!!.first)
            assertEquals("cursor", f.service.page!!.after)
            assertEquals(mapOf("endCursor" to "next", "hasNextPage" to true), page["pageInfo"])
            f.service.absent = true
            assertNull(f.call("Query", "managedPerson", mapOf("id" to id)))
        }

    @Test
    fun `movie writes carry complete values and selective patches without inventing omitted fields`() =
        fixture { f ->
            f.call(
                "Mutation",
                "createMovie",
                mapOf(
                    "input" to
                        mapOf(
                            "title" to "Film",
                            "originalTitle" to "Original",
                            "synopsis" to "Story",
                            "synopsisLanguage" to "en",
                            "originalLanguage" to "ja",
                            "countries" to listOf("JP"),
                            "genreIds" to listOf("drama"),
                            "release" to mapOf("year" to 2024, "month" to 10, "day" to 9),
                            "runtimeMinutes" to 95,
                        ),
                ),
            )
            assertEquals(
                "Film",
                f.service.movieWrite!!
                    .values.title,
            )
            assertEquals(
                "Story",
                f.service.movieWrite!!
                    .values.synopsis,
            )
            assertEquals(
                95,
                f.service.movieWrite!!
                    .values.runtimeMinutes,
            )
            f.call(
                "Mutation",
                "updateMovie",
                mapOf(
                    "id" to id,
                    "expectedVersion" to "4",
                    "patch" to
                        mapOf(
                            "title" to "Revised",
                            "originalTitle" to null,
                            "countries" to emptyList<String>(),
                            "release" to mapOf("year" to 2000),
                        ),
                ),
            )
            val patch = f.service.movieWrite!!
            assertEquals(4L, patch.expectedVersion)
            assertEquals("Revised", patch.values.title)
            assertEquals("", patch.values.originalTitle)
            assertFalse(patch.updateMask.pathsList.contains("values.synopsis"))
            assertTrue(patch.updateMask.pathsList.contains("values.original_title"))
        }

    @Test
    fun `managed title exposes dates genres provenance and internal artwork selection safely`() =
        fixture { f ->
            val title = f.call("Query", "managedTitle", mapOf("id" to id)) as Map<*, *>
            assertEquals("Film", title["title"])
            assertEquals("MONTH", (title["release"] as Map<*, *>)["precision"])
            assertEquals(listOf(mapOf("id" to "drama")), title["genres"])
            assertEquals("asset", title["_artworkAssetId"])
            assertNull(title["originalTitle"])
            val page = f.call("Query", "managedTitles", mapOf("state" to "ACTIVE")) as Map<*, *>
            assertEquals(1, (page["nodes"] as List<*>).size)
            assertEquals("ACTIVE", f.service.page!!.state)
            f.service.absent = true
            assertNull(f.call("Query", "managedTitle", mapOf("id" to id)))
        }

    @Test
    fun `credit writes preserve selector character list update masks and removal version`() =
        fixture { f ->
            f.call("Mutation", "createRole", mapOf("category" to "CAST", "label" to "Actor"))
            assertEquals("CAST", f.service.roleWrite!!.category)
            assertEquals("Actor", f.service.roleWrite!!.label)
            f.call(
                "Mutation",
                "addCredit",
                mapOf(
                    "titleId" to id,
                    "expectedVersion" to "3",
                    "input" to mapOf("personId" to id, "roleId" to "actor", "characters" to listOf("Lead", "Twin")),
                ),
            )
            assertEquals(listOf("Lead", "Twin"), f.service.creditWrite!!.charactersList)
            assertEquals(id, f.service.creditWrite!!.titleId)
            f.call(
                "Mutation",
                "updateCredit",
                mapOf(
                    "id" to id,
                    "expectedVersion" to "4",
                    "patch" to mapOf("roleId" to null, "characters" to emptyList<String>()),
                ),
            )
            assertEquals(
                listOf("roleId", "characters"),
                f.service.creditWrite!!
                    .updateMask.pathsList,
            )
            assertEquals("", f.service.creditWrite!!.roleId)
            f.call("Mutation", "removeCredit", mapOf("id" to id, "expectedVersion" to "5"))
            assertEquals(id, f.service.removal!!.id)
            assertEquals(5L, f.service.removal!!.expectedVersion)
            assertEquals(
                "",
                f.service.removal!!
                    .context.policyRevision,
            )
        }

    @Test
    fun `managed credit pages project role and support either exclusive selector`() =
        fixture { f ->
            for ((key, kind) in listOf("titleId" to "TITLE", "personId" to "PERSON")) {
                val page = f.call("Query", "managedCredits", mapOf(key to id, "first" to 1, "after" to "cursor")) as Map<*, *>
                assertEquals(kind, f.service.page!!.targetKind)
                assertEquals(id, f.service.page!!.targetId)
                val credit = (page["nodes"] as List<*>).single() as Map<*, *>
                assertEquals("Person", credit["personName"])
                assertEquals(listOf("Lead"), credit["characters"])
                assertEquals(true, (credit["role"] as Map<*, *>)["allowsCharacters"])
            }
            assertThrows(InputViolation::class.java) { f.call("Query", "managedCredits", emptyMap()) }
            assertThrows(InputViolation::class.java) { f.call("Query", "managedCredits", mapOf("titleId" to id, "personId" to id)) }
        }

    @Test
    fun `mutation rejection exposes safe problem fields and never backend message`() =
        fixture { f ->
            f.service.rejected = true
            for (field in listOf("createPerson", "createMovie", "createRole")) {
                val result =
                    f.call(
                        "Mutation",
                        field,
                        mapOf(
                            "input" to mapOf("name" to "Person", "title" to "Film"),
                            "category" to "CAST",
                            "label" to "Actor",
                        ),
                    ) as Map<*, *>
                val problem = result["problem"] as Map<*, *>
                assertEquals("CONFLICT", problem["code"])
                assertEquals("The operation could not be completed.", problem["message"])
                assertFalse(result.toString().contains("private database detail"))
            }
        }

    @Test
    fun `managed operations reject anonymous access and malformed identities before catalog invocation`() =
        fixture { f ->
            for (field in listOf("managedTitle", "managedPerson")) {
                assertThrows(InputViolation::class.java) { f.call("Query", field, mapOf("id" to "invalid")) }
                assertThrows(
                    CredentialRejected::class.java,
                ) { f.call("Query", field, mapOf("id" to id), BrowserContext(null, null, null, null)) }
            }
            assertEquals(0, f.service.reads)
        }

    @Test
    fun `public catalog reads project transport results without managed artwork access`() =
        fixture { f ->
            val title = f.call("Query", "title", mapOf("id" to id)) as Map<*, *>
            assertEquals("Film", title["title"])
            assertEquals(false, title["_artworkManagement"])
            assertEquals("MONTH", (title["release"] as Map<*, *>)["precision"])
            val person = f.call("Query", "person", mapOf("id" to id)) as Map<*, *>
            assertEquals("Person", person["name"])
            assertNull(person["biography"])
            for (field in listOf("titleCredits", "personCredits")) {
                val page = f.call("Query", field, mapOf("id" to id, "first" to 2, "after" to "cursor")) as Map<*, *>
                assertEquals(1, (page["nodes"] as List<*>).size)
                assertEquals(id, f.service.page!!.targetId)
                assertEquals("cursor", f.service.page!!.after)
            }
            val people = f.call("Query", "people", mapOf("text" to "Person", "first" to 2, "after" to "cursor")) as Map<*, *>
            assertEquals(1, (people["nodes"] as List<*>).size)
            assertEquals("Person", f.service.search!!.text)
            val titles = f.call("Query", "titles", mapOf("filter" to mapOf("text" to "Film"), "first" to 2)) as Map<*, *>
            assertEquals(1, (titles["nodes"] as List<*>).size)
            assertEquals("Film", f.service.search!!.text)
            assertEquals(listOf(mapOf("id" to "drama", "label" to "Drama")), f.call("Query", "genres", emptyMap()))
            val roles = f.call("Query", "creditRoles", emptyMap()) as List<*>
            assertEquals("Actor", (roles.single() as Map<*, *>)["label"])
            f.service.absent = true
            assertNull(f.call("Query", "title", mapOf("id" to id)))
            assertNull(f.call("Query", "person", mapOf("id" to id)))
        }

    @Test
    fun `lifecycle writes carry confirmation version policy and impact token with safe receipts`() =
        fixture { f ->
            for (field in listOf("movieLifecycle", "personLifecycle")) {
                val result =
                    f.call(
                        "Mutation",
                        field,
                        mapOf(
                            "id" to id,
                            "expectedVersion" to "9",
                            "action" to "ARCHIVE",
                            "confirmed" to true,
                            "impactToken" to "impact",
                        ),
                    ) as Map<*, *>
                val request = f.service.lifecycle!!
                assertEquals(id, request.target.id)
                assertEquals(9L, request.target.expectedVersion)
                assertEquals("current-revision", request.target.context.policyRevision)
                assertEquals("ARCHIVE", request.action)
                assertTrue(request.confirmed)
                assertEquals(if (field == "personLifecycle") "impact" else "", request.impactToken)
                assertNull(result["problem"])
                assertEquals("SUCCEEDED", f.context.operation(operation, OperationOwner.CATALOG)!!["state"])
            }
            val impact = f.call("Query", "deletionImpact", mapOf("personId" to id)) as Map<*, *>
            assertEquals(id, impact["targetId"])
            assertEquals(2, impact["affectedCreditCount"])
            assertEquals(listOf("Film"), impact["affectedTitles"])
        }

    @Test
    fun `audit preserves typed before after values and protects omitted large values`() =
        fixture { f ->
            val result = f.call("Query", "audit", mapOf("subjectId" to id, "first" to 2, "after" to "cursor")) as Map<*, *>
            val event = (result["nodes"] as List<*>).single() as Map<*, *>
            assertEquals(id, event["subjectId"])
            assertEquals(operation.toString(), event["operationId"])
            val changes = event["changes"] as Map<*, *>
            assertEquals(mapOf("before" to "Old", "after" to "New"), changes["title"])
            assertEquals(mapOf("before" to null, "after" to listOf("JP")), changes["countries"])
            assertEquals("cursor", f.service.page!!.after)
            f.service.invalidAudit = true
            assertThrows(io.grpc.StatusRuntimeException::class.java) { f.call("Query", "audit", emptyMap()) }
        }

    private fun awaitReady(channel: io.grpc.ManagedChannel) {
        val deadline =
            System.nanoTime() +
                java.util.concurrent.TimeUnit.SECONDS
                    .toNanos(30)
        while (true) {
            val state = channel.getState(true)
            if (state == io.grpc.ConnectivityState.READY) return
            val remaining = deadline - System.nanoTime()
            assertTrue(remaining > 0, "Local catalog fixture startup timed out")
            val changed = java.util.concurrent.CountDownLatch(1)
            channel.notifyWhenStateChanged(state, changed::countDown)
            assertTrue(changed.await(remaining, java.util.concurrent.TimeUnit.NANOSECONDS))
        }
    }

    private fun fixture(test: (Harness) -> Unit) {
        val service = CatalogFixture(id, operation)
        val server =
            NettyServerBuilder
                .forAddress(InetSocketAddress("127.0.0.1", 0))
                .addService(service)
                .build()
                .start()
        val channel = NettyChannelBuilder.forAddress("127.0.0.1", server.port).usePlaintext().build()
        try {
            awaitReady(channel)
            val keys = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
            val issuer = ActorIssuer(ActorSigner("test", keys.private))
            val wiring = RuntimeWiring.newRuntimeWiring()
            val stub = CatalogServiceGrpc.newBlockingStub(channel)
            ManagedCatalogGraphql(stub, issuer) { policy }.bind(wiring)
            ManagedPersonGraphql(stub, issuer) { policy }.bind(wiring)
            ManagedCreditGraphql(stub, issuer) { policy }.bind(wiring)
            com.spoolin.core.features.catalogview.lifecycle
                .LifecycleGraphql(stub, issuer) { "current-revision" }
                .bind(wiring)
            com.spoolin.core.features.catalogview.read
                .CatalogViewGraphql(stub, policy)
                .bind(wiring)
            val principal =
                SessionPrincipal(
                    UUID.randomUUID(),
                    AccountAuthority(UUID.randomUUID(), AccountRole.MAINTAINER, 1),
                    Instant.now().plusSeconds(3600),
                    "csrf",
                )
            test(Harness(service, wiring.build(), BrowserContext(principal, null, null, null)))
        } finally {
            channel.shutdownNow()
            server.shutdownNow()
        }
    }

    private inner class Harness(
        val service: CatalogFixture,
        val wiring: RuntimeWiring,
        val context: BrowserContext,
    ) {
        fun call(
            type: String,
            field: String,
            args: Map<String, Any?>,
            browser: BrowserContext = context,
        ): Any? {
            val environment =
                DataFetchingEnvironmentImpl
                    .newDataFetchingEnvironment()
                    .arguments(mapOf("operationId" to operation.toString(), "policyRevision" to "revision") + args)
                    .graphQLContext(GraphQLContext.newContext().of("browser", browser).build())
                    .build()
            return wiring.dataFetchers
                .getValue(type)
                .getValue(field)
                .get(environment)
        }
    }

    private class CatalogFixture(
        private val id: String,
        private val operation: UUID,
    ) : CatalogServiceGrpc.CatalogServiceImplBase() {
        var personWrite: Spoolin.PersonWrite? = null
        var movieWrite: Spoolin.MovieWrite? = null
        var creditWrite: Spoolin.CreditWrite? = null
        var roleWrite: Spoolin.RoleWrite? = null
        var removal: Spoolin.VersionedId? = null
        var page: Spoolin.PageRequest? = null
        var search: Spoolin.Search? = null
        var lifecycle: Spoolin.Lifecycle? = null
        var invalidAudit = false
        var absent = false
        var rejected = false
        var reads = 0

        private fun outcome(): Spoolin.Outcome =
            Spoolin.Outcome
                .newBuilder()
                .setOperationId(
                    operation.toString(),
                ).setState(if (rejected) "REJECTED" else "SUCCEEDED")
                .setResultId(id)
                .setVersion(7)
                .also {
                    if (rejected) {
                        it.problem =
                            Spoolin.Problem
                                .newBuilder()
                                .setCode("CONFLICT")
                                .build()
                    }
                }.build()

        private fun person(): Spoolin.Person =
            if (absent) {
                Spoolin.Person.getDefaultInstance()
            } else {
                Spoolin.Person
                    .newBuilder()
                    .setId(
                        id,
                    ).setState("ACTIVE")
                    .setVersion(7)
                    .setValues(Spoolin.PersonValues.newBuilder().setName("Person"))
                    .build()
            }

        private fun title(): Spoolin.Title =
            if (absent) {
                Spoolin.Title.getDefaultInstance()
            } else {
                Spoolin.Title
                    .newBuilder()
                    .setId(
                        id,
                    ).setKind(
                        "MOVIE",
                    ).setState(
                        "ACTIVE",
                    ).setVersion(
                        7,
                    ).setAssetId(
                        "asset",
                    ).setValues(
                        Spoolin.MovieValues
                            .newBuilder()
                            .setTitle(
                                "Film",
                            ).addGenreIds("drama")
                            .setRelease(
                                Spoolin.PartialDate
                                    .newBuilder()
                                    .setYear(2024)
                                    .setMonth(10),
                            ),
                    ).build()
            }

        private fun <T> reply(
            observer: StreamObserver<T>,
            result: T,
        ) {
            observer.onNext(result)
            observer.onCompleted()
        }

        override fun movieLifecycle(
            request: Spoolin.Lifecycle,
            observer: StreamObserver<Spoolin.Outcome>,
        ) {
            lifecycle = request
            reply(observer, outcome())
        }

        override fun personLifecycle(
            request: Spoolin.Lifecycle,
            observer: StreamObserver<Spoolin.Outcome>,
        ) {
            lifecycle = request
            reply(observer, outcome())
        }

        override fun deletionImpact(
            request: Spoolin.Id,
            observer: StreamObserver<Spoolin.Impact>,
        ) {
            reply(
                observer,
                Spoolin.Impact
                    .newBuilder()
                    .setTargetId(id)
                    .setToken("impact")
                    .setAffectedCreditCount(2)
                    .addAffectedTitles("Film")
                    .build(),
            )
        }

        override fun audit(
            request: Spoolin.PageRequest,
            observer: StreamObserver<Spoolin.AuditPage>,
        ) {
            page = request
            val event =
                Spoolin.AuditEvent
                    .newBuilder()
                    .setId(
                        id,
                    ).setActorRef(
                        "maintainer",
                    ).setAction("UPDATE")
                    .setOutcome("SUCCEEDED")
                    .setSubjectKind("TITLE")
                    .setSubjectId(id)
                    .setOperationId(operation.toString())
                    .addChanges(
                        Spoolin.AuditChange
                            .newBuilder()
                            .setPath(
                                "title",
                            ).setBeforeJson("\"Old\"")
                            .setAfterJson(if (invalidAudit) "{" else "\"New\""),
                    ).addChanges(
                        Spoolin.AuditChange
                            .newBuilder()
                            .setPath("countries")
                            .setAfterJson("[\"JP\"]"),
                    )
            reply(
                observer,
                Spoolin.AuditPage
                    .newBuilder()
                    .addNodes(event)
                    .build(),
            )
        }

        override fun getPerson(
            request: Spoolin.Id,
            observer: StreamObserver<Spoolin.Person>,
        ) = managedPerson(request, observer)

        override fun getTitle(
            request: Spoolin.Id,
            observer: StreamObserver<Spoolin.Title>,
        ) = managedTitle(request, observer)

        override fun searchPeople(
            request: Spoolin.Search,
            observer: StreamObserver<Spoolin.PersonPage>,
        ) {
            search = request
            reply(
                observer,
                Spoolin.PersonPage
                    .newBuilder()
                    .addNodes(person())
                    .build(),
            )
        }

        override fun searchTitles(
            request: Spoolin.Search,
            observer: StreamObserver<Spoolin.TitlePage>,
        ) {
            search = request
            reply(
                observer,
                Spoolin.TitlePage
                    .newBuilder()
                    .addNodes(title())
                    .build(),
            )
        }

        override fun titleCredits(
            request: Spoolin.PageRequest,
            observer: StreamObserver<Spoolin.CreditPage>,
        ) = managedCredits(request, observer)

        override fun personCredits(
            request: Spoolin.PageRequest,
            observer: StreamObserver<Spoolin.CreditPage>,
        ) = managedCredits(request, observer)

        override fun listGenres(
            request: com.google.protobuf.Empty,
            observer: StreamObserver<Spoolin.Genres>,
        ) {
            reply(
                observer,
                Spoolin.Genres
                    .newBuilder()
                    .addNodes(
                        Spoolin.Genre
                            .newBuilder()
                            .setId("drama")
                            .setLabel("Drama"),
                    ).build(),
            )
        }

        override fun listRoles(
            request: com.google.protobuf.Empty,
            observer: StreamObserver<Spoolin.Roles>,
        ) {
            reply(
                observer,
                Spoolin.Roles
                    .newBuilder()
                    .addNodes(
                        Spoolin.CreditRole
                            .newBuilder()
                            .setId("actor")
                            .setLabel("Actor")
                            .setCategory("CAST")
                            .setAllowsCharacters(true),
                    ).build(),
            )
        }

        override fun writePerson(
            request: Spoolin.PersonWrite,
            observer: StreamObserver<Spoolin.Outcome>,
        ) {
            personWrite = request
            reply(observer, outcome())
        }

        override fun writeMovie(
            request: Spoolin.MovieWrite,
            observer: StreamObserver<Spoolin.Outcome>,
        ) {
            movieWrite = request
            reply(observer, outcome())
        }

        override fun writeCredit(
            request: Spoolin.CreditWrite,
            observer: StreamObserver<Spoolin.Outcome>,
        ) {
            creditWrite = request
            reply(observer, outcome())
        }

        override fun createRole(
            request: Spoolin.RoleWrite,
            observer: StreamObserver<Spoolin.Outcome>,
        ) {
            roleWrite = request
            reply(observer, outcome())
        }

        override fun removeCredit(
            request: Spoolin.VersionedId,
            observer: StreamObserver<Spoolin.Outcome>,
        ) {
            removal = request
            reply(observer, outcome())
        }

        override fun managedPerson(
            request: Spoolin.Id,
            observer: StreamObserver<Spoolin.Person>,
        ) {
            reads++
            reply(observer, person())
        }

        override fun managedTitle(
            request: Spoolin.Id,
            observer: StreamObserver<Spoolin.Title>,
        ) {
            reads++
            reply(observer, title())
        }

        override fun managedPeople(
            request: Spoolin.PageRequest,
            observer: StreamObserver<Spoolin.PersonPage>,
        ) {
            page = request
            reply(
                observer,
                Spoolin.PersonPage
                    .newBuilder()
                    .addNodes(person())
                    .setEndCursor("next")
                    .setHasNextPage(true)
                    .build(),
            )
        }

        override fun managedTitles(
            request: Spoolin.PageRequest,
            observer: StreamObserver<Spoolin.TitlePage>,
        ) {
            page = request
            reply(
                observer,
                Spoolin.TitlePage
                    .newBuilder()
                    .addNodes(title())
                    .build(),
            )
        }

        override fun managedCredits(
            request: Spoolin.PageRequest,
            observer: StreamObserver<Spoolin.CreditPage>,
        ) {
            page = request
            reply(
                observer,
                Spoolin.CreditPage
                    .newBuilder()
                    .addNodes(
                        Spoolin.Credit
                            .newBuilder()
                            .setId(
                                id,
                            ).setPersonId(
                                id,
                            ).setPersonName(
                                "Person",
                            ).setTitleId(
                                id,
                            ).setTitleName(
                                "Film",
                            ).setVersion(
                                7,
                            ).addCharacters(
                                "Lead",
                            ).setRole(
                                Spoolin.CreditRole
                                    .newBuilder()
                                    .setId("actor")
                                    .setLabel("Actor")
                                    .setCategory("CAST")
                                    .setAllowsCharacters(true),
                            ),
                    ).build(),
            )
        }
    }
}
