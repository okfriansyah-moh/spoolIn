package com.spoolin.core.features.catalogview.read

import com.spoolin.core.boundary.graphql.PreparedGraphql
import com.spoolin.core.boundary.http.BrowserAdmission
import com.spoolin.core.boundary.http.BrowserIdentity
import com.spoolin.core.features.ratelimit.RateAdmission
import com.spoolin.core.features.ratelimit.TrustedAdmission
import com.spoolin.core.features.session.AnonymousCsrf
import com.spoolin.core.features.session.SessionCsrf
import com.spoolin.core.features.session.SessionStore
import com.spoolin.support.identity.RateProfiles
import com.spoolin.support.policy.ConfigurationSources
import com.spoolin.support.policy.PolicyEnvironment
import graphql.ExecutionInput
import graphql.GraphQL
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import io.grpc.ConnectivityState
import io.grpc.ManagedChannel
import io.grpc.netty.NettyChannelBuilder
import io.grpc.netty.NettyServerBuilder
import io.grpc.stub.StreamObserver
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.jdbc.datasource.AbstractDataSource
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.Spoolin
import java.lang.reflect.Proxy
import java.net.InetSocketAddress
import java.sql.Connection
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class CatalogFilterOptionsRulesTest {
    @Test
    fun `public filter options project genre labels and year month availability over grpc`() {
        val server =
            NettyServerBuilder
                .forAddress(InetSocketAddress("127.0.0.1", 0))
                .addService(Fixture())
                .build()
                .start()
        val channel = NettyChannelBuilder.forAddress("127.0.0.1", server.port).usePlaintext().build()
        try {
            awaitReady(channel)
            val wiring = RuntimeWiring.newRuntimeWiring()
            val catalog = CatalogServiceGrpc.newBlockingStub(channel)
            CatalogViewGraphql(catalog, PolicyEnvironment.resolve { null }).bind(wiring)
            val schema = SchemaGenerator().makeExecutableSchema(SchemaParser().parse(SCHEMA), wiring.build())
            val result =
                GraphQL.newGraphQL(schema).build().execute(
                    "{ catalogFilterOptions { genres { id label } releaseYears { year months } } }",
                )
            assertTrue(result.errors.isEmpty(), result.errors.toString())
            val data = result.getData<Map<String, Any?>>()
            assertEquals(
                mapOf(
                    "genres" to listOf(mapOf("id" to "drama", "label" to "Drama")),
                    "releaseYears" to listOf(mapOf("year" to 1999, "months" to listOf(3))),
                ),
                data["catalogFilterOptions"],
            )
        } finally {
            channel.shutdownNow()
            server.shutdownNow()
        }
    }

    @Test
    fun `public filter metadata bypasses browser identity before source admission`() {
        val provider = unusedDatabase()
        val csrf =
            SessionCsrf(ByteArray(32) { 17 })
        val identity =
            BrowserIdentity(
                SessionStore(provider, csrf),
                AnonymousCsrf(ByteArray(32) { 18 }),
                csrf,
            )
        val marker = IllegalStateException("Source admission reached")
        val limiter =
            RateAdmission(
                provider,
                RateProfiles(
                    ConfigurationSources(),
                ),
                ByteArray(32) { 19 },
                { throw marker },
            )
        val request =
            Proxy.newProxyInstance(
                javaClass.classLoader,
                arrayOf(HttpServletRequest::class.java),
            ) { _, method, _ ->
                check(method.name == "getRemoteAddr") { "Public read must not inspect browser identity" }
                "127.0.0.1"
            } as HttpServletRequest
        val parsed =
            PreparedGraphql(
                ExecutionInput.newExecutionInput("{ catalogFilterOptions { releaseYears { year } } }").build(),
                false,
                listOf("catalogFilterOptions"),
            )
        val admission =
            BrowserAdmission(
                TrustedAdmission(limiter),
            )
        val error =
            assertThrows(IllegalStateException::class.java) {
                admission.context(request, parsed, identity, System.nanoTime() + 1_000_000_000)
            }
        assertSame(marker, error)
    }

    private fun awaitReady(channel: ManagedChannel) {
        // Fixture startup must finish before the application RPC deadline begins.
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        while (true) {
            val state = channel.getState(true)
            if (state == ConnectivityState.READY) return
            val remaining = deadline - System.nanoTime()
            assertTrue(remaining > 0, "Local gRPC fixture did not become ready")
            val changed = CountDownLatch(1)
            channel.notifyWhenStateChanged(state, changed::countDown)
            assertTrue(changed.await(remaining, TimeUnit.NANOSECONDS), "Local gRPC fixture startup timed out")
        }
    }

    private fun unusedDatabase(): AbstractDataSource =
        object : AbstractDataSource() {
            override fun getConnection(): Connection = error("Database must not be reached")

            override fun getConnection(
                username: String,
                password: String,
            ): Connection = getConnection()
        }

    private class Fixture : CatalogServiceGrpc.CatalogServiceImplBase() {
        private val genre =
            Spoolin.Genre
                .newBuilder()
                .setId("drama")
                .setLabel("Drama")
                .build()

        override fun getCatalogFilterOptions(
            request: com.google.protobuf.Empty,
            response: StreamObserver<Spoolin.CatalogFilterOptions>,
        ) {
            response.onNext(
                Spoolin.CatalogFilterOptions
                    .newBuilder()
                    .addGenres(genre)
                    .addReleaseYears(
                        Spoolin.CatalogReleaseYear
                            .newBuilder()
                            .setYear(1999)
                            .addMonths(3),
                    ).build(),
            )
            response.onCompleted()
        }

        override fun listGenres(
            request: com.google.protobuf.Empty,
            response: StreamObserver<Spoolin.Genres>,
        ) {
            response.onNext(
                Spoolin.Genres
                    .newBuilder()
                    .addNodes(genre)
                    .build(),
            )
            response.onCompleted()
        }
    }

    companion object {
        private const val SCHEMA =
            "type Query { catalogFilterOptions: CatalogFilterOptions! } " +
                "type CatalogFilterOptions { genres: [Genre!]! releaseYears: [CatalogReleaseYear!]! } " +
                "type Genre { id: ID! label: String! } " +
                "type CatalogReleaseYear { year: Int! months: [Int!]! } " +
                "type Title { credits: String }"
    }
}
