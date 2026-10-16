package com.spoolin.core.boundary.http

import jakarta.servlet.http.HttpServletRequest
import java.net.URI

class BrowserRejected(
    val status: Int,
) : IllegalArgumentException("Browser request was rejected")

class BrowserBoundary(
    origin: String,
) {
    private val expected = URI.create(origin)
    private val authority = expected.rawAuthority

    init {
        require(expected.scheme == "https" && expected.host in setOf("localhost", "127.0.0.1", "[::1]"))
        require(expected.rawUserInfo == null && expected.rawPath.isEmpty())
        require(expected.rawQuery == null && expected.rawFragment == null)
    }

    fun check(request: HttpServletRequest) {
        if (!request.isSecure || !exact(request, "Host", authority) || !exact(request, "Origin", expected.toString())) {
            throw BrowserRejected(FORBIDDEN)
        }
        if (!exact(request, "X-Spoolin-Client", "web")) reject(FORBIDDEN)
        if (request.method != "POST" || request.queryString != null) reject(BAD_REQUEST)
        val content = request.contentType?.substringBefore(';')?.lowercase(java.util.Locale.ROOT)
        if (content != "application/json") reject(BAD_REQUEST)
    }

    private fun reject(status: Int): Nothing = throw BrowserRejected(status)

    private fun exact(
        request: HttpServletRequest,
        name: String,
        expectedValue: String,
    ): Boolean {
        val values = request.getHeaders(name).toList()
        return values.size == 1 && values.single() == expectedValue
    }

    companion object {
        const val BAD_REQUEST = 400
        const val FORBIDDEN = 403
        const val TOO_LARGE = 413
        const val UNAVAILABLE = 503
    }
}
