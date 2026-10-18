package com.spoolin.core.features.bootstrap

import com.spoolin.core.features.account.persistence.AccountIdentity
import com.spoolin.core.features.account.persistence.AccountRole
import com.spoolin.core.features.account.persistence.IdentityRules
import com.spoolin.core.features.crypto.KeyMount
import com.spoolin.core.features.crypto.KeyPurpose
import com.spoolin.core.requireProtectedFile
import tools.jackson.core.StreamReadFeature
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.UUID

private const val KEY_PURPOSE_COUNT = 3
private const val CONFIGURATION_FILE_LIMIT = 16384L

private const val INITIAL_KEY_COUNT = 3

class InitializationAccount(
    val id: UUID,
    val identity: AccountIdentity,
    val role: AccountRole,
    val passwordFile: Path,
) {
    override fun toString(): String = "InitializationAccount(redacted)"
}

class InitializationConfiguration(
    val version: String,
    val accounts: List<InitializationAccount>,
    val keys: Map<String, KeyMount>,
) {
    init {
        require(version.matches(Regex("[a-zA-Z0-9_-]{1,32}"))) { "Initialization version is invalid" }
        require(accounts.size == 2 && accounts.map { it.role }.toSet() == AccountRole.entries.toSet())
        require(accounts.map { it.id }.toSet().size == 2)
        require(accounts.map { it.identity.emailNormalized }.toSet().size == 2)
        require(keys.size == KEY_PURPOSE_COUNT && keys.values.map { it.purpose }.toSet() == KeyPurpose.entries.toSet())
        require(keys.keys.all { it.matches(Regex("[a-zA-Z0-9_-]{1,64}")) })
    }

    fun checksum(): ByteArray {
        val identities =
            accounts.sortedBy { it.role.name }.map {
                listOf(
                    it.id.toString(),
                    it.identity.name,
                    it.identity.emailDisplay,
                    it.identity.emailNormalized,
                    it.role.name,
                )
            }
        val mounts = keys.toSortedMap().map { (id, mount) -> listOf(id, mount.purpose.name) }
        val bytes = JsonMapper.builder().build().writeValueAsBytes(listOf(version, identities, mounts))
        return MessageDigest.getInstance("SHA-256").digest(bytes)
    }

    override fun toString(): String = "InitializationConfiguration(redacted)"

    companion object {
        fun load(
            path: Path,
            rules: IdentityRules,
        ): InitializationConfiguration {
            requireProtectedFile(path, CONFIGURATION_FILE_LIMIT)
            val mapper =
                JsonMapper
                    .builder()
                    .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                    .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .build()
            val root = mapper.readTree(Files.readAllBytes(path))
            require(
                root.isObject && root.properties().map { it.key }.toSet() == setOf("setupVersion", "accounts", "keys"),
            )
            val accounts = requireNotNull(root.get("accounts"))
            require(accounts.isArray && accounts.size() == 2)
            val keys = requireNotNull(root.get("keys"))
            require(keys.isObject && keys.size() == INITIAL_KEY_COUNT)
            return InitializationConfiguration(
                text(root, "setupVersion"),
                (0 until accounts.size()).map { account(accounts.get(it), rules) },
                keys.properties().associate { entry ->
                    require(
                        entry.value
                            .properties()
                            .map { it.key }
                            .toSet() == setOf("purpose", "file"),
                    )
                    entry.key to
                        KeyMount(
                            KeyPurpose.valueOf(text(entry.value, "purpose")),
                            absolute(text(entry.value, "file")),
                        )
                },
            )
        }

        private fun account(
            node: JsonNode,
            rules: IdentityRules,
        ): InitializationAccount {
            val fields = setOf("id", "name", "email", "role", "passwordFile")
            require(node.isObject && node.properties().map { it.key }.toSet() == fields)
            return InitializationAccount(
                UUID.fromString(text(node, "id")),
                rules.identity(text(node, "name"), text(node, "email")),
                AccountRole.valueOf(text(node, "role")),
                absolute(text(node, "passwordFile")),
            )
        }

        private fun text(
            node: JsonNode,
            field: String,
        ): String {
            val value = requireNotNull(node.get(field))
            require(value.isString)
            return value.asText()
        }

        private fun absolute(value: String): Path = Path.of(value).also { require(it.isAbsolute) }
    }
}
