package com.spoolin.core.boundary

import java.security.MessageDigest
import java.util.Base64

object FrontendPolicy {
    fun forHtml(bytes: ByteArray): String {
        val hashes =
            Regex("<script(?:\\s[^>]*)?>([\\s\\S]*?)</script>")
                .findAll(bytes.toString(Charsets.UTF_8))
                .map { it.groupValues[1] }
                .filter { it.isNotBlank() }
                .map { script ->
                    val hash = MessageDigest.getInstance("SHA-256").digest(script.toByteArray(Charsets.UTF_8))
                    "'sha256-${Base64.getEncoder().encodeToString(hash)}'"
                }.distinct()
                .joinToString(" ")
        return "default-src 'self'; script-src 'self' $hashes; object-src 'none'; frame-ancestors 'none'"
    }
}
