package com.spoolin.core.features.crypto

import javax.sql.DataSource

class ProtectionUnavailable : IllegalStateException("Protected data is unavailable")

class ReservedNonce internal constructor(
    val keyId: String,
    private val counter: Long,
) {
    fun bytes(): ByteArray = NonceCounter.encode(counter.toString())
}

class NonceAllocator(
    private val dataSource: DataSource,
) {
    fun reserve(
        keyId: String,
        purpose: KeyPurpose,
    ): ReservedNonce {
        val counter =
            dataSource.committed { connection ->
                connection
                    .queryBound(
                        "UPDATE appdata.crypto_key_version SET next_nonce=next_nonce+1 " +
                            "WHERE id=? AND purpose=? AND state='WRITE' AND next_nonce<9223372036854775807 RETURNING " +
                            "next_nonce-1 AS allocated",
                        listOf(keyId, purpose.name),
                    ) { it.getLong("allocated") }
                    .singleOrNull() ?: throw ProtectionUnavailable()
            }
        return ReservedNonce(keyId, counter)
    }
}
