package com.spoolin.core.features.crypto.rotation

import com.spoolin.core.features.crypto.AeadCipher
import com.spoolin.core.features.crypto.KeyMount
import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.features.crypto.MountedKeys
import com.spoolin.core.features.crypto.ProtectedRead
import com.spoolin.core.features.crypto.ProtectionBinding
import com.spoolin.core.features.crypto.committed
import com.spoolin.core.features.crypto.queryBound
import com.spoolin.core.requireProtectedFile
import tools.jackson.core.StreamReadFeature
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import javax.sql.DataSource

object RotationKeys {
    fun load(file: Path): MountedKeys {
        requireProtectedFile(file, 16384)
        val root =
            JsonMapper
                .builder()
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .build()
                .readTree(Files.readAllBytes(file))
        require(root.isObject && root.size() in 2..64) { "Rotation key configuration is invalid" }
        return MountedKeys(
            root.properties().associate { entry ->
                val node = entry.value
                require(node.isObject && node.properties().map { it.key }.toSet() == setOf("purpose", "file"))
                val path = Path.of(node["file"].asString()).also { require(it.isAbsolute) }
                entry.key to KeyMount(KeyPurpose.valueOf(node["purpose"].asString()), path)
            },
        )
    }

    fun registerTarget(
        dataSource: DataSource,
        keys: MountedKeys,
        id: String,
        purpose: KeyPurpose,
    ) {
        require(id.matches(Regex("[A-Za-z0-9_-]{1,64}")))
        require(keys.key(id, purpose)?.also { it.fill(0) } != null) { "Target key material is unavailable" }
        dataSource.committed { connection ->
            connection
                .prepareStatement(
                    "INSERT INTO appdata.crypto_key_version(id,purpose,state) VALUES(?,?,'READ') ON CONFLICT(id) DO NOTHING",
                ).use {
                    it.setString(1, id)
                    it.setString(2, purpose.name)
                    it.executeUpdate()
                }
            val registered =
                connection
                    .queryBound("SELECT purpose,state FROM appdata.crypto_key_version WHERE id=? FOR UPDATE", listOf(id)) {
                        it.getString("purpose") to it.getString("state")
                    }.single()
            require(
                registered.first == purpose.name && registered.second in setOf("READ", "WRITE"),
            ) { "Target key registration is incompatible" }
        }
    }

    fun validateSource(
        dataSource: DataSource,
        keys: MountedKeys,
        id: String,
        purpose: KeyPurpose,
    ) {
        require(keys.key(id, purpose)?.also { it.fill(0) } != null) { "Source key material is unavailable" }
        val table = if (purpose == KeyPurpose.DRAFT) "form_draft" else "user_account"
        val prefix = if (purpose == KeyPurpose.DRAFT) "body" else "pii"
        val source =
            dataSource.connection.use { connection ->
                val from =
                    if (purpose ==
                        KeyPurpose.EMAIL_LOOKUP
                    ) {
                        "appdata.user_account r JOIN appdata.account_email_lookup e ON e.account_id=r.id"
                    } else {
                        "appdata.$table r"
                    }
                val predicate = if (purpose == KeyPurpose.EMAIL_LOOKUP) "e.key_id=?" else "r.${prefix}_key_id=?"
                val digest = if (purpose == KeyPurpose.EMAIL_LOOKUP) "e.digest" else "NULL::bytea"
                connection
                    .queryBound(
                        "SELECT r.id,r.${prefix}_key_id AS key_id,r.${prefix}_nonce AS nonce,r.${prefix}_cipher AS cipher,$digest AS digest FROM $from WHERE $predicate ORDER BY r.id LIMIT 1",
                        listOf(id),
                    ) {
                        Source(
                            it.getObject("id", UUID::class.java),
                            com.spoolin.core.features.crypto.ProtectedEnvelope(
                                1,
                                it.getString("key_id"),
                                it.getBytes("nonce"),
                                it.getBytes("cipher"),
                            ),
                            it.getBytes("digest"),
                        )
                    }.singleOrNull()
            } ?: return
        val encryptionPurpose = if (purpose == KeyPurpose.EMAIL_LOOKUP) KeyPurpose.PII else purpose
        val material = keys.key(source.envelope.keyId, encryptionPurpose) ?: error("Canary decryption key is unavailable")
        try {
            val read = AeadCipher().decrypt(source.envelope, material, ProtectionBinding("core", table, source.id, encryptionPurpose))
            check(read is ProtectedRead.Value) { "Existing protected canary failed authentication" }
            try {
                if (purpose == KeyPurpose.EMAIL_LOOKUP) verifyAlias(keys, id, source, read.value)
            } finally {
                read.value.fill(0)
            }
        } finally {
            material.fill(0)
        }
    }

    private fun verifyAlias(
        keys: MountedKeys,
        id: String,
        source: Source,
        clear: ByteArray,
    ) {
        val identity =
            com.spoolin.core.features.account.persistence
                .AccountPayload()
                .decode(clear)
        check(identity is ProtectedRead.Value) { "Lookup canary identity is invalid" }
        val material = keys.key(id, KeyPurpose.EMAIL_LOOKUP) ?: error("Lookup key is unavailable")
        try {
            val mac = javax.crypto.Mac.getInstance("HmacSHA256")
            mac.init(javax.crypto.spec.SecretKeySpec(material, "HmacSHA256"))
            val expected = mac.doFinal(("email-lookup-v1\u0000" + identity.value.emailNormalized).toByteArray(Charsets.UTF_8))
            try {
                check(java.security.MessageDigest.isEqual(expected, source.digest)) { "Lookup canary does not match existing aliases" }
            } finally {
                expected.fill(0)
            }
        } finally {
            material.fill(0)
        }
    }

    private data class Source(
        val id: UUID,
        val envelope: com.spoolin.core.features.crypto.ProtectedEnvelope,
        val digest: ByteArray?,
    )

    fun validate(
        dataSource: DataSource,
        keys: MountedKeys,
        id: String,
        purpose: KeyPurpose,
    ) {
        val material = keys.key(id, purpose) ?: error("Required protected rotation key is unavailable")
        try {
            if (purpose == KeyPurpose.EMAIL_LOOKUP) {
                val mac = javax.crypto.Mac.getInstance("HmacSHA256")
                mac.init(javax.crypto.spec.SecretKeySpec(material, "HmacSHA256"))
                check(mac.doFinal("rotation-canary-v1".toByteArray()).size == 32)
                return
            }
            // Canary allocations are committed and advance the same counter before READ-key promotion.
            val counter =
                dataSource.committed { connection ->
                    connection
                        .queryBound(
                            "UPDATE appdata.crypto_key_version SET next_nonce=next_nonce+1 WHERE id=? AND purpose=? " +
                                "AND state IN ('WRITE','READ') AND next_nonce<9223372036854775807 RETURNING next_nonce-1 AS nonce",
                            listOf(id, purpose.name),
                        ) { it.getLong("nonce") }
                        .single()
                }
            val nonce =
                ByteBuffer
                    .allocate(12)
                    .putInt(0)
                    .putLong(counter)
                    .array()
            val binding =
                ProtectionBinding(
                    "core",
                    if (purpose ==
                        KeyPurpose.PII
                    ) {
                        "user_account"
                    } else {
                        "form_draft"
                    },
                    UUID.randomUUID(),
                    purpose,
                )
            val clear = "rotation-canary-v1".toByteArray()
            try {
                val envelope = AeadCipher().encrypt(id, material, nonce, binding, clear)
                val read = AeadCipher().decrypt(envelope, material, binding)
                check(read is ProtectedRead.Value && java.security.MessageDigest.isEqual(clear, read.value))
                if (read is ProtectedRead.Value) read.value.fill(0)
            } finally {
                clear.fill(0)
            }
        } finally {
            material.fill(0)
        }
    }
}
