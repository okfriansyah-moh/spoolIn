package com.spoolin.core.boundary.http

import com.spoolin.core.features.artworkproxy.ArtworkReadProxy
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.ObjectProvider
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.nio.file.Files
import java.util.UUID

@RestController
class ArtworkServing(private val artwork: ArtworkReadProxy, private val identity: ObjectProvider<BrowserIdentity>) {
    @GetMapping("/artwork/{assetId}")
    fun get(
        @PathVariable assetId: String,
        @RequestParam(defaultValue = "false") management: Boolean,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        response.setHeader("Cache-Control", "no-store")
        response.setHeader("X-Content-Type-Options", "nosniff")
        try {
            val id = UUID.fromString(assetId).also { require(it.toString() == assetId) }
            val identities = identity.ifAvailable ?: throw io.grpc.Status.UNAVAILABLE.asRuntimeException()
            val context = identities.context(request, false, false, bootstrap = false)
            artwork.read(id, management, context).use { asset ->
                response.contentType = asset.contentType
                response.setContentLengthLong(asset.bytes)
                Files.newInputStream(asset.path).use { it.copyTo(response.outputStream, 65536) }
            }
        } catch (failure: io.grpc.StatusRuntimeException) {
            if (!response.isCommitted) response.sendError(if (failure.status.code in setOf(io.grpc.Status.Code.NOT_FOUND, io.grpc.Status.Code.PERMISSION_DENIED)) 404 else 503)
        } catch (_: IllegalArgumentException) {
            if (!response.isCommitted) response.sendError(404)
        }
    }
}
