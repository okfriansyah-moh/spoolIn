package com.spoolin.media.features.processing

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class ImageEnvelopeRulesTest {
    @Test
    fun `actual static PNG and JPEG reject appended payloads`() {
        listOf("png" to ImageFormat.PNG, "jpeg" to ImageFormat.JPEG).forEach { (extension, format) ->
            val encoded = ByteArrayOutputStream()
            ImageIO.write(BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), extension, encoded)
            val bytes = encoded.toByteArray()
            assertTrue(ImageEnvelope.complete(bytes, format))
            assertFalse(ImageEnvelope.complete(bytes + "<script>".toByteArray(), format))
            assertFalse(ImageEnvelope.complete(bytes.dropLast(1).toByteArray(), format))
        }
    }

    @Test
    fun `WebP RIFF length must match the exact received bytes`() {
        val bytes = "RIFF".toByteArray() + byteArrayOf(4, 0, 0, 0) + "WEBP".toByteArray()
        assertTrue(ImageEnvelope.complete(bytes, ImageFormat.WEBP))
        assertFalse(ImageEnvelope.complete(bytes + byteArrayOf(0), ImageFormat.WEBP))
    }
}
