package com.spoolin.core.operations.receipts

import com.spoolin.core.features.account.persistence.AccountProtectionSettings
import com.spoolin.support.receipts.ComparisonKeys
import com.spoolin.support.receipts.IntentHashes
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.core.env.Environment

object ComparisonKeySettings {
    fun load(environment: Environment): IntentHashes {
        val name = "spoolin.operations.comparison-keys"
        validateJson(environment, name)
        require(environment.getProperty(name) == null) { "$name: Supply a map, not a scalar root." }
        val paths =
            runCatching {
                Binder.get(environment).bind(name, Bindable.mapOf(String::class.java, String::class.java)).get()
            }.getOrElse { throw IllegalArgumentException("$name: Supply an explicit key ID to protected-file map.") }
        val id = environment.getProperty("OPERATION_HMAC_KEY_ID") ?: invalid(name)
        val file = environment.getProperty("OPERATION_HMAC_KEY_FILE") ?: invalid(name)
        val others = otherPurposes(environment, name)
        return ComparisonKeys.load(paths, id, file, others)
    }

    private fun otherPurposes(
        environment: Environment,
        name: String,
    ): List<String> =
        runCatching {
            val settings =
                Binder
                    .get(environment)
                    .bind(
                        "spoolin.account-protection",
                        Bindable.of(AccountProtectionSettings::class.java),
                    ).orElse(
                        com.spoolin.core.features.account.persistence
                            .AccountProtectionSettings(),
                    )
            val mounts =
                settings
                    ?.keys
                    .orEmpty()
                    .values
                    .map { it.file.toString() }
            val selectors =
                listOf("CSRF_KEY_FILE", "RATE_LIMIT_KEY_FILE", "CURSOR_KEY_FILE", "MCP_UI_ASSERTION_KEY_FILE")
                    .mapNotNull { environment.getProperty(it) }
            mounts + selectors
        }.getOrElse { throw IllegalArgumentException("$name: Correct the protected purpose-key configuration.") }

    private fun validateJson(
        environment: Environment,
        name: String,
    ) {
        val raw =
            environment.getProperty("SPRING_APPLICATION_JSON")
                ?: environment.getProperty("spring.application.json") ?: return
        val valid =
            runCatching {
                val mapper =
                    tools.jackson.databind.json.JsonMapper
                        .builder()
                        .enable(tools.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                        .build()
                val root =
                    mapper
                        .readTree(raw)
                        .path("spoolin")
                        .path("operations")
                        .path("comparison-keys")
                if (!root.isMissingNode) {
                    require(root.isObject)
                    root.properties().forEach { entry ->
                        val id = entry.key
                        val value = entry.value
                        require(id.matches(Regex("[a-zA-Z0-9_-]{1,64}")) && value.isString)
                    }
                }
            }.isSuccess
        require(valid) { "$name: Supply a map with unique exact key IDs and protected-file references." }
    }

    private fun invalid(name: String): Nothing =
        throw IllegalArgumentException("$name: Supply the active operation comparison key selectors.")
}
