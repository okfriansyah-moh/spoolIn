package com.spoolin.media.features.hold

import com.spoolin.media.boundary.grpc.MediaPeer
import com.spoolin.media.features.asset.AssetSql
import com.spoolin.media.features.asset.AssetState
import com.spoolin.media.features.asset.CatalogAttachments
import com.spoolin.support.receipts.committed
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import spoolin.v1.Spoolin.Attachment
import spoolin.v1.Spoolin.Hold
import java.sql.Connection
import java.time.Clock
import javax.sql.DataSource

/** Catalog owns current intent and actor checks; Media independently verifies its live persisted tuple. */
class HoldRpc(
    private val dataSource: DataSource,
    catalog: CatalogAttachments? = null,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val authority = HoldAuthority(catalog)

    fun reserve(request: Hold, response: StreamObserver<Attachment>) = execute(request, response, HoldAction.RESERVE)
    fun confirm(request: Hold, response: StreamObserver<Attachment>) = execute(request, response, HoldAction.CONFIRM)
    fun release(request: Hold, response: StreamObserver<Attachment>) = execute(request, response, HoldAction.RELEASE)

    private fun execute(request: Hold, response: StreamObserver<Attachment>, action: HoldAction) {
        try {
            if (MediaPeer.current() != "catalog") throw Status.PERMISSION_DENIED.asRuntimeException()
            val command = HoldCommand.parse(request, action)
            val current = authority.verify(command)
            val result = perform(command, current)
            response.onNext(projection(result, command))
            response.onCompleted()
        } catch (failure: StatusRuntimeException) { response.onError(failure) }
        catch (_: java.sql.SQLException) { unavailable(response) }
        catch (_: IllegalStateException) { unavailable(response) }
        catch (_: IllegalArgumentException) { unavailable(response) }
    }

    private fun unavailable(response: StreamObserver<Attachment>) = response.onError(
        Status.UNAVAILABLE.withDescription("HOLD_UNAVAILABLE").asRuntimeException(),
    )

    private fun perform(command: HoldCommand, current: Attachment): HoldRecord = dataSource.committed { connection ->
        val first = HoldSql.reserveReceipt(connection, command, clock.instant())
        val asset = AssetSql.findLocked(connection, command.asset) ?: throw Status.NOT_FOUND.asRuntimeException()
        authority.ownedUpload(asset, command)
        val existing = HoldSql.lock(connection, command.hold)
        verifyExisting(command, existing)
        if (!first) return@committed requireNotNull(existing)
        authority.transition(command, current, existing)
        if (command.action != HoldAction.RELEASE && asset.state != AssetState.READY) {
            throw Status.FAILED_PRECONDITION.withDescription("ASSET_NOT_READY").asRuntimeException()
        }
        mutate(connection, command, existing)
        val committed = requireNotNull(HoldSql.lock(connection, command.hold))
        HoldSql.completeReceipt(connection, command, committed)
        committed
    }

    private fun mutate(connection: Connection, command: HoldCommand, existing: HoldRecord?) {
        val updated = when (command.action) {
            HoldAction.RESERVE -> reserveHold(connection, command, existing)
            HoldAction.CONFIRM -> confirmHold(connection, command, existing)
            HoldAction.RELEASE -> releaseHold(connection, command, existing)
        }
        if (updated != 1) throw Status.FAILED_PRECONDITION.withDescription("HOLD_STATE_CONFLICT").asRuntimeException()
    }

    private fun projection(hold: HoldRecord, command: HoldCommand): Attachment = Attachment.newBuilder()
        .setHoldId(hold.id.toString()).setState(hold.state).setAssetId(hold.asset.toString())
        .setTitleId(hold.title.toString()).setActorId(command.actor.toString())
        .also { builder -> hold.version?.let { builder.catalogVersion = it } }.build()
}

private fun verifyExisting(command: HoldCommand, existing: HoldRecord?) {
    if (existing != null && (existing.asset != command.asset || existing.title != command.title)) {
        throw Status.ALREADY_EXISTS.withDescription("HOLD_TUPLE_CONFLICT").asRuntimeException()
    }
}

private fun reserveHold(connection: Connection, command: HoldCommand, existing: HoldRecord?): Int =
    if (existing == null) HoldSql.insertReserved(connection, command.hold, command.asset, command.title)
    else if (existing.state == "RESERVED") 1 else 0

private fun confirmHold(connection: Connection, command: HoldCommand, existing: HoldRecord?): Int =
    when (existing?.state) {
    "RESERVED" -> HoldSql.confirmActive(connection, command.hold, requireNotNull(command.version))
    "ACTIVE" -> if (existing.version == command.version) 1 else 0
    else -> 0
}

private fun releaseHold(connection: Connection, command: HoldCommand, existing: HoldRecord?): Int =
    when (existing?.state) {
    "RESERVED", "ACTIVE" -> HoldSql.releaseHold(connection, command.hold, requireNotNull(command.version))
    "RELEASED" -> 1
    else -> 0
}
