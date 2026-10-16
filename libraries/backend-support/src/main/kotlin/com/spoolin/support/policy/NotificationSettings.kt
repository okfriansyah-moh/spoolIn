package com.spoolin.support.policy

private const val SMTP_DEFAULT_PORT = 587
private const val MAXIMUM_TCP_PORT = 65535

object NotificationSettings {
    fun resolve(source: ConfigurationSources): Map<String, String?> {
        val mode =
            source
                .choice(
                    "NOTIFICATION_MODE",
                    "database",
                    setOf("database", "smtp", "slack", "google_chat"),
                ).value
        val names =
            when (mode) {
                "smtp" -> smtp(source)
                "slack" -> alternative(source, "SLACK_WEBHOOK_FILE", listOf("SLACK_BOT_TOKEN_FILE", "SLACK_CHANNEL"))
                "google_chat" ->
                    alternative(
                        source,
                        "GOOGLE_CHAT_WEBHOOK_FILE",
                        listOf("GOOGLE_CHAT_CREDENTIAL_FILE", "GOOGLE_CHAT_SPACE"),
                    )
                else -> emptyList()
            }
        return names.associateWith { source.required(it).value }
    }

    private fun smtp(source: ConfigurationSources): List<String> {
        source.integer("SMTP_PORT", SMTP_DEFAULT_PORT, 1, MAXIMUM_TCP_PORT)
        if (!source
                .boolean(
                    "SMTP_TLS_VERIFY",
                    true,
                ).value
        ) {
            throw ConfigurationViolation("SMTP_TLS_VERIFY", "Enable certificate verification.")
        }
        val credentials =
            when {
                source.value("SMTP_USERNAME") != null -> listOf("SMTP_USERNAME", "SMTP_PASSWORD_FILE")
                source.value(
                    "SMTP_PASSWORD_FILE",
                ) != null -> throw ConfigurationViolation(
                    "SMTP_USERNAME",
                    "Supply the selected SMTP authentication identity.",
                )
                else -> emptyList()
            }
        return listOf("SMTP_HOST", "SMTP_FROM", "SMTP_TO") + credentials
    }

    private fun alternative(
        source: ConfigurationSources,
        webhook: String,
        credentialGroup: List<String>,
    ): List<String> {
        val hasWebhook = source.value(webhook) != null
        val hasCredential = credentialGroup.any { source.value(it) != null }
        if (hasWebhook &&
            hasCredential
        ) {
            throw ConfigurationViolation(webhook, "Select one configured provider credential group.")
        }
        return if (hasCredential) credentialGroup else listOf(webhook)
    }
}
