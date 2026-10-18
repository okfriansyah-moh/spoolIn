package com.spoolin.core.features.connector

import java.security.MessageDigest

object ConnectorTokenDigest {
    fun sha256(rawToken: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(rawToken).also { require(it.size == 32) }
}
