package com.spoolin.core.boundary

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.security.MessageDigest
import java.util.Base64

class FrontendPolicyRulesTest {
    @Test
    fun `packaged bootstrap receives only its exact script hash`() {
        val script = "\nstart('/app.js');\n"
        val html = "<script>$script</script><script src=\"/external.js\"></script>"
        val policy = FrontendPolicy.forHtml(html.toByteArray())
        val expected =
            Base64.getEncoder().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(script.toByteArray()),
            )
        assertTrue(policy.contains("'sha256-$expected'"))
        assertTrue(policy.contains("object-src 'none'; frame-ancestors 'none'"))
        assertFalse(policy.contains("unsafe-inline"))
        assertFalse(policy.contains("unsafe-eval"))
        assertFalse(FrontendPolicy.forHtml("<h1>Empty</h1>".toByteArray()).contains("sha256-"))
    }
}
