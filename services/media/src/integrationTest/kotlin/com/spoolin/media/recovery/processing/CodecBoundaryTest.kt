package com.spoolin.media.recovery.processing

import com.spoolin.media.features.processing.ImageFormat
import com.spoolin.media.features.processing.ProcessorRunner
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import javax.imageio.ImageIO

/** Requires the pinned Media codec filesystem and Linux namespace sandbox on the remote runner. */
class CodecBoundaryTest {
    @TempDir lateinit var directory: Path

    @Test
    fun `real sandbox decodes reencodes strips and reconciles immutable output`() {
        val source = directory.resolve(UUID.randomUUID().toString())
        val output = Files.createDirectory(directory.resolve("output"))
        ImageIO.write(BufferedImage(12, 7, BufferedImage.TYPE_INT_RGB), "png", source.toFile())
        val id = UUID.randomUUID()
        val first = ProcessorRunner().process(source, output, ImageFormat.PNG, id)
        assertTrue(first is ProcessorRunner.Outcome.Success, first.toString())
        first as ProcessorRunner.Outcome.Success
        assertEquals(12, first.width)
        assertEquals(7, first.height)
        assertEquals("$id.png", first.outputPath.fileName.toString())
        val before = Files.readAllBytes(first.outputPath)
        val replay = ProcessorRunner().process(source, output, ImageFormat.PNG, id)
        assertTrue(replay is ProcessorRunner.Outcome.Success)
        assertArrayEquals(before, Files.readAllBytes(first.outputPath))
        assertArrayEquals(first.contentHash, (replay as ProcessorRunner.Outcome.Success).contentHash)
        Files.list(output).use { paths -> assertEquals(1, paths.count()) }
    }

    @Test
    fun `oversized decoded dimensions fail before publishing any output`() {
        val source = directory.resolve(UUID.randomUUID().toString())
        val output = Files.createDirectory(directory.resolve("output"))
        ImageIO.write(BufferedImage(8193, 1, BufferedImage.TYPE_INT_RGB), "png", source.toFile())
        val result = runCatching { ProcessorRunner().process(source, output, ImageFormat.PNG) }
        assertFalse(result.getOrNull() is ProcessorRunner.Outcome.Success)
        Files.list(output).use { paths -> assertEquals(0, paths.count()) }
    }
}
