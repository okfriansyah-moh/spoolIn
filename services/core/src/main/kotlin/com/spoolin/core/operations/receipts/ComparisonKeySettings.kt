package com.spoolin.core.operations.receipts

import com.spoolin.support.receipts.ComparisonKeys
import com.spoolin.support.receipts.IntentHashes
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.core.env.Environment

object ComparisonKeySettings {
    fun load(environment: Environment): IntentHashes {
        val name = "spoolin.operations.comparison-keys"
        require(environment.getProperty(name) == null) { "$name: Supply a map, not a scalar root." }
        val paths =
            runCatching {
                Binder.get(environment).bind(name, Bindable.mapOf(String::class.java, String::class.java)).get()
            }.getOrElse { throw IllegalArgumentException("$name: Supply an explicit key ID to protected-file map.") }
        val id = environment.getProperty("OPERATION_HMAC_KEY_ID") ?: invalid(name)
        val file = environment.getProperty("OPERATION_HMAC_KEY_FILE") ?: invalid(name)
        val others =
            listOf("PII_KEY_FILE", "DRAFT_KEY_FILE", "EMAIL_LOOKUP_KEY_FILE")
                .mapNotNull { environment.getProperty(it) }
        return ComparisonKeys.load(paths, id, file, others)
    }

    private fun invalid(name: String): Nothing =
        throw IllegalArgumentException("$name: Supply the active operation comparison key selectors.")
}
