package com.spoolin.management.alerts

import com.spoolin.core.features.alerts.AlertCapacity
import com.spoolin.core.features.alerts.AlertPolicy
import com.spoolin.core.features.alerts.AlertRecorder
import com.spoolin.core.features.alerts.AlertWebhook
import com.spoolin.core.features.alerts.InvalidAlert
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import java.security.cert.X509Certificate
import java.sql.SQLException
import java.util.concurrent.Semaphore

private const val MAXIMUM_ALERT_BYTES = 65536
private const val SAN_ENTRY_MINIMUM_SIZE = 2
private const val DNS_SAN_TYPE = 2

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
        return rejection(request) ?: record(request)
    }

    private fun rejection(request: HttpServletRequest): ResponseEntity<Map<String, String>>? =
        when {
            !authorized(request) -> response(HttpStatus.FORBIDDEN.value(), "FORBIDDEN")
            request.contentLengthLong > MAXIMUM_ALERT_BYTES ->
                response(HttpStatus.PAYLOAD_TOO_LARGE.value(), "PAYLOAD_TOO_LARGE")
            request.getHeader("Content-Encoding")?.let { it != "identity" } == true ->
                response(HttpStatus.BAD_REQUEST.value(), "INVALID_ALERT")
            !slots.tryAcquire() -> response(HttpStatus.SERVICE_UNAVAILABLE.value(), "UNAVAILABLE")
            else -> null
        }

    private fun record(request: HttpServletRequest): ResponseEntity<Map<String, String>> {
        return try {
            val episodes = webhook.parse(request.inputStream.readNBytes(MAXIMUM_ALERT_BYTES + 1))
            recorder.recordBatch(episodes, policy)
            response(HttpStatus.ACCEPTED.value(), "RECORDED")
        } catch (
            _: AlertCapacity,
        ) {
            response(HttpStatus.PAYLOAD_TOO_LARGE.value(), "PAYLOAD_TOO_LARGE")
        } catch (
            _: InvalidAlert,
        ) {
            response(HttpStatus.BAD_REQUEST.value(), "INVALID_ALERT")
        } catch (
            _: SQLException,
        ) {
            response(HttpStatus.SERVICE_UNAVAILABLE.value(), "UNAVAILABLE")
        } catch (_: java.io.IOException) {
            response(HttpStatus.BAD_REQUEST.value(), "INVALID_ALERT")
        } finally {
            slots.release()
        }
    }

    private fun authorized(request: HttpServletRequest): Boolean {
        val port = environment.getProperty("local.management.port")?.toIntOrNull()
        return request.isSecure && request.localPort == port && certificateNames(request) == listOf("alertmanager")
    }

    private fun certificateNames(request: HttpServletRequest): List<Any?> {
        val chain = request.getAttribute("jakarta.servlet.request.X509Certificate") as? Array<*>
        val leaf = chain?.firstOrNull() as? X509Certificate
        return leaf?.subjectAlternativeNames.orEmpty()
            .filter { it.size >= SAN_ENTRY_MINIMUM_SIZE && it[0] == DNS_SAN_TYPE }
            .map { it[1] }
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
