package com.spoolin.integration.catalog.reads

import com.spoolin.integration.identity.SignedReceiptFixture
import com.spoolin.support.identity.ActorRpc
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.Spoolin.Id
import java.util.concurrent.TimeUnit

/**
 * Cross-service catalog read integration: receipt operation state is readable under scoped admission.
 */
class CatalogReadsBoundaryTest {
    @Test
    fun `catalog operation read uses live identity and receipt owner`() {
        SignedReceiptFixture("t009-catalog-read-boundary.json").use { proof ->
            val token = proof.signer.sign(proof.claims)
            val request = Id.newBuilder().setId(proof.operation.toString()).build()
            val metadata = Metadata().apply { put(ActorRpc.metadata, token) }
            val stub =
                CatalogServiceGrpc
                    .newBlockingStub(proof.caller)
                    .withDeadlineAfter(3, TimeUnit.SECONDS)
                    .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
            val operation = stub.getOperation(request)
            assertEquals("SUCCEEDED", operation.state)
            assertEquals(7, operation.version)
        }
    }

    @Test
    fun `public catalog methods read real owner state and preserve hidden visibility`() {
        PublicCatalogReadFixture().use { proof ->
            val stub = CatalogServiceGrpc.newBlockingStub(proof.caller).withDeadlineAfter(3, TimeUnit.SECONDS)
            val page =
                stub.searchTitles(
                    spoolin.v1.Spoolin.Search
                        .newBuilder()
                        .setFirst(24)
                        .build(),
                )
            assertEquals(listOf(proof.title.toString()), page.nodesList.map { it.id })
            val title = stub.getTitle(Id.newBuilder().setId(proof.title.toString()).build())
            assertEquals("Boundary movie", title.values.title)
            assertEquals(
                "Synthetic test permission",
                title.provenanceList
                    .single()
                    .fieldSourcesList
                    .single()
                    .reuseBasis,
            )
            org.junit.jupiter.api.Assertions
                .assertFalse(title.toString().contains("must-not-leak"))
            assertEquals("", stub.getTitle(Id.newBuilder().setId(proof.archived.toString()).build()).id)
            assertEquals(
                "Boundary drama",
                stub
                    .listGenres(
                        com.google.protobuf.Empty
                            .getDefaultInstance(),
                    ).nodesList
                    .single()
                    .label,
            )
            assertEquals("Boundary person", stub.getPerson(Id.newBuilder().setId(proof.person.toString()).build()).values.name)
            assertEquals(
                1,
                stub
                    .searchPeople(
                        spoolin.v1.Spoolin.Search
                            .newBuilder()
                            .setFirst(24)
                            .build(),
                    ).nodesCount,
            )
            assertEquals(0, stub.titleCredits(page(proof.title)).nodesCount)
            assertEquals(0, stub.personCredits(page(proof.person)).nodesCount)
            org.junit.jupiter.api.Assertions
                .assertTrue(
                    stub
                        .listRoles(
                            com.google.protobuf.Empty
                                .getDefaultInstance(),
                        ).nodesCount > 0,
                )
            val denied =
                org.junit.jupiter.api.Assertions.assertThrows(io.grpc.StatusRuntimeException::class.java) {
                    CatalogServiceGrpc
                        .newBlockingStub(proof.wrongCaller)
                        .withDeadlineAfter(3, TimeUnit.SECONDS)
                        .getTitle(Id.newBuilder().setId(proof.title.toString()).build())
                }
            assertEquals(io.grpc.Status.Code.PERMISSION_DENIED, denied.status.code)
        }
    }

    @Test
    fun `production GraphQL title response resolves real genre labels and source facts over mTLS`() {
        PublicCatalogReadFixture().use { proof ->
            val catalog = CatalogServiceGrpc.newBlockingStub(proof.caller)
            val policy =
                com.spoolin.core.features.policy.PolicyProvider(
                    com.spoolin.support.policy.PolicyEnvironment
                        .resolve { null },
                )
            val views =
                com.spoolin.core.boundary.graphql.PolicyGraphqlViews(
                    catalog =
                        com.spoolin.core.features.catalogview.read
                            .CatalogViewGraphql(catalog, policy.policy),
                )
            val schema =
                java.nio.file.Files
                    .readString(
                        proof.fixture.owner.repository
                            .resolve("contracts/graphql/schema.graphql"),
                    )
            val engine =
                com.spoolin.core.boundary.graphql
                    .PolicyGraphql(schema, policy, views)
            val result =
                engine.execute(
                    mapOf("query" to "{ titles(filter:{}) { nodes { title genres { id label } provenance { fieldSources } } } }"),
                )
            org.junit.jupiter.api.Assertions
                .assertFalse(result.containsKey("errors"), result.toString())
            org.junit.jupiter.api.Assertions
                .assertTrue(result.toString().contains("Boundary drama"))
            org.junit.jupiter.api.Assertions
                .assertTrue(result.toString().contains("Synthetic test permission"))
            org.junit.jupiter.api.Assertions
                .assertFalse(result.toString().contains("must-not-leak"))
        }
    }

    @Test
    fun `retained ResolveActor contract denies all four service callers before dispatch`() {
        PublicCatalogReadFixture().use { proof ->
            val transport =
                com.spoolin.integration.identity
                    .ScopedActorTest()
            val rpc =
                com.spoolin.core.boundary.grpc
                    .IdentityRpc({ null }, { null })
            val server = transport.server(proof.fixture.owner.root, "core", rpc, com.spoolin.core.ServiceIdentityInterceptor())
            try {
                listOf("core", "catalog", "media", "mcp").forEach { identity ->
                    val channel = transport.channel(proof.fixture.owner.root, identity, "core", server.port)
                    try {
                        val failure =
                            org.junit.jupiter.api.Assertions.assertThrows(io.grpc.StatusRuntimeException::class.java) {
                                spoolin.v1.IdentityServiceGrpc
                                    .newBlockingStub(channel)
                                    .withDeadlineAfter(3, TimeUnit.SECONDS)
                                    .resolveActor(
                                        spoolin.v1.Spoolin.ResolveActorRequest
                                            .getDefaultInstance(),
                                    )
                            }
                        assertEquals(io.grpc.Status.Code.PERMISSION_DENIED, failure.status.code, identity)
                    } finally {
                        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
                    }
                }
            } finally {
                server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
            }
        }
    }

    private fun page(id: java.util.UUID): spoolin.v1.Spoolin.PageRequest =
        spoolin.v1.Spoolin.PageRequest
            .newBuilder()
            .setTargetId(id.toString())
            .setFirst(24)
            .build()
}
