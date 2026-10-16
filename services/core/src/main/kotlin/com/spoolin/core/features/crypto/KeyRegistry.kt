package com.spoolin.core.features.crypto

import java.sql.Connection
import javax.sql.DataSource

class RegisteredKey(
    val id: String,
    val purpose: KeyPurpose,
    val state: KeyState = KeyState.READ,
)

class KeyRegistry(
    private val dataSource: DataSource,
) {
    fun writeKey(purpose: KeyPurpose): RegisteredKey =
        dataSource.readRows { connection ->
            connection
                .queryBound(
                    "SELECT id FROM appdata.crypto_key_version WHERE purpose=? AND state='WRITE'",
                    listOf(purpose.name),
                ) {
                    RegisteredKey(it.getString("id"), purpose, KeyState.WRITE)
                }.singleOrNull() ?: throw ProtectionUnavailable()
        }

    fun readable(
        id: String,
        purpose: KeyPurpose,
    ): Boolean =
        dataSource.readRows { connection ->
            connection
                .queryBound(
                    "SELECT id FROM appdata.crypto_key_version WHERE id=? AND purpose=? AND state IN ('WRITE','READ')",
                    listOf(id, purpose.name),
                ) { it.getString("id") }
                .isNotEmpty()
        }

    fun lookupKeys(connection: Connection): List<RegisteredKey> =
        connection
            .queryBound(
                "SELECT id,state FROM appdata.crypto_key_version WHERE purpose='EMAIL_LOOKUP' AND state IN " +
                    "('WRITE','READ') ORDER BY id FOR SHARE",
            ) { RegisteredKey(it.getString("id"), KeyPurpose.EMAIL_LOOKUP, KeyState.valueOf(it.getString("state"))) }
            .also { if (it.isEmpty()) throw ProtectionUnavailable() }
}
