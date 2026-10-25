package com.spoolin.core.features.artworkproxy

import io.grpc.netty.NettyChannelBuilder
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.MediaServiceGrpc
import spoolin.v1.Spoolin
import java.util.UUID

class ArtworkOutcomeBehaviorTest {
    private val operation = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001")
    private val upload = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001")

    @Test
    fun `cancellation reports both receipts without exposing internal problem detail`() = resolver { resolver ->
        val outcome = Spoolin.ArtworkCancelOutcome.newBuilder()
            .setOperation(receipt(operation, "SUCCEEDED"))
            .setUploadOperation(receipt(upload, "FAILED").toBuilder().setProblem(Spoolin.Problem.newBuilder().setCode("CANCELLED")))
            .setCancelled(true).build()
        val result = resolver.cancellationResult(outcome, operation, upload)
        assertEquals(true, result["cancelled"])
        val original = result["uploadOperation"] as Map<*, *>
        assertEquals(upload.toString(), original["id"])
        assertEquals("FAILED", original["state"])
        assertEquals("CANCELLED", (original["problem"] as Map<*, *>)["code"])
    }

    @Test
    fun `unknown upload remains absent when cancellation did not occur`() = resolver { resolver ->
        val result = resolver.cancellationResult(
            Spoolin.ArtworkCancelOutcome.newBuilder().setOperation(receipt(operation, "SUCCEEDED")).build(), operation, upload,
        )
        assertEquals(false, result["cancelled"])
        assertNull(result["uploadOperation"])
    }

    @Test
    fun `contradictory or malformed upstream receipts fail closed as data loss`() = resolver { resolver ->
        val malformed = listOf(
            receipt(upload, "SUCCEEDED"),
            receipt(operation, "UNKNOWN"),
            receipt(operation, "SUCCEEDED").toBuilder().setVersion(-1).build(),
            receipt(operation, "EXPIRED").toBuilder().setResultExpired(true).setResultId(upload.toString()).build(),
            receipt(operation, "EXPIRED").toBuilder().setResultExpired(true).setVersion(1).build(),
            receipt(operation, "SUCCEEDED").toBuilder().setResultId("invalid").build(),
        )
        for (receipt in malformed) {
            val outcome = Spoolin.ArtworkCancelOutcome.newBuilder().setOperation(receipt).build()
            val error = assertThrows(StatusRuntimeException::class.java) { resolver.cancellationResult(outcome, operation, upload) }
            assertEquals(Status.Code.DATA_LOSS, error.status.code)
        }
        assertThrows(StatusRuntimeException::class.java) {
            resolver.cancellationResult(Spoolin.ArtworkCancelOutcome.newBuilder().setOperation(receipt(operation, "SUCCEEDED")).setCancelled(true).build(), operation, upload)
        }
        assertThrows(StatusRuntimeException::class.java) {
            resolver.cancellationResult(Spoolin.ArtworkCancelOutcome.newBuilder().setOperation(receipt(operation, "SUCCEEDED")).setUploadOperation(receipt(operation, "FAILED")).build(), operation, upload)
        }
    }

    @Test
    fun `invalid artwork content produces file rejection before any transport access`() = resolver { resolver ->
        val principal = com.spoolin.core.features.session.SessionPrincipal(
            UUID.randomUUID(),
            com.spoolin.core.identity.AccountAuthority(UUID.randomUUID(), com.spoolin.core.identity.AccountRole.MAINTAINER, 1),
            java.time.Instant.now().plusSeconds(60), "csrf",
        )
        val browser = com.spoolin.core.boundary.http.BrowserContext(principal, null, null, null)
        val wiring = graphql.schema.idl.RuntimeWiring.newRuntimeWiring()
        resolver.bind(wiring)
        val fetch = wiring.build().dataFetchers.getValue("Mutation").getValue("uploadArtwork")
        val args = mapOf("operationId" to operation.toString(), "titleId" to upload.toString(), "expectedVersion" to "3", "policyRevision" to "revision", "content" to "%%invalid-base64")
        val env = graphql.schema.DataFetchingEnvironmentImpl.newDataFetchingEnvironment().arguments(args)
            .graphQLContext(graphql.GraphQLContext.newContext().of("browser", browser).build()).build()
        val result = fetch.get(env) as Map<*, *>
        assertEquals("FILE_INVALID", (result["problem"] as Map<*, *>)["code"])
        assertEquals("FAILED", (result["operation"] as Map<*, *>)["state"])
        assertNull(browser.operation(operation, com.spoolin.support.receipts.OperationOwner.CATALOG))
        val noArtwork = wiring.build().dataFetchers.getValue("Title").getValue("artwork")
        assertNull(noArtwork.get(graphql.schema.DataFetchingEnvironmentImpl.newDataFetchingEnvironment().source(emptyMap<String, Any?>()).build()))
    }

    private fun receipt(id: UUID, state: String) = Spoolin.Outcome.newBuilder().setOperationId(id.toString()).setState(state).build()
    private fun resolver(test: (ArtworkGraphql) -> Unit) {
        // This channel is never contacted: cancellation projection validates an already received DTO.
        val channel = NettyChannelBuilder.forAddress("127.0.0.1", 1).usePlaintext().build()
        try {
            test(ArtworkGraphql(CatalogServiceGrpc.newBlockingStub(channel), MediaServiceGrpc.newStub(channel), null))
        } finally {
            channel.shutdownNow()
        }
    }
}
