package com.spoolin.core.coveragehttp

import com.google.protobuf.ByteString
import com.spoolin.core.boundary.grpc.ActorIssuer
import com.spoolin.core.boundary.http.BrowserContext
import com.spoolin.core.features.artworkproxy.ArtworkGraphql
import com.spoolin.core.features.artworkproxy.ArtworkReadProxy
import com.spoolin.core.features.session.SessionPrincipal
import com.spoolin.core.identity.AccountAuthority
import com.spoolin.core.identity.AccountRole
import com.spoolin.support.identity.ActorSigner
import com.spoolin.support.policy.InputViolation
import com.spoolin.support.receipts.OperationOwner
import com.spoolin.support.receipts.WorkRequest
import graphql.GraphQLContext
import graphql.schema.DataFetchingEnvironment
import graphql.schema.idl.RuntimeWiring
import io.grpc.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import spoolin.v1.CatalogServiceGrpc
import spoolin.v1.MediaServiceGrpc
import spoolin.v1.Spoolin
import java.lang.reflect.Proxy
import java.nio.file.Files
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import java.util.UUID

internal val artworkOperation: UUID = UUID.fromString("00000000-0000-4000-8000-000000000011")
internal val artworkAsset: UUID = UUID.fromString("00000000-0000-4000-8000-000000000012")
internal val artworkAccount: UUID = UUID.fromString("00000000-0000-4000-8000-000000000013")

internal fun artworkBrowser(role: AccountRole = AccountRole.MAINTAINER): BrowserContext =
    BrowserContext(
        SessionPrincipal(artworkOperation, AccountAuthority(artworkAccount, role, 1), Instant.now().plusSeconds(600), "csrf"),
        null,
        null,
        null,
    )

internal fun artworkIssuer() = ActorIssuer(ActorSigner("test", KeyPairGenerator.getInstance("Ed25519").generateKeyPair().private))

internal fun artworkGrant() =
    Spoolin.ArtworkGrant
        .newBuilder()
        .setAssetId(artworkAsset.toString())
        .setHoldId(artworkOperation.toString())
        .setTitleId(artworkAccount.toString())
        .setCatalogVersion(7)
        .build()

internal fun artworkChunks(): List<Spoolin.AssetReadChunk> {
    val bytes = "processed-png".toByteArray()
    return listOf(
        Spoolin.AssetReadChunk
            .newBuilder()
            .setHeader(
                Spoolin.AssetReadHeader
                    .newBuilder()
                    .setFormat("PNG")
                    .setOutputBytes(bytes.size.toLong())
                    .setWidth(10)
                    .setHeight(20)
                    .setContentHash(ByteString.copyFrom(MessageDigest.getInstance("SHA-256").digest(bytes))),
            ).build(),
        Spoolin.AssetReadChunk
            .newBuilder()
            .setData(ByteString.copyFrom(bytes))
            .build(),
    )
}

class ArtworkBoundaryRulesTest {
    private fun handler(
        catalog: HttpRpcReplies,
        media: HttpRpcReplies = HttpRpcReplies(),
        issuer: ActorIssuer? = artworkIssuer(),
        reads: ArtworkReadProxy? = null,
    ) = ArtworkGraphql(CatalogServiceGrpc.newBlockingStub(catalog), MediaServiceGrpc.newStub(media), issuer, reads = reads)

    private fun arguments() =
        mapOf(
            "operationId" to artworkOperation.toString(),
            "titleId" to artworkAccount.toString(),
            "expectedVersion" to "7",
            "policyRevision" to "policy",
            "content" to Base64.getEncoder().encodeToString("bytes".toByteArray()),
            "uploadOperationId" to artworkAsset.toString(),
        )

    private fun fetch(
        handler: ArtworkGraphql,
        field: String,
        browser: BrowserContext = artworkBrowser(),
        args: Map<String, Any?> = arguments(),
        source: Map<String, Any?> = emptyMap(),
    ): Any? {
        val wiring = RuntimeWiring.newRuntimeWiring()
        handler.bind(wiring)
        val context = GraphQLContext.newContext().of("browser", browser).build()
        val env =
            Proxy.newProxyInstance(
                DataFetchingEnvironment::class.java.classLoader,
                arrayOf(DataFetchingEnvironment::class.java),
            ) { _, method, values ->
                when (method.name) {
                    "getArgument" -> args[values[0]]
                    "getGraphQlContext" -> context
                    "getSource" -> source
                    else -> null
                }
            } as DataFetchingEnvironment
        val type = if (field == "artwork") "Title" else "Mutation"
        return wiring
            .build()
            .dataFetchers
            .getValue(type)
            .getValue(field)
            .get(env)
    }

    private fun outcome(
        state: String = "PENDING",
        operation: UUID = artworkOperation,
    ) = Spoolin.Outcome
        .newBuilder()
        .setOperationId(operation.toString())
        .setState(state)
        .build()

    @Test fun `invalid artwork input reports file problem without beginning downstream operation`() {
        val channel = HttpRpcReplies()
        val result = fetch(handler(channel), "uploadArtwork", args = arguments() + ("content" to "invalid base64!")) as Map<*, *>
        assertEquals("FILE_INVALID", (result["problem"] as Map<*, *>)["code"])
        assertTrue(channel.requests.isEmpty())
        assertThrows(com.spoolin.core.features.account.application.CredentialRejected::class.java) {
            fetch(handler(channel), "uploadArtwork", BrowserContext(null, null, null, null))
        }
        assertThrows(InputViolation::class.java) { fetch(handler(channel), "uploadArtwork", args = arguments() + ("titleId" to "bad")) }
    }

    @Test fun `upload begins attachment transmits bounded bytes and remembers both owner operations`() {
        val child = WorkRequest("media-upload", "ARTWORK", artworkOperation, Instant.MAX).id(artworkOperation)
        val catalog = HttpRpcReplies(outcome())
        val media = HttpRpcReplies(outcome("SUCCEEDED", child))
        val browser = artworkBrowser()
        val result = fetch(handler(catalog, media), "uploadArtwork", browser) as Map<*, *>
        assertEquals("PENDING", (result["operation"] as Map<*, *>)["state"])
        assertEquals("PENDING", browser.operation(artworkOperation, OperationOwner.CATALOG)?.get("state"))
        assertEquals("SUCCEEDED", browser.operation(child, OperationOwner.MEDIA)?.get("state"))
        val hold = catalog.requests.single() as Spoolin.Hold
        assertEquals(7L, hold.expectedVersion)
        assertEquals(artworkAccount.toString(), hold.actorId)
        val chunks = media.requests.map { it as Spoolin.UploadChunk }
        assertEquals("bytes", chunks.drop(1).joinToString("") { it.data.toStringUtf8() })
        assertEquals(5L, chunks.first().header.inputBytes)
        assertEquals(
            child.toString(),
            chunks
                .first()
                .header.context.operationId,
        )
    }

    @Test fun `completed attachment replay avoids media and projects safe downstream problem fields`() {
        val problem =
            Spoolin.Problem
                .newBuilder()
                .setCode(
                    "FILE_INVALID",
                ).addFields(
                    Spoolin.FieldIssue
                        .newBuilder()
                        .setPath("content")
                        .setCode("INVALID")
                        .setMessage("Choose an image"),
                )
        val catalog = HttpRpcReplies(outcome("FAILED").toBuilder().setProblem(problem).build())
        val media = HttpRpcReplies()
        val result = fetch(handler(catalog, media), "uploadArtwork") as Map<*, *>
        assertEquals("FILE_INVALID", (result["problem"] as Map<*, *>)["code"])
        assertTrue(media.requests.isEmpty())
    }

    @Test fun `downstream outcome identity state version and expiry invariants reject corrupt responses`() {
        val invalid =
            listOf(
                outcome(operation = artworkAsset),
                outcome("UNKNOWN"),
                outcome().toBuilder().setVersion(-1).build(),
                outcome().toBuilder().setResultId("bad").build(),
                outcome()
                    .toBuilder()
                    .setVersion(1)
                    .setResultExpired(true)
                    .build(),
            )
        for (response in invalid) {
            assertEquals(
                Status.Code.DATA_LOSS,
                assertThrows(io.grpc.StatusRuntimeException::class.java) {
                    fetch(handler(HttpRpcReplies(response)), "uploadArtwork")
                }.status.code,
            )
        }
        assertThrows(com.spoolin.core.features.ratelimit.RateUnavailable::class.java) {
            fetch(handler(HttpRpcReplies(), issuer = null), "uploadArtwork")
        }
    }

    @Test fun `cancellation validates both operation identities and records original cancellation state`() {
        val response =
            Spoolin.ArtworkCancelOutcome
                .newBuilder()
                .setOperation(outcome("SUCCEEDED"))
                .setUploadOperation(outcome("FAILED", artworkAsset))
                .setCancelled(true)
                .build()
        val channel = HttpRpcReplies(response)
        val result = fetch(handler(channel), "cancelArtwork") as Map<*, *>
        assertEquals(true, result["cancelled"])
        assertEquals("FAILED", (result["uploadOperation"] as Map<*, *>)["state"])
        assertEquals(artworkAsset.toString(), (channel.requests.single() as Spoolin.CancelArtworkRequest).uploadOperationId)
        assertEquals(
            Status.Code.DATA_LOSS,
            assertThrows(io.grpc.StatusRuntimeException::class.java) {
                handler(
                    HttpRpcReplies(),
                ).cancellationResult(response.toBuilder().clearUploadOperation().build(), artworkOperation, artworkAsset)
            }.status.code,
        )
        val absent =
            handler(
                HttpRpcReplies(),
            ).cancellationResult(
                response
                    .toBuilder()
                    .setCancelled(false)
                    .clearUploadOperation()
                    .build(),
                artworkOperation,
                artworkAsset,
            )
        assertNull(absent["uploadOperation"])
    }

    @Test fun `artwork field resolves verified dimensions and management URL only when asset exists`() {
        assertNull(fetch(handler(HttpRpcReplies()), "artwork"))
        val catalog = HttpRpcReplies(artworkGrant())
        val media = HttpRpcReplies(artworkChunks())
        val reads = ArtworkReadProxy(CatalogServiceGrpc.newBlockingStub(catalog), MediaServiceGrpc.newBlockingStub(media), artworkIssuer())
        val result =
            fetch(
                handler(HttpRpcReplies(), reads = reads),
                "artwork",
                source =
                    mapOf(
                        "_artworkAssetId" to artworkAsset.toString(),
                        "_artworkManagement" to true,
                    ),
            ) as Map<*, *>
        assertEquals(10, result["width"])
        assertEquals(20, result["height"])
        assertEquals("/artwork/$artworkAsset?view=management", result["url"])
        assertThrows(com.spoolin.core.features.ratelimit.RateUnavailable::class.java) {
            fetch(handler(HttpRpcReplies()), "artwork", source = mapOf("_artworkAssetId" to artworkAsset.toString()))
        }
    }

    @Test fun `read proxy authenticates management validates grants and deletes verified temporary bytes`() {
        val catalog = HttpRpcReplies(artworkGrant())
        val media = HttpRpcReplies(artworkChunks())
        val reads = ArtworkReadProxy(CatalogServiceGrpc.newBlockingStub(catalog), MediaServiceGrpc.newBlockingStub(media), artworkIssuer())
        val asset = reads.read(artworkAsset, false, BrowserContext(null, null, null, null))
        assertEquals("image/png", asset.contentType)
        assertEquals("processed-png", Files.readString(asset.path))
        asset.close()
        assertFalse(Files.exists(asset.path))
        for (browser in listOf(BrowserContext(null, null, null, null), artworkBrowser(AccountRole.VIEWER))) {
            assertEquals(
                Status.Code.PERMISSION_DENIED,
                assertThrows(io.grpc.StatusRuntimeException::class.java) {
                    reads.metadata(artworkAsset, true, browser)
                }.status.code,
            )
        }
        val invalid =
            ArtworkReadProxy(
                CatalogServiceGrpc.newBlockingStub(HttpRpcReplies(artworkGrant().toBuilder().setHoldId("bad").build())),
                MediaServiceGrpc.newBlockingStub(HttpRpcReplies()),
                null,
            )
        assertEquals(
            Status.Code.DATA_LOSS,
            assertThrows(io.grpc.StatusRuntimeException::class.java) {
                invalid.metadata(artworkAsset, false, BrowserContext(null, null, null, null))
            }.status.code,
        )
    }
}
