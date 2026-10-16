package com.spoolin.support.policy

internal data class SelectedReference(
    val name: String,
    val mode: String,
    val baseline: Map<String, String>,
    val environmentValue: String = "/private/synthetic-environment",
    val fileValue: String = "/private/synthetic-file",
) {
    fun sources(
        environment: Map<String, String>,
        file: Map<String, String>,
    ) = ConfigurationSources(baseline.filterKeys { it != name } + environment, file)

    fun read(source: ConfigurationSources): Map<String, String?> =
        if (mode == "notification") NotificationSettings.resolve(source) else OperationalSettings.modes(source)

    fun disabled(): ConfigurationSources =
        ConfigurationSources(
            baseline + (name to "") +
                mapOf(
                    "NOTIFICATION_MODE" to "database",
                    "TUNNEL_ENABLED" to "false",
                    "MCP_PUBLICATION_ENABLED" to "false",
                ),
        )
}

internal object SelectedReferences {
    private val smtp =
        mapOf(
            "NOTIFICATION_MODE" to "smtp",
            "SMTP_HOST" to "smtp.example.test",
            "SMTP_FROM" to "sender@example.test",
            "SMTP_TO" to "recipient@example.test",
        )
    private val smtpAuth =
        smtp + mapOf("SMTP_USERNAME" to "synthetic-user", "SMTP_PASSWORD_FILE" to "/private/synthetic-file")
    private val slackWebhook = mapOf("NOTIFICATION_MODE" to "slack", "SLACK_WEBHOOK_FILE" to "/private/synthetic-file")
    private val slackToken =
        mapOf(
            "NOTIFICATION_MODE" to "slack",
            "SLACK_BOT_TOKEN_FILE" to "/private/synthetic-file",
            "SLACK_CHANNEL" to "C1234567890",
        )
    private val chatWebhook =
        mapOf(
            "NOTIFICATION_MODE" to "google_chat",
            "GOOGLE_CHAT_WEBHOOK_FILE" to "/private/synthetic-file",
        )
    private val chatCredential =
        mapOf(
            "NOTIFICATION_MODE" to "google_chat",
            "GOOGLE_CHAT_CREDENTIAL_FILE" to "/private/synthetic-file",
            "GOOGLE_CHAT_SPACE" to "spaces/synthetic",
        )
    private val tunnel =
        mapOf(
            "TUNNEL_ENABLED" to "true",
            "TUNNEL_ID" to "synthetic-tunnel",
            "TUNNEL_KEY_FILE" to "/private/synthetic-file",
            "TUNNEL_RUNNER_PATH" to "/private/synthetic-runner",
            "MCP_PUBLICATION_ENABLED" to "false",
        )
    private val publication =
        mapOf(
            "TUNNEL_ENABLED" to "false",
            "MCP_PUBLICATION_ENABLED" to "true",
            "MCP_UI_ASSERTION_KEY_FILE" to "/private/synthetic-file",
            "MCP_UI_ASSERTION_KEY_ID" to "ui-first",
        )
    val all =
        listOf(
            SelectedReference("SMTP_HOST", "notification", smtp, "smtp-a.example.test", "smtp-b.example.test"),
            SelectedReference("SMTP_FROM", "notification", smtp, "first@example.test", "second@example.test"),
            SelectedReference("SMTP_TO", "notification", smtp, "first@example.test", "second@example.test"),
            SelectedReference("SMTP_USERNAME", "notification", smtpAuth, "synthetic-first", "synthetic-second"),
            SelectedReference("SMTP_PASSWORD_FILE", "notification", smtpAuth),
            SelectedReference("SLACK_WEBHOOK_FILE", "notification", slackWebhook),
            SelectedReference("SLACK_BOT_TOKEN_FILE", "notification", slackToken),
            SelectedReference("SLACK_CHANNEL", "notification", slackToken, "C1234567890", "C9876543210"),
            SelectedReference("GOOGLE_CHAT_WEBHOOK_FILE", "notification", chatWebhook),
            SelectedReference("GOOGLE_CHAT_CREDENTIAL_FILE", "notification", chatCredential),
            SelectedReference("GOOGLE_CHAT_SPACE", "notification", chatCredential, "spaces/first", "spaces/second"),
            SelectedReference("TUNNEL_ID", "operational", tunnel, "synthetic-first", "synthetic-second"),
            SelectedReference("TUNNEL_KEY_FILE", "operational", tunnel),
            SelectedReference("TUNNEL_RUNNER_PATH", "operational", tunnel),
            SelectedReference("MCP_UI_ASSERTION_KEY_FILE", "operational", publication),
            SelectedReference("MCP_UI_ASSERTION_KEY_ID", "operational", publication, "ui-first", "ui-second"),
        )
}
