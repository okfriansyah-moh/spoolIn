package com.spoolin.core.boundary.http

import com.spoolin.support.observability.LogicalResult
import jdk.jfr.Recording
import jdk.jfr.consumer.RecordingFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class CoreRequestTimingRulesTest {
    @TempDir lateinit var directory: Path

    @Test
    fun `recording keeps exact monotonic completion and fixed fields without unknown operation text`() {
        CoreRequestTiming.beginIfEnabled()
        val output = directory.resolve("timing.jfr")
        Recording().use { recording ->
            recording.enable("spoolin.CoreRequestTiming").withoutStackTrace()
            recording.start()
            val started = System.nanoTime()
            checkNotNull(CoreRequestTiming.beginIfEnabled()).complete("title", LogicalResult.success(), started)
            val ignored = checkNotNull(CoreRequestTiming.beginIfEnabled())
            ignored.complete("private-operation-canary", LogicalResult.success(), started)
            recording.stop()
            recording.dump(output)
        }
        val events = RecordingFile.readAllEvents(output)
        assertEquals(1, events.size)
        val event = events.single()
        assertEquals("spoolin.CoreRequestTiming", event.eventType.name)
        assertEquals("title", event.getString("operation"))
        assertEquals("SUCCESS", event.getString("outcome"))
        assertEquals("OK", event.getString("code"))
        assertTrue(event.getLong("durationNanos") >= 0)
        assertFalse(event.hasField("request"))
        assertFalse(event.hasField("accountId"))
        assertEquals(null, event.stackTrace)
    }
}
