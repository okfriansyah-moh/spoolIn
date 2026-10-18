package com.spoolin.media.features.hold

import com.spoolin.media.boundary.grpc.MediaPeer
import com.spoolin.media.features.asset.AssetRow
import com.spoolin.media.features.asset.CatalogAttachments
import io.grpc.Status
import spoolin.v1.Spoolin.Attachment

internal class HoldAuthority(private val catalog: CatalogAttachments?) {
    fun verify(command: HoldCommand): Attachment {
        if (MediaPeer.current() != "catalog") throw Status.PERMISSION_DENIED.asRuntimeException()
        val authority = catalog ?: throw Status.UNAVAILABLE.withDescription("ATTACHMENT_AUTHORITY_UNAVAILABLE")
            .asRuntimeException()
        val current = authority.find(command.hold)
        val exact = current.holdId == command.hold.toString() && current.assetId == command.asset.toString() &&
            current.titleId == command.title.toString() && current.hasActorId() && current.actorId == command.actor.toString()
        if (!exact) throw Status.PERMISSION_DENIED.withDescription("HOLD_TUPLE_INVALID").asRuntimeException()
        if (current.state !in setOf("PREPARING", "ACTIVE", "RELEASED")) throw Status.UNAVAILABLE.asRuntimeException()
        if (!current.hasCatalogVersion() || current.catalogVersion < 0) throw Status.UNAVAILABLE.asRuntimeException()
        return current
    }

    fun ownedUpload(asset: AssetRow, command: HoldCommand) {
        if (asset.actorId != command.actor) throw Status.PERMISSION_DENIED.asRuntimeException()
        if (command.action == HoldAction.RESERVE &&
            (asset.uploadOperationId != command.child("media-upload") || asset.id != command.child("asset"))) {
            throw Status.PERMISSION_DENIED.withDescription("HOLD_UPLOAD_BINDING_INVALID").asRuntimeException()
        }
    }

    fun transition(command: HoldCommand, current: Attachment, local: HoldRecord?) {
        val permitted = when (command.action) {
            HoldAction.RESERVE -> current.state == "PREPARING" &&
                (command.expectedVersion == null || command.expectedVersion == current.catalogVersion)
            HoldAction.CONFIRM -> current.state == "ACTIVE" && version(command, current, local)
            HoldAction.RELEASE -> current.state == "RELEASED" && version(command, current, local)
        }
        if (!permitted) throw Status.FAILED_PRECONDITION.withDescription("HOLD_AUTHORITY_CHANGED").asRuntimeException()
    }

    private fun version(command: HoldCommand, current: Attachment, local: HoldRecord?): Boolean {
        val requested = command.version ?: return false
        return requested > 0 && requested <= current.catalogVersion && requested >= (local?.version ?: 0)
    }
}
