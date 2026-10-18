package com.spoolin.core.features.connector

import java.security.MessageDigest

private const val TOKEN_HASH_BYTES = 32

object ConnectorTokenDigest {
    fun sha256(rawToken: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(rawToken).also { require(it.size == TOKEN_HASH_BYTES) }
}
