package com.spoolin.support.policy

import java.net.URI

private const val RETRY_MAX_RETRIES_DEFAULT = 2
private const val RETRY_MAX_RETRIES_MAXIMUM = 9
private const val RETRY_BASE_DELAY_MS_DEFAULT = 500
private const val RETRY_BASE_DELAY_MS_MAXIMUM = 60000
private const val RETRY_MAX_DELAY_MS_DEFAULT = 5000
private const val RETRY_MAX_DELAY_MS_MAXIMUM = 3600000
private const val RETRY_DEADLINE_MS_DEFAULT = 60000
private const val RETRY_DEADLINE_MS_MINIMUM = 1000
private const val RETRY_DEADLINE_MS_MAXIMUM = 3600000
private const val RETRY_MULTIPLIER_DEFAULT = 2
private const val RETRY_MULTIPLIER_MAXIMUM = 10
private const val MCP_RATE_LIMIT_MAX_BINDINGS_DEFAULT = 1024
private const val MCP_RATE_LIMIT_MAX_BINDINGS_MAXIMUM = 100000
private const val SIGNIN_ACCOUNT_REQUESTS = 10
private const val SIGNIN_ACCOUNT_SECONDS = 900
private const val SIGNIN_SOURCE_REQUESTS = 60
private const val SIGNIN_SOURCE_SECONDS = 900
private const val REGISTER_REQUESTS = 5
private const val REGISTER_SECONDS = 3600
private const val MUTATION_REQUESTS = 60
private const val MUTATION_SECONDS = 60
private const val READ_REQUESTS = 2400
private const val READ_SECONDS = 60
private const val MAXIMUM_RATE_WINDOW_SECONDS = 86400

enum class ValueSource { ENVIRONMENT, FILE, DEFAULT }

data class SettingValue<T>(
    val value: T,
    val source: ValueSource,
) {
    override fun toString(): String = "SettingValue(source=$source, value=redacted)"
}

class ConfigurationViolation(
    val setting: String,
    val correction: String,
) : IllegalArgumentException("$setting: $correction")

class ConfigurationSources(
    private val environment: Map<String, String> = emptyMap(),
    private val file: Map<String, String> = emptyMap(),
) {
    fun value(
        name: String,
        default: String? = null,
    ): SettingValue<String>? =
        when {
            environment.containsKey(name) -> SettingValue(environment.getValue(name), ValueSource.ENVIRONMENT)
            file.containsKey(name) -> SettingValue(file.getValue(name), ValueSource.FILE)
            default != null -> SettingValue(default, ValueSource.DEFAULT)
            else -> null
        }

    fun required(name: String): SettingValue<String> {
        val found = value(name) ?: throw ConfigurationViolation(name, "Supply the required setting.")
        if (TextRules.blank(found.value) || found.value.any { it in "\u0000\r\n" }) {
            throw ConfigurationViolation(name, "Supply a nonempty single-line setting.")
        }
        return found
    }

    fun integer(
        name: String,
        default: Int,
        min: Int,
        max: Int,
    ): SettingValue<Int> {
        val found = requireNotNull(value(name, default.toString()))
        val number = found.value.takeIf { it.matches(Regex("-?[0-9]+")) }?.toIntOrNull()
        if (number == null ||
            number !in min..max
        ) {
            throw ConfigurationViolation(name, "Use a decimal integer from $min to $max.")
        }
        return SettingValue(number, found.source)
    }

    fun boolean(
        name: String,
        default: Boolean,
    ): SettingValue<Boolean> {
        val found = requireNotNull(value(name, default.toString()))
        val bool =
            when (found.value) {
                "true" -> true
                "false" -> false
                else -> throw ConfigurationViolation(name, "Use true or false.")
            }
        return SettingValue(bool, found.source)
    }

    fun choice(
        name: String,
        default: String,
        choices: Set<String>,
    ): SettingValue<String> {
        val found = requireNotNull(value(name, default))
        if (found.value !in choices) throw ConfigurationViolation(name, "Select a supported mode.")
        return found
    }

    fun optionalDisabled(
        name: String,
        enabled: Boolean,
    ): String? = if (enabled) required(name).value else value(name)?.value?.takeUnless { TextRules.blank(it) }

    override fun toString(): String = "ConfigurationSources(redacted)"
}

data class IntegerSetting(
    val name: String,
    val default: Int,
    val min: Int,
    val max: Int,
    val units: String,
)

object OperationalSettings {
    val retry =
        listOf(
            IntegerSetting("RETRY_MAX_RETRIES", RETRY_MAX_RETRIES_DEFAULT, 0, RETRY_MAX_RETRIES_MAXIMUM, "retries"),
            IntegerSetting(
                "RETRY_BASE_DELAY_MS",
                RETRY_BASE_DELAY_MS_DEFAULT,
                1,
                RETRY_BASE_DELAY_MS_MAXIMUM,
                "milliseconds",
            ),
            IntegerSetting(
                "RETRY_MAX_DELAY_MS",
                RETRY_MAX_DELAY_MS_DEFAULT,
                1,
                RETRY_MAX_DELAY_MS_MAXIMUM,
                "milliseconds",
            ),
            IntegerSetting(
                "RETRY_DEADLINE_MS",
                RETRY_DEADLINE_MS_DEFAULT,
                RETRY_DEADLINE_MS_MINIMUM,
                RETRY_DEADLINE_MS_MAXIMUM,
                "milliseconds",
            ),
            IntegerSetting("RETRY_MULTIPLIER", RETRY_MULTIPLIER_DEFAULT, 1, RETRY_MULTIPLIER_MAXIMUM, "multiplier"),
        )
    val rate =
        listOf(
            "SIGNIN_ACCOUNT" to (SIGNIN_ACCOUNT_REQUESTS to SIGNIN_ACCOUNT_SECONDS),
            "SIGNIN_SOURCE" to (SIGNIN_SOURCE_REQUESTS to SIGNIN_SOURCE_SECONDS),
            "REGISTER" to (REGISTER_REQUESTS to REGISTER_SECONDS),
            "MUTATION" to (MUTATION_REQUESTS to MUTATION_SECONDS),
            "READ" to (READ_REQUESTS to READ_SECONDS),
        ).flatMap { (profile, defaults) ->
            listOf(
                IntegerSetting("RATE_LIMIT_${profile}_MAX_REQUESTS", defaults.first, 1, Int.MAX_VALUE, "requests"),
                IntegerSetting(
                    "RATE_LIMIT_${profile}_WINDOW_SECONDS",
                    defaults.second,
                    1,
                    MAXIMUM_RATE_WINDOW_SECONDS,
                    "seconds",
                ),
            )
        } +
            IntegerSetting(
                "MCP_RATE_LIMIT_MAX_BINDINGS",
                MCP_RATE_LIMIT_MAX_BINDINGS_DEFAULT,
                1,
                MCP_RATE_LIMIT_MAX_BINDINGS_MAXIMUM,
                "bindings",
            )

    fun resolve(source: ConfigurationSources): Map<String, SettingValue<Int>> {
        val settings = (retry + rate).associate { it.name to source.integer(it.name, it.default, it.min, it.max) }
        if (settings.getValue("RETRY_MAX_DELAY_MS").value < settings.getValue("RETRY_BASE_DELAY_MS").value) {
            throw ConfigurationViolation("RETRY_MAX_DELAY_MS", "Use a cap at least as large as the base delay.")
        }
        return settings
    }

    fun modes(source: ConfigurationSources): Map<String, String?> {
        val values = NotificationSettings.resolve(source).toMutableMap()
        val tunnel = source.boolean("TUNNEL_ENABLED", true).value
        listOf("TUNNEL_ID", "TUNNEL_KEY_FILE", "TUNNEL_RUNNER_PATH").forEach {
            values[it] =
                source.optionalDisabled(it, tunnel)
        }
        val publication = source.boolean("MCP_PUBLICATION_ENABLED", false).value
        listOf("MCP_UI_ASSERTION_KEY_FILE", "MCP_UI_ASSERTION_KEY_ID").forEach {
            values[it] =
                source.optionalDisabled(it, publication)
        }
        source.choice("LOG_LEVEL", "info", setOf("debug", "info"))
        return values
    }
}

object OwnerConfiguration {
    fun validate(
        owner: String,
        source: ConfigurationSources,
    ) {
        if (owner !in
            setOf("core", "catalog", "media", "mcp")
        ) {
            throw ConfigurationViolation("SERVICE_IDENTITY", "Select an application service.")
        }
        listOf("SERVICE_CERT_FILE", "SERVICE_KEY_FILE", "SERVICE_CA_FILE").forEach(source::required)
        if (owner != "mcp") database(owner, source)
        if (owner == "core") core(source)
        if (owner in setOf("catalog", "media")) source.required("ACTOR_VERIFY_KEYS_FILE")
    }

    private fun database(
        owner: String,
        source: ConfigurationSources,
    ) {
        val prefix = owner.uppercase()
        val url = source.required("${prefix}_DB_URL").value
        val uri = runCatching { URI(url.removePrefix("jdbc:")) }.getOrNull()
        val addressValid = url.startsWith("jdbc:postgresql://") && uri?.path == "/spoolin_$owner"
        val parameters =
            uri
                ?.rawQuery
                .orEmpty()
                .split('&')
                .map { it.split('=', limit = 2) }
        val tls = parameters.filter { it.first() == "sslmode" }
        val supported = setOf("sslmode", "sslrootcert", "connectTimeout")
        val safeOptions = parameters.all { it.size == 2 && it.first() in supported }
        val secure = uri?.userInfo == null && tls == listOf(listOf("sslmode", "verify-full"))
        if (!addressValid || !secure || !safeOptions) {
            throw ConfigurationViolation("${prefix}_DB_URL", "Use the owned PostgreSQL database with verify-full TLS.")
        }
        if (source.required("${prefix}_DB_USER").value !in
            setOf("spoolin_${owner}_runtime", "spoolin_${owner}_migrator")
        ) {
            throw ConfigurationViolation("${prefix}_DB_USER", "Select the owner role.")
        }
        source.required("${prefix}_DB_PASSWORD_FILE")
    }

    private fun core(source: ConfigurationSources) {
        listOf(
            "PII_KEY_FILE",
            "DRAFT_KEY_FILE",
            "EMAIL_LOOKUP_KEY_FILE",
            "OPERATION_HMAC_KEY_FILE",
            "CURSOR_KEY_FILE",
            "CSRF_KEY_FILE",
            "RATE_LIMIT_KEY_FILE",
            "ACTOR_SIGNING_PRIVATE_KEY_FILE",
            "ACTOR_SIGNING_KEY_ID",
            "VIEWER_IDENTITY_FILE",
            "VIEWER_PASSWORD_FILE",
            "MAINTAINER_IDENTITY_FILE",
            "MAINTAINER_PASSWORD_FILE",
        ).forEach(source::required)
        listOf("ACTOR_SIGNING_KEY_ID", "PII_KEY_ID", "DRAFT_KEY_ID", "EMAIL_LOOKUP_KEY_ID", "OPERATION_HMAC_KEY_ID")
            .forEach { name -> CompatibilityFacts.keyId(source.required(name).value, name) }
        if (source.required("BROWSER_ORIGIN").value != "https://localhost:8443") {
            throw ConfigurationViolation("BROWSER_ORIGIN", "Use the configured trusted loopback HTTPS origin.")
        }
    }
}
