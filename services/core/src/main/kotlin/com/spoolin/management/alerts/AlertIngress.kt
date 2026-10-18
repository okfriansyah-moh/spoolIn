package com.spoolin.management.alerts

import com.spoolin.core.features.alerts.AlertCapacity
import com.spoolin.core.features.alerts.AlertPolicy
import com.spoolin.core.features.alerts.AlertRecorder
import com.spoolin.core.features.alerts.AlertWebhook
import com.spoolin.core.features.alerts.InvalidAlert
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.env.Environment
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import java.security.cert.X509Certificate
import java.sql.SQLException
import java.util.concurrent.Semaphore

@RestController
class AlertIngress(
    private val environment: Environment,
    private val recorder: AlertRecorder,
    private val policy: AlertPolicy,
) {
    private val webhook = AlertWebhook(policy)
    private val slots = Semaphore(2)

    @PostMapping("/internal/alerts", consumes = ["application/json"])
    fun accept(request: HttpServletRequest): ResponseEntity<Map<String, String>> {
        if (!authorized(request)) return response(403, "FORBIDDEN")
        if (request.contentLengthLong > 65536) return response(413, "PAYLOAD_TOO_LARGE")
        if (request.getHeader("Content-Encoding")?.let { it != "identity" } == true) return response(400, "INVALID_ALERT")
        if (!slots.tryAcquire()) return response(503, "UNAVAILABLE")
        return try {
            val episodes = webhook.parse(request.inputStream.readNBytes(65537))
            recorder.recordBatch(episodes, policy)
            response(202, "RECORDED")
        } catch (
            _: AlertCapacity,
        ) {
            response(413, "PAYLOAD_TOO_LARGE")
        } catch (
            _: InvalidAlert,
        ) {
            response(400, "INVALID_ALERT")
        } catch (
            _: SQLException,
        ) {
            response(503, "UNAVAILABLE")
        } catch (_: java.io.IOException) {
            response(400, "INVALID_ALERT")
        } finally {
            slots.release()
        }
    }

    private fun authorized(request: HttpServletRequest): Boolean {
        val port = environment.getProperty("local.management.port")?.toIntOrNull() ?: return false
        if (!request.isSecure || request.localPort != port) return false
        val chain = request.getAttribute("jakarta.servlet.request.X509Certificate") as? Array<*> ?: return false
        val leaf = chain.firstOrNull() as? X509Certificate ?: return false
        val names =
            leaf.subjectAlternativeNames
                .orEmpty()
                .filter { it.size >= 2 && it[0] == 2 }
                .map { it[1] }
        return names == listOf("alertmanager")
    }

    private fun response(
        status: Int,
        code: String,
    ): ResponseEntity<Map<String, String>> =
        ResponseEntity
            .status(status)
            .header("Cache-Control", "no-store")
            .header("X-Content-Type-Options", "nosniff")
            .body(mapOf("status" to code))
}
