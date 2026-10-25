package com.spoolin.media.features.hold

import com.spoolin.media.boundary.grpc.MediaPeer
import com.spoolin.media.features.asset.AssetSql
import com.spoolin.media.features.asset.CatalogAttachments
import com.spoolin.media.fixtures.*
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import spoolin.v1.Spoolin.*
import java.time.Clock
import java.time.ZoneOffset
import java.util.UUID

class HoldWorkflowRulesTest {
    private fun command(action: HoldAction = HoldAction.RESERVE): HoldCommand {
        val provisional = HoldCommand(holdId, assetId, titleId, actorId, operationId, operationId, 4, 3, "p1", action)
        return provisional.copy(operation = provisional.child(action.step))
    }
    private fun request(command: HoldCommand): Hold = Hold.newBuilder().setHoldId(command.hold.toString())
        .setAssetId(command.asset.toString()).setTitleId(command.title.toString()).setActorId(command.actor.toString())
        .setCatalogVersion(command.version ?: 0).setExpectedVersion(command.expectedVersion ?: 0)
        .setContext(Context.newBuilder().setOperationId(command.operation.toString()).setCorrelationId(command.correlation.toString()).setPolicyRevision(command.policy))
        .build()
    private fun remote(state: String = "PREPARING"): Attachment = Attachment.newBuilder().setHoldId(holdId.toString())
        .setAssetId(assetId.toString()).setTitleId(titleId.toString()).setActorId(actorId.toString()).setCatalogVersion(4).setState(state).build()
    private fun record(state: String = "RESERVED") = mapOf<String, Any?>("id" to holdId, "asset_id" to assetId,
        "title_id" to titleId, "state" to state, "catalog_version" to 4L)
    private fun receipt(command: HoldCommand, state: String = "PENDING") = mapOf<String, Any?>(
        "actor_scope" to command.scope, "kind" to command.action.kind, "request_hash" to command.hash(), "state" to state)
    private fun denied(code: Status.Code, action: () -> Unit) {
        assertEquals(code, Status.fromThrowable(assertThrows(StatusRuntimeException::class.java, action)).code)
    }

    @Test fun `commands bind operation identities and reject malformed public requests`() {
        for (action in HoldAction.entries) {
            val expected = command(action)
            assertEquals(expected, HoldCommand.parse(request(expected), action))
            assertEquals("account:$actorId", expected.scope)
            assertArrayEquals(expected.hash(), HoldCommand.parse(request(expected), action).hash())
            assertFalse(expected.hash().contentEquals(expected.copy(policy = "p2").hash()))
        }
        val valid = request(command())
        for (invalid in listOf(valid.toBuilder().setHoldId("invalid").build(),
            valid.toBuilder().setActorId("").build(), valid.toBuilder().setCatalogVersion(-1).build(),
            valid.toBuilder().setExpectedVersion(-1).build(),
            valid.toBuilder().setContext(valid.context.toBuilder().setPolicyRevision("x".repeat(65))).build(),
            valid.toBuilder().setContext(valid.context.toBuilder().setOperationId(UUID.randomUUID().toString())).build())) {
            denied(Status.Code.INVALID_ARGUMENT) { HoldCommand.parse(invalid, HoldAction.RESERVE) }
        }
    }

    @Test fun `authority checks exact identity projection version and trusted peer`() {
        val boundary = CatalogBoundary(remote())
        val authority = HoldAuthority(CatalogAttachments(boundary.client))
        denied(Status.Code.PERMISSION_DENIED) { authority.verify(command()) }
        MediaPeer.context("catalog").run {
            assertEquals(remote(), authority.verify(command()))
            denied(Status.Code.UNAVAILABLE) { HoldAuthority(null).verify(command()) }
            for (invalid in listOf(remote().toBuilder().clearActorId().build(),
                remote().toBuilder().setTitleId(UUID.randomUUID().toString()).build(),
                remote().toBuilder().setAssetId(UUID.randomUUID().toString()).build(),
                remote().toBuilder().setActorId(UUID.randomUUID().toString()).build())) {
                boundary.attachment = invalid
                denied(Status.Code.PERMISSION_DENIED) { authority.verify(command()) }
            }
            for (invalid in listOf(remote("UNKNOWN"), remote().toBuilder().clearCatalogVersion().build(),
                remote().toBuilder().setCatalogVersion(-1).build())) {
                boundary.attachment = invalid
                denied(Status.Code.UNAVAILABLE) { authority.verify(command()) }
            }
        }
    }

    @Test fun `only bound uploads owned by the actor may reserve a hold`() {
        val c = command()
        val row = requireNotNull(AssetSql.find(JdbcBoundary().rows(assetRecord()).source, assetId))
        val bound = row.copy(id = c.child("asset"), uploadOperationId = c.child("media-upload"))
        val authority = HoldAuthority(null)
        authority.ownedUpload(bound, c)
        denied(Status.Code.PERMISSION_DENIED) { authority.ownedUpload(bound.copy(actorId = UUID.randomUUID()), c) }
        denied(Status.Code.PERMISSION_DENIED) { authority.ownedUpload(row, c) }
        authority.ownedUpload(row, c.copy(action = HoldAction.RELEASE))
    }

    @Test fun `prepared poster reserves require the original intent and derived asset identity`() {
        val c = command()
        val posterAsset = UUID.nameUUIDFromBytes("poster:$operationId".toByteArray())
        val prepared = remote().toBuilder().setHoldId(operationId.toString()).setTitleId(operationId.toString()).setAssetId(posterAsset.toString()).build()
        val authority = HoldAuthority(CatalogAttachments(CatalogBoundary(prepared).client))
        val row = requireNotNull(AssetSql.find(JdbcBoundary().rows(assetRecord()).source, assetId))
            .copy(id = posterAsset, provenance = "{\"preparedPoster\":true}")
        authority.ownedUpload(row, c)
        denied(Status.Code.PERMISSION_DENIED) { authority.ownedUpload(row.copy(id = UUID.randomUUID()), c) }
    }

    @Test fun `hold transitions refuse stale authority and regressing versions`() {
        val authority = HoldAuthority(null)
        val local = HoldRecord(holdId, assetId, titleId, "RESERVED", 3)
        authority.transition(command().copy(expectedVersion = 4), remote(), null)
        authority.transition(command().copy(expectedVersion = null), remote(), null)
        denied(Status.Code.FAILED_PRECONDITION) { authority.transition(command(), remote(), null) }
        for ((action, state) in listOf(HoldAction.CONFIRM to "ACTIVE", HoldAction.RELEASE to "RELEASED")) {
            authority.transition(command(action), remote(state), local)
            for (version in listOf<Long?>(null, 0, 2, 5)) {
                denied(Status.Code.FAILED_PRECONDITION) { authority.transition(command(action).copy(version = version), remote(state), local) }
            }
            denied(Status.Code.FAILED_PRECONDITION) { authority.transition(command(action), remote("PREPARING"), local) }
        }
    }

    @Test fun `receipt reservation rejects actor replay hash conflict and unfinished retries`() {
        val c = command()
        for ((row, code) in listOf(receipt(c) + ("actor_scope" to "account:other") to Status.Code.NOT_FOUND,
            receipt(c) + ("request_hash" to byteArrayOf(0)) to Status.Code.ALREADY_EXISTS,
            receipt(c) + ("kind" to "OTHER") to Status.Code.ALREADY_EXISTS)) {
            val db = JdbcBoundary().rows(row)
            denied(code) { HoldSql.reserveReceipt(db.connection, c, instant) }
        }
        val unfinished = JdbcBoundary().rows(receipt(c)).apply { updates.add(0) }
        denied(Status.Code.FAILED_PRECONDITION) { HoldSql.reserveReceipt(unfinished.connection, c, instant) }
        val unbound = JdbcBoundary().rows(receipt(c, "SUCCEEDED")).rows(mapOf("1" to 0)).apply { updates.add(0) }
        denied(Status.Code.UNAVAILABLE) { HoldSql.reserveReceipt(unbound.connection, c, instant) }
        val replay = JdbcBoundary().rows(receipt(c, "SUCCEEDED")).rows(mapOf("1" to 1)).apply { updates.add(0) }
        assertFalse(HoldSql.reserveReceipt(replay.connection, c, instant))
        assertTrue(HoldSql.reserveReceipt(JdbcBoundary().rows(receipt(c)).connection, c, instant))
    }

    @Test fun `reserve confirm release return committed tuple and preserve rollback on conflict`() {
        for ((action, state, previous) in listOf(Triple(HoldAction.RESERVE, "PREPARING", null),
            Triple(HoldAction.CONFIRM, "ACTIVE", "RESERVED"), Triple(HoldAction.RELEASE, "RELEASED", "ACTIVE"))) {
            val c = command(action).copy(expectedVersion = 4).let { if (action == HoldAction.RESERVE) it.copy(asset = it.child("asset")) else it }
            val asset = assetRecord().toMutableMap().apply {
                if (action == HoldAction.RESERVE) { put("id", c.child("asset")); put("upload_operation_id", c.child("media-upload")) }
            }
            val db = JdbcBoundary().rows(receipt(c)).rows(asset)
            if (previous == null) db.rows() else db.rows(record(previous))
            db.rows(record(if (action == HoldAction.RESERVE) "RESERVED" else state))
            val rpc = HoldRpc(db.source, CatalogAttachments(CatalogBoundary(remote(state).toBuilder().setAssetId(c.asset.toString()).build()).client), Clock.fixed(instant, ZoneOffset.UTC))
            val response = Response<Attachment>()
            MediaPeer.context("catalog").run {
                when (action) { HoldAction.RESERVE -> rpc.reserve(request(c), response); HoldAction.CONFIRM -> rpc.confirm(request(c), response); HoldAction.RELEASE -> rpc.release(request(c), response) }
            }
            assertNull(response.failure, "$action: ${response.failure}")
            assertTrue(response.completed)
            assertEquals(holdId.toString(), response.values.single().holdId)
            assertEquals(actorId.toString(), response.values.single().actorId)
            assertEquals(1, db.commits)
            assertEquals(0, db.rollbacks)
        }
    }

    @Test fun `untrusted requests do not expose holds`() {
        for (call in listOf<(HoldRpc, Hold, Response<Attachment>) -> Unit>({ rpc, req, res -> rpc.reserve(req, res) },
            { rpc, req, res -> rpc.confirm(req, res) }, { rpc, req, res -> rpc.release(req, res) })) {
            val response = Response<Attachment>()
            call(HoldRpc(JdbcBoundary().source), request(command()), response)
            assertEquals(Status.Code.PERMISSION_DENIED, response.code)
        }
    }
}
