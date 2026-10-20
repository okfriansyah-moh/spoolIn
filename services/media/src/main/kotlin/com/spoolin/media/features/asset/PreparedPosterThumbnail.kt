package com.spoolin.media.features.asset

import com.spoolin.media.features.processing.CodecSandbox
import com.google.protobuf.ByteString
import io.grpc.Status
import spoolin.v1.Spoolin
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.TimeUnit

internal object PreparedPosterThumbnail {
    fun render(output: Path,row: AssetRow): Spoolin.MoviePosterImage {
        val extension = when (row.format) { "JPEG" -> "jpeg"; "PNG" -> "png"; "WEBP" -> "webp"; else -> throw Status.UNAVAILABLE.asRuntimeException() }
        val scratch = Files.createDirectory(output.resolve(UUID.randomUUID().toString()))
        try {
            val error = CodecSandbox().execute(output.resolve("${row.id}.$extension"),scratch,
                listOf("/usr/local/bin/vips","thumbnail","/input","/output/encoded.jpeg[Q=80,strip=true]","320","--height","320","--size","down","--vips-concurrency","1","--vips-cache-max","0"),
                System.nanoTime()+TimeUnit.SECONDS.toNanos(10))
            if (error != null) throw Status.UNAVAILABLE.withDescription("POSTER_PREVIEW_UNAVAILABLE").asRuntimeException()
            val file = scratch.resolve("encoded.jpeg")
            require(Files.size(file) in 1..1_048_576)
            val bytes = Files.readAllBytes(file)
            val image = javax.imageio.ImageIO.read(java.io.ByteArrayInputStream(bytes)) ?: throw Status.UNAVAILABLE.asRuntimeException()
            require(image.width in 1..320 && image.height in 1..320)
            val poster = Spoolin.PreparedPoster.newBuilder().setAssetId(row.id.toString()).setMediaType("image/jpeg").setWidth(image.width).setHeight(image.height).setState("READY")
                .setContentHash(java.util.Base64.getEncoder().encodeToString(requireNotNull(row.contentHash)))
            return Spoolin.MoviePosterImage.newBuilder().setPoster(poster).setImageBytes(ByteString.copyFrom(bytes)).build()
        } finally {
            Files.walk(scratch).use { files -> files.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) } }
        }
    }
}
