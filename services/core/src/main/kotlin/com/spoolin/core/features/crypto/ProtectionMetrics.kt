package com.spoolin.core.features.crypto

import io.micrometer.core.instrument.MeterRegistry
import java.security.GeneralSecurityException
import java.sql.SQLException
import java.util.UUID

/** Fixed failure classes from the protected-data metric contract; no account or key labels. */
enum class ProtectionFailure { AUTHENTICATION, KEY, UNAVAILABLE, NONCE_EXHAUSTION }

class ProtectionMetrics(
    private val registry: MeterRegistry,
) {
    fun observing(delegate: ProtectionAlert): ProtectionAlert =
        object : ProtectionAlert {
            override fun unavailable(rowId: UUID) = delegate.unavailable(rowId)

            override fun failed(code: ProtectionFailure) {
                registry.counter("spoolin_crypto_failures_total", "service", "core", "code", code.name).increment()
            }
        }
}

internal fun ProtectionAlert.recordFailure(code: ProtectionFailure) {
    // Diagnostic failure must never replace the protected-data result or its original exception.
    runCatching { failed(code) }
}

internal fun <T> observeProtection(
    alert: ProtectionAlert,
    code: ProtectionFailure,
    operation: () -> T,
): T =
    try {
        operation()
    } catch (failure: ProtectionUnavailable) {
        alert.recordFailure(if (failure.reason == ProtectionFailure.NONCE_EXHAUSTION) failure.reason else code)
        throw failure
    } catch (failure: SQLException) {
        alert.recordFailure(ProtectionFailure.UNAVAILABLE)
        throw failure
    } catch (failure: GeneralSecurityException) {
        alert.recordFailure(ProtectionFailure.UNAVAILABLE)
        throw failure
    }

internal fun decryptProtected(
    alert: ProtectionAlert,
    bytes: ByteArray?,
    decrypt: (ByteArray) -> ProtectedRead<ByteArray>,
): ProtectedRead<ByteArray> {
    if (bytes == null) {
        alert.recordFailure(ProtectionFailure.KEY)
        return ProtectedRead.Unavailable
    }
    val result =
        try {
            decrypt(bytes)
        } finally {
            bytes.fill(0)
        }
    return result
}
