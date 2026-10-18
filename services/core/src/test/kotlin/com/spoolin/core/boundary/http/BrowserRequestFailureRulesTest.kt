package com.spoolin.core.boundary.http

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class BrowserRequestFailureRulesTest {
    @Test
    fun `checked and runtime action failures finish the deferred request with a safe unavailable response`() {
        val executor = BrowserRequests()
        try {
            val failures =
                listOf(IOException("synthetic private IO detail"), IllegalStateException("synthetic private state"))
            for (actionFailure in failures) {
                val received = AtomicReference<Any>()
                val completed = CountDownLatch(1)
                val response = executor.run { throw actionFailure }
                response.setResultHandler {
                    received.set(it)
                    completed.countDown()
                }
                assertTrue(completed.await(5, TimeUnit.SECONDS))
                val actual = assertInstanceOf(ResponseEntity::class.java, received.get())
                assertEquals(BrowserBoundary.UNAVAILABLE, actual.statusCode.value())
                assertEquals(BrowserResponses.failure(BrowserBoundary.UNAVAILABLE, "UNAVAILABLE").body, actual.body)
                assertFalse(actual.body.toString().contains("private"))
            }
        } finally {
            executor.close()
        }
    }
}
