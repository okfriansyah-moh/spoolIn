package com.spoolin.support.policy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

abstract class FullConfigurationFixture {
    protected val references =
        listOf(
            "SERVICE_CERT_FILE",
            "SERVICE_KEY_FILE",
            "SERVICE_CA_FILE",
            "PII_KEY_FILE",
            "DRAFT_KEY_FILE",
            "EMAIL_LOOKUP_KEY_FILE",
            "OPERATION_HMAC_KEY_FILE",
            "CURSOR_KEY_FILE",
            "CSRF_KEY_FILE",
            "RATE_LIMIT_KEY_FILE",
            "ACTOR_SIGNING_PRIVATE_KEY_FILE",
            "ACTOR_SIGNING_KEY_ID",
            "ACTOR_VERIFY_KEYS_FILE",
            "VIEWER_IDENTITY_FILE",
            "VIEWER_PASSWORD_FILE",
            "MAINTAINER_IDENTITY_FILE",
            "MAINTAINER_PASSWORD_FILE",
        ).associateWith {
            "/private/synthetic-file"
        } +
            ("BROWSER_ORIGIN" to "https://localhost:8443") + ("ACTOR_SIGNING_KEY_ID" to "actor-v1") +
            listOf(
                "PII_KEY_ID",
                "DRAFT_KEY_ID",
                "EMAIL_LOOKUP_KEY_ID",
                "OPERATION_HMAC_KEY_ID",
            ).associateWith { "test-v1" }

    protected fun ownerSetting(
        owner: String,
        name: String,
    ) {
        val prefix = owner.uppercase()
        val input =
            references +
                mapOf(
                    "${prefix}_DB_URL" to "jdbc:postgresql://db:5432/spoolin_$owner?sslmode=verify-full",
                    "${prefix}_DB_USER" to "spoolin_${owner}_runtime",
                    "${prefix}_DB_PASSWORD_FILE" to "/private/synthetic-file",
                )
        OwnerConfiguration.validate(owner, ConfigurationSources(input))
        assertThrows(ConfigurationViolation::class.java) {
            OwnerConfiguration.validate(
                owner,
                ConfigurationSources(input + (name to "")),
            )
        }
    }

    protected fun textBoundary(
        field: String,
        min: Int,
        max: Int,
    ) {
        val bound = PolicySettings.resolve(ConfigurationSources()).textBounds.getValue(field)
        assertEquals(min, bound.minCodePoints)
        assertEquals(max, bound.maxCodePoints)
        for (size in listOf(min - 1, min, min + 1, max - 1, max, max + 1).filter { it >= 0 }) {
            if (size in min..max) {
                assertEquals(size, TextRules.ordinary("x".repeat(size), bound).length)
            } else {
                assertThrows(InputViolation::class.java) { TextRules.ordinary("x".repeat(size), bound) }
            }
        }
    }

    protected fun conditionalReference(
        name: String,
        enabled: String,
    ) {
        assertNull(ConfigurationSources(mapOf(name to "")).optionalDisabled(name, false))
        assertThrows(ConfigurationViolation::class.java) { ConfigurationSources().optionalDisabled(name, true) }
        val base =
            if (enabled ==
                "TUNNEL_ENABLED"
            ) {
                mapOf("TUNNEL_ENABLED" to "false")
            } else {
                mapOf(
                    "TUNNEL_ENABLED" to "false",
                    "MCP_PUBLICATION_ENABLED" to "false",
                )
            }
        OperationalSettings.modes(ConfigurationSources(base))
    }

    protected fun notificationRecipe(row: Int) {
        val smtp =
            mapOf(
                "NOTIFICATION_MODE" to "smtp",
                "SMTP_HOST" to "smtp.example.test",
                "SMTP_FROM" to "sender@example.test",
                "SMTP_TO" to "recipient@example.test",
            )
        val selected =
            mapOf(
                49 to smtp,
                50 to smtp,
                51 to smtp,
                52 to
                    (
                        smtp +
                            mapOf(
                                "SMTP_USERNAME" to "test-account",
                                "SMTP_PASSWORD_FILE" to "/private/synthetic-file",
                            )
                    ),
                53 to mapOf("NOTIFICATION_MODE" to "slack", "SLACK_WEBHOOK_FILE" to "/private/synthetic-file"),
                54 to
                    mapOf(
                        "NOTIFICATION_MODE" to "google_chat",
                        "GOOGLE_CHAT_WEBHOOK_FILE" to "/private/synthetic-file",
                    ),
            ).getValue(row)
        NotificationSettings.resolve(ConfigurationSources(selected))
        val bad =
            mapOf(
                49 to "SMTP_HOST",
                50 to "SMTP_TLS_VERIFY",
                51 to "SMTP_FROM",
                52 to "SMTP_PASSWORD_FILE",
                53 to "SLACK_WEBHOOK_FILE",
                54 to "GOOGLE_CHAT_WEBHOOK_FILE",
            ).getValue(row)
        assertThrows(ConfigurationViolation::class.java) {
            NotificationSettings.resolve(
                ConfigurationSources(selected + (bad to if (row == 50) "false" else "")),
            )
        }
    }
}

class FullConfigurationRulesTest : FullConfigurationFixture() {
    private val recipes: List<DynamicTest> =
        listOf(
            dynamicTest(
                "F-CONFIG row000 CORE verify-full owner-specific JDBC URL",
            ) { ownerSetting("core", "CORE_DB_URL") },
            dynamicTest(
                "F-CONFIG row001 CORE owner-specific runtime/migrator principal",
            ) { ownerSetting("core", "CORE_DB_USER") },
            dynamicTest(
                "F-CONFIG row002 CORE protected credential reference",
            ) { ownerSetting("core", "CORE_DB_PASSWORD_FILE") },
            dynamicTest("F-CONFIG row003 CATALOG verify-full owner-specific JDBC URL") {
                ownerSetting("catalog", "CATALOG_DB_URL")
            },
            dynamicTest("F-CONFIG row004 CATALOG owner-specific runtime/migrator principal") {
                ownerSetting("catalog", "CATALOG_DB_USER")
            },
            dynamicTest("F-CONFIG row005 CATALOG protected credential reference") {
                ownerSetting("catalog", "CATALOG_DB_PASSWORD_FILE")
            },
            dynamicTest(
                "F-CONFIG row006 MEDIA verify-full owner-specific JDBC URL",
            ) { ownerSetting("media", "MEDIA_DB_URL") },
            dynamicTest("F-CONFIG row007 MEDIA owner-specific runtime/migrator principal") {
                ownerSetting("media", "MEDIA_DB_USER")
            },
            dynamicTest(
                "F-CONFIG row008 MEDIA protected credential reference",
            ) { ownerSetting("media", "MEDIA_DB_PASSWORD_FILE") },
            dynamicTest("F-CONFIG row009 pool.maximum") {
                CompatibilityFacts.validate("pool.maximum", 8)
                assertThrows(ConfigurationViolation::class.java) { CompatibilityFacts.validate("pool.maximum", 9) }
            },
            dynamicTest("F-CONFIG row010 pool.minimumIdle") {
                CompatibilityFacts.validate("pool.minimumIdle", 1)
                assertThrows(ConfigurationViolation::class.java) { CompatibilityFacts.validate("pool.minimumIdle", 2) }
            },
            dynamicTest("F-CONFIG row011 connectTimeout") {
                CompatibilityFacts.validate("connectTimeout", 2000)
                assertThrows(ConfigurationViolation::class.java) { CompatibilityFacts.validate("connectTimeout", 2001) }
            },
            dynamicTest("F-CONFIG row012 statementTimeout") {
                CompatibilityFacts.validate("statementTimeout", 3000)
                assertThrows(
                    ConfigurationViolation::class.java,
                ) { CompatibilityFacts.validate("statementTimeout", 3001) }
            },
            dynamicTest("F-CONFIG row013 lockTimeout") {
                CompatibilityFacts.validate("lockTimeout", 2000)
                assertThrows(ConfigurationViolation::class.java) { CompatibilityFacts.validate("lockTimeout", 2001) }
            },
            dynamicTest("F-CONFIG row014 idleTransactionTimeout") {
                CompatibilityFacts.validate("idleTransactionTimeout", 5000)
                assertThrows(
                    ConfigurationViolation::class.java,
                ) { CompatibilityFacts.validate("idleTransactionTimeout", 5001) }
            },
            dynamicTest("F-CONFIG row015 migrationStatementTimeout") {
                CompatibilityFacts.validate("migrationStatementTimeout", 120000)
                assertThrows(
                    ConfigurationViolation::class.java,
                ) { CompatibilityFacts.validate("migrationStatementTimeout", 120001) }
            },
            dynamicTest("F-CONFIG row016 trusted browser origin") {
                assertEquals(
                    "/private/synthetic-file",
                    ConfigurationSources(
                        mapOf("BROWSER_ORIGIN" to "/private/synthetic-file"),
                    ).required("BROWSER_ORIGIN").value,
                )
                assertThrows(ConfigurationViolation::class.java) { ConfigurationSources().required("BROWSER_ORIGIN") }
            },
            dynamicTest("F-CONFIG row017 SERVICE_CERT_FILE") {
                assertEquals(
                    "/private/synthetic-file",
                    ConfigurationSources(
                        mapOf("SERVICE_CERT_FILE" to "/private/synthetic-file"),
                    ).required("SERVICE_CERT_FILE").value,
                )
                assertThrows(
                    ConfigurationViolation::class.java,
                ) { ConfigurationSources().required("SERVICE_CERT_FILE") }
            },
            dynamicTest("F-CONFIG row018 SERVICE_KEY_FILE") {
                assertEquals(
                    "/private/synthetic-file",
                    ConfigurationSources(
                        mapOf("SERVICE_KEY_FILE" to "/private/synthetic-file"),
                    ).required("SERVICE_KEY_FILE").value,
                )
                assertThrows(
                    ConfigurationViolation::class.java,
                ) { ConfigurationSources().required("SERVICE_KEY_FILE") }
            },
            dynamicTest("F-CONFIG row019 SERVICE_CA_FILE") {
                assertEquals(
                    "/private/synthetic-file",
                    ConfigurationSources(
                        mapOf("SERVICE_CA_FILE" to "/private/synthetic-file"),
                    ).required("SERVICE_CA_FILE").value,
                )
                assertThrows(
                    ConfigurationViolation::class.java,
                ) { ConfigurationSources().required("SERVICE_CA_FILE") }
            },
            dynamicTest("F-CONFIG row020 service certificate SAN") {
                OwnerConfiguration.validate("mcp", ConfigurationSources(references))
                assertThrows(
                    ConfigurationViolation::class.java,
                ) { OwnerConfiguration.validate("invented", ConfigurationSources(references)) }
            },
            dynamicTest("F-CONFIG row021 CoreHTTPSport") {
                CompatibilityFacts.validate("CoreHTTPSport", 8443)
                assertThrows(ConfigurationViolation::class.java) { CompatibilityFacts.validate("CoreHTTPSport", 8444) }
            },
            dynamicTest("F-CONFIG row022 MCPtransportPort") {
                CompatibilityFacts.validate("MCPtransportPort", 8787)
                assertThrows(
                    ConfigurationViolation::class.java,
                ) { CompatibilityFacts.validate("MCPtransportPort", 8788) }
            },
            dynamicTest("F-CONFIG row023 MCPprivateExporterPort") {
                CompatibilityFacts.validate("MCPprivateExporterPort", 8788)
                assertThrows(
                    ConfigurationViolation::class.java,
                ) { CompatibilityFacts.validate("MCPprivateExporterPort", 8789) }
            },
            dynamicTest("F-CONFIG row024 PII secret reference") {
                assertEquals(
                    "/private/synthetic-file",
                    ConfigurationSources(
                        mapOf("PII_KEY_FILE" to "/private/synthetic-file"),
                    ).required("PII_KEY_FILE").value,
                )
                assertThrows(ConfigurationViolation::class.java) { ConfigurationSources().required("PII_KEY_FILE") }
            },
            dynamicTest("F-CONFIG row025 DRAFT secret reference") {
                assertEquals(
                    "/private/synthetic-file",
                    ConfigurationSources(
                        mapOf("DRAFT_KEY_FILE" to "/private/synthetic-file"),
                    ).required("DRAFT_KEY_FILE").value,
                )
                assertThrows(ConfigurationViolation::class.java) { ConfigurationSources().required("DRAFT_KEY_FILE") }
            },
            dynamicTest("F-CONFIG row026 EMAIL_LOOKUP secret reference") {
                assertEquals(
                    "/private/synthetic-file",
                    ConfigurationSources(
                        mapOf("EMAIL_LOOKUP_KEY_FILE" to "/private/synthetic-file"),
                    ).required("EMAIL_LOOKUP_KEY_FILE").value,
                )
                assertThrows(
                    ConfigurationViolation::class.java,
                ) { ConfigurationSources().required("EMAIL_LOOKUP_KEY_FILE") }
            },
            dynamicTest("F-CONFIG row027 operation-HMAC secret reference") {
                assertEquals(
                    "/private/synthetic-file",
                    ConfigurationSources(
                        mapOf("OPERATION_HMAC_KEY_FILE" to "/private/synthetic-file"),
                    ).required("OPERATION_HMAC_KEY_FILE").value,
                )
                assertThrows(
                    ConfigurationViolation::class.java,
                ) { ConfigurationSources().required("OPERATION_HMAC_KEY_FILE") }
            },
            dynamicTest("F-CONFIG row028 cursor secret reference") {
                assertEquals(
                    "/private/synthetic-file",
                    ConfigurationSources(
                        mapOf("CURSOR_KEY_FILE" to "/private/synthetic-file"),
                    ).required("CURSOR_KEY_FILE").value,
                )
                assertThrows(
                    ConfigurationViolation::class.java,
                ) { ConfigurationSources().required("CURSOR_KEY_FILE") }
            },
            dynamicTest("F-CONFIG row029 CSRF secret reference") {
                assertEquals(
                    "/private/synthetic-file",
                    ConfigurationSources(
                        mapOf("CSRF_KEY_FILE" to "/private/synthetic-file"),
                    ).required("CSRF_KEY_FILE").value,
                )
                assertThrows(ConfigurationViolation::class.java) { ConfigurationSources().required("CSRF_KEY_FILE") }
            },
            dynamicTest("F-CONFIG row030 active/read key IDs") {
                CompatibilityFacts.keyId("test-v1")
                assertThrows(ConfigurationViolation::class.java) { CompatibilityFacts.keyId("x".repeat(65)) }
                assertThrows(ConfigurationViolation::class.java) { CompatibilityFacts.keyId("") }
            },
            dynamicTest("F-CONFIG row031 AES key size") {
                CompatibilityFacts.validate("AES key size", 32)
                assertThrows(ConfigurationViolation::class.java) { CompatibilityFacts.validate("AES key size", 33) }
            },
            dynamicTest("F-CONFIG row032 protected file mode") {
                CompatibilityFacts.validate("protected file mode", "0600")
                assertThrows(
                    ConfigurationViolation::class.java,
                ) { CompatibilityFacts.validate("protected file mode", "0600-invalid") }
            },
            dynamicTest("F-CONFIG row033 account/draft plaintext envelope") {
                CompatibilityFacts.validate("account/draft plaintext envelope", 65536)
                assertThrows(ConfigurationViolation::class.java) {
                    CompatibilityFacts.validate("account/draft plaintext envelope", 65537)
                }
            },
            dynamicTest("F-CONFIG row034 ACTOR_SIGNING_PRIVATE_KEY_FILE") {
                assertEquals(
                    "/private/synthetic-file",
                    ConfigurationSources(
                        mapOf("ACTOR_SIGNING_PRIVATE_KEY_FILE" to "/private/synthetic-file"),
                    ).required("ACTOR_SIGNING_PRIVATE_KEY_FILE").value,
                )
                assertThrows(
                    ConfigurationViolation::class.java,
                ) { ConfigurationSources().required("ACTOR_SIGNING_PRIVATE_KEY_FILE") }
            },
            dynamicTest("F-CONFIG row035 ACTOR_SIGNING_KEY_ID") {
                assertEquals(
                    "/private/synthetic-file",
                    ConfigurationSources(
                        mapOf("ACTOR_SIGNING_KEY_ID" to "/private/synthetic-file"),
                    ).required("ACTOR_SIGNING_KEY_ID").value,
                )
                assertThrows(
                    ConfigurationViolation::class.java,
                ) { ConfigurationSources().required("ACTOR_SIGNING_KEY_ID") }
            },
            dynamicTest("F-CONFIG row036 ACTOR_VERIFY_KEYS_FILE") {
                assertEquals(
                    "/private/synthetic-file",
                    ConfigurationSources(
                        mapOf("ACTOR_VERIFY_KEYS_FILE" to "/private/synthetic-file"),
                    ).required("ACTOR_VERIFY_KEYS_FILE").value,
                )
                assertThrows(
                    ConfigurationViolation::class.java,
                ) { ConfigurationSources().required("ACTOR_VERIFY_KEYS_FILE") }
            },
            dynamicTest("F-CONFIG row037 verify map entries maximum") {
                CompatibilityFacts.validate("verify map entries maximum", 4)
                assertThrows(
                    ConfigurationViolation::class.java,
                ) { CompatibilityFacts.validate("verify map entries maximum", 5) }
            },
            dynamicTest("F-CONFIG row038 previous verifier minimum retention seconds") {
                CompatibilityFacts.validate("previous verifier minimum retention seconds", 60)
                assertThrows(
                    ConfigurationViolation::class.java,
                ) { CompatibilityFacts.validate("previous verifier minimum retention seconds", 61) }
            },
            dynamicTest("F-CONFIG row039 assertion lifetime maximum seconds") {
                CompatibilityFacts.validate("assertion lifetime maximum seconds", 30)
                assertThrows(ConfigurationViolation::class.java) {
                    CompatibilityFacts.validate("assertion lifetime maximum seconds", 31)
                }
            },
            dynamicTest("F-CONFIG row040 assertion metadata maximum bytes") {
                CompatibilityFacts.validate("assertion metadata maximum bytes", 4096)
                assertThrows(ConfigurationViolation::class.java) {
                    CompatibilityFacts.validate("assertion metadata maximum bytes", 4097)
                }
            },
            dynamicTest("F-CONFIG row041 skew maximum seconds") {
                CompatibilityFacts.validate("skew maximum seconds", 2)
                assertThrows(
                    ConfigurationViolation::class.java,
                ) { CompatibilityFacts.validate("skew maximum seconds", 3) }
            },
        )

    @TestFactory
    fun configurationRows() = recipes
}

class ConfigurationSecurityRulesTest : FullConfigurationFixture() {
    private val recipes: List<DynamicTest> =
        listOf(
            dynamicTest("F-CONFIG row042 Viewer identity reference") {
                assertEquals(
                    "/private/synthetic-file",
                    ConfigurationSources(
                        mapOf("VIEWER_IDENTITY_FILE" to "/private/synthetic-file"),
                    ).required("VIEWER_IDENTITY_FILE").value,
                )
                assertThrows(
                    ConfigurationViolation::class.java,
                ) { ConfigurationSources().required("VIEWER_IDENTITY_FILE") }
            },
            dynamicTest("F-CONFIG row043 Viewer password reference") {
                assertEquals(
                    "/private/synthetic-file",
                    ConfigurationSources(
                        mapOf("VIEWER_PASSWORD_FILE" to "/private/synthetic-file"),
                    ).required("VIEWER_PASSWORD_FILE").value,
                )
                assertThrows(
                    ConfigurationViolation::class.java,
                ) { ConfigurationSources().required("VIEWER_PASSWORD_FILE") }
            },
            dynamicTest("F-CONFIG row044 Maintainer identity reference") {
                assertEquals(
                    "/private/synthetic-file",
                    ConfigurationSources(
                        mapOf("MAINTAINER_IDENTITY_FILE" to "/private/synthetic-file"),
                    ).required("MAINTAINER_IDENTITY_FILE").value,
                )
                assertThrows(
                    ConfigurationViolation::class.java,
                ) { ConfigurationSources().required("MAINTAINER_IDENTITY_FILE") }
            },
            dynamicTest("F-CONFIG row045 Maintainer password reference") {
                assertEquals(
                    "/private/synthetic-file",
                    ConfigurationSources(
                        mapOf("MAINTAINER_PASSWORD_FILE" to "/private/synthetic-file"),
                    ).required("MAINTAINER_PASSWORD_FILE").value,
                )
                assertThrows(
                    ConfigurationViolation::class.java,
                ) { ConfigurationSources().required("MAINTAINER_PASSWORD_FILE") }
            },
            dynamicTest("F-CONFIG row046 frozen manifest identity") {
                StructuralRules.manifest("seed-test-v1", "0".repeat(64))
                assertThrows(ConfigurationViolation::class.java) { StructuralRules.manifest("", "0".repeat(64)) }
            },
            dynamicTest("F-CONFIG row047 frozen manifest hash") {
                StructuralRules.manifest("seed-test-v1", "0".repeat(64))
                assertThrows(ConfigurationViolation::class.java) { StructuralRules.manifest("seed-test-v1", "bad") }
            },
            dynamicTest("F-CONFIG row048 selected mode") {
                assertTrue(NotificationSettings.resolve(ConfigurationSources()).isEmpty())
                assertThrows(ConfigurationViolation::class.java) {
                    NotificationSettings.resolve(
                        ConfigurationSources(
                            mapOf("NOTIFICATION_MODE" to "unknown"),
                        ),
                    )
                }
            },
            dynamicTest("F-CONFIG row049 SMTP fixed endpoint") { notificationRecipe(49) },
            dynamicTest("F-CONFIG row050 SMTP TLS verification") { notificationRecipe(50) },
            dynamicTest("F-CONFIG row051 SMTP fixed from/to") { notificationRecipe(51) },
            dynamicTest("F-CONFIG row052 SMTP credential references as required") { notificationRecipe(52) },
            dynamicTest("F-CONFIG row053 Slack fixed webhook or token/channel") { notificationRecipe(53) },
            dynamicTest("F-CONFIG row054 Google Chat fixed space webhook/credential") { notificationRecipe(54) },
            dynamicTest("F-CONFIG row055 maximum retries") {
                assertEquals(2, ConfigurationSources().integer("RETRY_MAX_RETRIES", 2, 0, 9).value)
                assertThrows(ConfigurationViolation::class.java) {
                    ConfigurationSources(
                        mapOf("RETRY_MAX_RETRIES" to "NaN"),
                    ).integer("RETRY_MAX_RETRIES", 2, 0, 9)
                }
            },
            dynamicTest("F-CONFIG row056 base delay") {
                assertEquals(500, ConfigurationSources().integer("RETRY_BASE_DELAY_MS", 500, 1, 60000).value)
                assertThrows(ConfigurationViolation::class.java) {
                    ConfigurationSources(
                        mapOf("RETRY_BASE_DELAY_MS" to "NaN"),
                    ).integer("RETRY_BASE_DELAY_MS", 500, 1, 60000)
                }
            },
            dynamicTest("F-CONFIG row057 cap delay") {
                assertEquals(5000, ConfigurationSources().integer("RETRY_MAX_DELAY_MS", 5000, 1, 3600000).value)
                assertThrows(ConfigurationViolation::class.java) {
                    ConfigurationSources(
                        mapOf("RETRY_MAX_DELAY_MS" to "NaN"),
                    ).integer("RETRY_MAX_DELAY_MS", 5000, 1, 3600000)
                }
            },
            dynamicTest("F-CONFIG row058 work deadline") {
                assertEquals(60000, ConfigurationSources().integer("RETRY_DEADLINE_MS", 60000, 1000, 3600000).value)
                assertThrows(ConfigurationViolation::class.java) {
                    ConfigurationSources(
                        mapOf("RETRY_DEADLINE_MS" to "NaN"),
                    ).integer("RETRY_DEADLINE_MS", 60000, 1000, 3600000)
                }
            },
            dynamicTest("F-CONFIG row059 multiplier") {
                assertEquals(2, ConfigurationSources().integer("RETRY_MULTIPLIER", 2, 1, 10).value)
                assertThrows(ConfigurationViolation::class.java) {
                    ConfigurationSources(
                        mapOf("RETRY_MULTIPLIER" to "NaN"),
                    ).integer("RETRY_MULTIPLIER", 2, 1, 10)
                }
            },
            dynamicTest("F-CONFIG row060 jitter algorithm") {
                CompatibilityFacts.validate("jitter algorithm", "uniform[0,min(cap,base*2^(attempt-1))]")
                assertThrows(ConfigurationViolation::class.java) {
                    CompatibilityFacts.validate("jitter algorithm", "uniform[0,min(cap,base*2^(attempt-1))]-invalid")
                }
            },
            dynamicTest("F-CONFIG row061 registrationName") { textBoundary("registrationName", 2, 100) },
            dynamicTest("F-CONFIG row062 personName") { textBoundary("personName", 1, 100) },
            dynamicTest("F-CONFIG row063 title") { textBoundary("title", 1, 255) },
            dynamicTest("F-CONFIG row064 originalTitle") { textBoundary("originalTitle", 1, 255) },
            dynamicTest("F-CONFIG row065 synopsis") { textBoundary("synopsis", 1, 5000) },
            dynamicTest("F-CONFIG row066 creditRole") { textBoundary("creditRole", 1, 100) },
            dynamicTest("F-CONFIG row067 characterName") { textBoundary("characterName", 1, 200) },
            dynamicTest("F-CONFIG row068 biography") { textBoundary("biography", 1, 5000) },
            dynamicTest("F-CONFIG row069 email") { textBoundary("email", 1, 254) },
            dynamicTest("F-CONFIG row070 new password") { textBoundary("password", 15, 128) },
            dynamicTest("F-CONFIG row071 new-password byte ceiling") {
                CompatibilityFacts.validate("new-password byte ceiling", 512)
                assertThrows(
                    ConfigurationViolation::class.java,
                ) { CompatibilityFacts.validate("new-password byte ceiling", 513) }
            },
            dynamicTest("F-CONFIG row072 search") { textBoundary("search", 0, 200) },
            dynamicTest("F-CONFIG row073 sourceUrl") {
                CompatibilityFacts.validate("sourceUrl", 2048)
                assertThrows(ConfigurationViolation::class.java) { CompatibilityFacts.validate("sourceUrl", 2049) }
            },
            dynamicTest("F-CONFIG row074 reuseBasis") {
                CompatibilityFacts.validate("reuseBasis", 4000)
                assertThrows(ConfigurationViolation::class.java) { CompatibilityFacts.validate("reuseBasis", 4001) }
            },
            dynamicTest("F-CONFIG row075 input byte maximum") {
                assertEquals(
                    5242880,
                    ConfigurationSources().integer("ARTWORK_MAX_INPUT_BYTES", 5242880, 1, 5242880).value,
                )
                assertThrows(ConfigurationViolation::class.java) {
                    ConfigurationSources(
                        mapOf("ARTWORK_MAX_INPUT_BYTES" to "NaN"),
                    ).integer("ARTWORK_MAX_INPUT_BYTES", 5242880, 1, 5242880)
                }
            },
            dynamicTest("F-CONFIG row076 enabled formats") {
                assertEquals(
                    listOf("JPEG"),
                    PolicySettings
                        .resolve(
                            ConfigurationSources(
                                mapOf("ARTWORK_FORMATS" to "JPEG"),
                            ),
                        ).artwork.formats,
                )
                assertThrows(ConfigurationViolation::class.java) {
                    PolicySettings.resolve(
                        ConfigurationSources(
                            mapOf("ARTWORK_FORMATS" to ""),
                        ),
                    )
                }
            },
            dynamicTest("F-CONFIG row077 maxDimensionPx") {
                CompatibilityFacts.validate("maxDimensionPx", 8192)
                assertThrows(ConfigurationViolation::class.java) { CompatibilityFacts.validate("maxDimensionPx", 0) }
            },
            dynamicTest("F-CONFIG row078 maxPixels") {
                CompatibilityFacts.validate("maxPixels", 40000000)
                assertThrows(ConfigurationViolation::class.java) { CompatibilityFacts.validate("maxPixels", 0) }
            },
            dynamicTest("F-CONFIG row079 maxDecodedBytes") {
                CompatibilityFacts.validate("maxDecodedBytes", 160000000)
                assertThrows(ConfigurationViolation::class.java) { CompatibilityFacts.validate("maxDecodedBytes", 0) }
            },
            dynamicTest("F-CONFIG row080 processorRss") {
                CompatibilityFacts.validate("processorRss", 268435456)
                assertThrows(ConfigurationViolation::class.java) { CompatibilityFacts.validate("processorRss", 0) }
            },
            dynamicTest("F-CONFIG row081 processorCpu") {
                CompatibilityFacts.validate("processorCpu", 5)
                assertThrows(ConfigurationViolation::class.java) { CompatibilityFacts.validate("processorCpu", 0) }
            },
            dynamicTest("F-CONFIG row082 processorWall") {
                CompatibilityFacts.validate("processorWall", 10)
                assertThrows(ConfigurationViolation::class.java) { CompatibilityFacts.validate("processorWall", 0) }
            },
            dynamicTest("F-CONFIG row083 processorTempDisk") {
                CompatibilityFacts.validate("processorTempDisk", 33554432)
                assertThrows(ConfigurationViolation::class.java) { CompatibilityFacts.validate("processorTempDisk", 0) }
            },
        )

    @TestFactory
    fun configurationRows() = recipes
}

class ConfigurationInputRulesTest : FullConfigurationFixture() {
    private val recipes: List<DynamicTest> =
        listOf(
            dynamicTest("F-CONFIG row084 maxOutputBytes") {
                CompatibilityFacts.validate("maxOutputBytes", 8388608)
                assertThrows(ConfigurationViolation::class.java) { CompatibilityFacts.validate("maxOutputBytes", 0) }
            },
            dynamicTest("F-CONFIG row085 genre count maximum") {
                CompatibilityFacts.validate("genre count maximum", 20)
                assertThrows(
                    ConfigurationViolation::class.java,
                ) { CompatibilityFacts.validate("genre count maximum", 21) }
            },
            dynamicTest("F-CONFIG row086 BCP47 maximum characters") {
                CompatibilityFacts.validate("BCP47 maximum characters", 35)
                assertThrows(
                    ConfigurationViolation::class.java,
                ) { CompatibilityFacts.validate("BCP47 maximum characters", 36) }
            },
            dynamicTest("F-CONFIG row087 runtime minimum minutes") {
                CompatibilityFacts.validate("runtime minimum minutes", 1)
                assertThrows(
                    ConfigurationViolation::class.java,
                ) { CompatibilityFacts.validate("runtime minimum minutes", 2) }
            },
            dynamicTest("F-CONFIG row088 runtime maximum minutes") {
                CompatibilityFacts.validate("runtime maximum minutes", 100000)
                assertThrows(
                    ConfigurationViolation::class.java,
                ) { CompatibilityFacts.validate("runtime maximum minutes", 100001) }
            },
            dynamicTest("F-CONFIG row089 year minimum") {
                CompatibilityFacts.validate("year minimum", 1)
                assertThrows(ConfigurationViolation::class.java) { CompatibilityFacts.validate("year minimum", 2) }
            },
            dynamicTest("F-CONFIG row090 year maximum") {
                CompatibilityFacts.validate("year maximum", 9999)
                assertThrows(ConfigurationViolation::class.java) { CompatibilityFacts.validate("year maximum", 10000) }
            },
            dynamicTest("F-CONFIG row091 country tags") {
                assertEquals("ID", StructuralRules.country("ID"))
                assertThrows(InputViolation::class.java) { StructuralRules.country("id") }
                assertThrows(InputViolation::class.java) { StructuralRules.country("ZZ") }
            },
            dynamicTest("F-CONFIG row092 metadataBytes") {
                assertEquals(
                    131072,
                    ConfigurationSources().integer("REQUEST_METADATA_BYTES", 131072, 1, 2147483647).value,
                )
                assertThrows(ConfigurationViolation::class.java) {
                    ConfigurationSources(
                        mapOf("REQUEST_METADATA_BYTES" to "NaN"),
                    ).integer("REQUEST_METADATA_BYTES", 131072, 1, 2147483647)
                }
            },
            dynamicTest("F-CONFIG row093 uploadEnvelopeBytes") {
                assertEquals(
                    7100000,
                    ConfigurationSources().integer("REQUEST_UPLOAD_ENVELOPE_BYTES", 7100000, 1, 2147483647).value,
                )
                assertThrows(ConfigurationViolation::class.java) {
                    ConfigurationSources(
                        mapOf("REQUEST_UPLOAD_ENVELOPE_BYTES" to "NaN"),
                    ).integer("REQUEST_UPLOAD_ENVELOPE_BYTES", 7100000, 1, 2147483647)
                }
            },
            dynamicTest("F-CONFIG row094 graphqlQueryBytes") {
                assertEquals(
                    16384,
                    ConfigurationSources().integer("REQUEST_GRAPHQL_QUERY_BYTES", 16384, 1, 2147483647).value,
                )
                assertThrows(ConfigurationViolation::class.java) {
                    ConfigurationSources(
                        mapOf("REQUEST_GRAPHQL_QUERY_BYTES" to "NaN"),
                    ).integer("REQUEST_GRAPHQL_QUERY_BYTES", 16384, 1, 2147483647)
                }
            },
            dynamicTest("F-CONFIG row095 maxDepth") {
                assertEquals(8, ConfigurationSources().integer("REQUEST_MAX_DEPTH", 8, 1, 2147483647).value)
                assertThrows(ConfigurationViolation::class.java) {
                    ConfigurationSources(
                        mapOf("REQUEST_MAX_DEPTH" to "NaN"),
                    ).integer("REQUEST_MAX_DEPTH", 8, 1, 2147483647)
                }
            },
            dynamicTest("F-CONFIG row096 maxFields") {
                assertEquals(200, ConfigurationSources().integer("REQUEST_MAX_FIELDS", 200, 1, 2147483647).value)
                assertThrows(ConfigurationViolation::class.java) {
                    ConfigurationSources(
                        mapOf("REQUEST_MAX_FIELDS" to "NaN"),
                    ).integer("REQUEST_MAX_FIELDS", 200, 1, 2147483647)
                }
            },
            dynamicTest("F-CONFIG row097 maxAliases") {
                assertEquals(20, ConfigurationSources().integer("REQUEST_MAX_ALIASES", 20, 1, 2147483647).value)
                assertThrows(ConfigurationViolation::class.java) {
                    ConfigurationSources(
                        mapOf("REQUEST_MAX_ALIASES" to "NaN"),
                    ).integer("REQUEST_MAX_ALIASES", 20, 1, 2147483647)
                }
            },
            dynamicTest("F-CONFIG row098 maxCost") {
                assertEquals(1000, ConfigurationSources().integer("REQUEST_MAX_COST", 1000, 1, 2147483647).value)
                assertThrows(ConfigurationViolation::class.java) {
                    ConfigurationSources(
                        mapOf("REQUEST_MAX_COST" to "NaN"),
                    ).integer("REQUEST_MAX_COST", 1000, 1, 2147483647)
                }
            },
            dynamicTest("F-CONFIG row099 defaultPage") {
                assertEquals(24, ConfigurationSources().integer("REQUEST_DEFAULT_PAGE", 24, 1, 100).value)
                assertThrows(ConfigurationViolation::class.java) {
                    ConfigurationSources(
                        mapOf("REQUEST_DEFAULT_PAGE" to "NaN"),
                    ).integer("REQUEST_DEFAULT_PAGE", 24, 1, 100)
                }
            },
            dynamicTest("F-CONFIG row100 maxPage") {
                assertEquals(100, ConfigurationSources().integer("REQUEST_MAX_PAGE", 100, 1, 100).value)
                assertThrows(ConfigurationViolation::class.java) {
                    ConfigurationSources(
                        mapOf("REQUEST_MAX_PAGE" to "NaN"),
                    ).integer("REQUEST_MAX_PAGE", 100, 1, 100)
                }
            },
            dynamicTest("F-CONFIG row101 gRPC metadata message") {
                CompatibilityFacts.validate("gRPC metadata message", 262144)
                assertThrows(
                    ConfigurationViolation::class.java,
                ) { CompatibilityFacts.validate("gRPC metadata message", 262145) }
            },
            dynamicTest("F-CONFIG row102 gRPC list reply") {
                CompatibilityFacts.validate("gRPC list reply", 1048576)
                assertThrows(
                    ConfigurationViolation::class.java,
                ) { CompatibilityFacts.validate("gRPC list reply", 1048577) }
            },
            dynamicTest("F-CONFIG row103 upload chunk") {
                CompatibilityFacts.validate("upload chunk", 65536)
                assertThrows(ConfigurationViolation::class.java) { CompatibilityFacts.validate("upload chunk", 65537) }
            },
            dynamicTest("F-CONFIG row104 upload message") {
                CompatibilityFacts.validate("upload message", 131072)
                assertThrows(
                    ConfigurationViolation::class.java,
                ) { CompatibilityFacts.validate("upload message", 131073) }
            },
            dynamicTest("F-CONFIG row105 SIGNIN_ACCOUNT quota") {
                assertEquals(
                    10,
                    ConfigurationSources().integer("RATE_LIMIT_SIGNIN_ACCOUNT_MAX_REQUESTS", 10, 1, 2147483647).value,
                )
                assertThrows(ConfigurationViolation::class.java) {
                    ConfigurationSources(
                        mapOf("RATE_LIMIT_SIGNIN_ACCOUNT_MAX_REQUESTS" to "NaN"),
                    ).integer("RATE_LIMIT_SIGNIN_ACCOUNT_MAX_REQUESTS", 10, 1, 2147483647)
                }
            },
            dynamicTest("F-CONFIG row106 SIGNIN_ACCOUNT duration") {
                assertEquals(
                    900,
                    ConfigurationSources().integer("RATE_LIMIT_SIGNIN_ACCOUNT_WINDOW_SECONDS", 900, 1, 86400).value,
                )
                assertThrows(ConfigurationViolation::class.java) {
                    ConfigurationSources(
                        mapOf("RATE_LIMIT_SIGNIN_ACCOUNT_WINDOW_SECONDS" to "NaN"),
                    ).integer("RATE_LIMIT_SIGNIN_ACCOUNT_WINDOW_SECONDS", 900, 1, 86400)
                }
            },
            dynamicTest("F-CONFIG row107 SIGNIN_SOURCE quota") {
                assertEquals(
                    60,
                    ConfigurationSources().integer("RATE_LIMIT_SIGNIN_SOURCE_MAX_REQUESTS", 60, 1, 2147483647).value,
                )
                assertThrows(ConfigurationViolation::class.java) {
                    ConfigurationSources(
                        mapOf("RATE_LIMIT_SIGNIN_SOURCE_MAX_REQUESTS" to "NaN"),
                    ).integer("RATE_LIMIT_SIGNIN_SOURCE_MAX_REQUESTS", 60, 1, 2147483647)
                }
            },
            dynamicTest("F-CONFIG row108 SIGNIN_SOURCE duration") {
                assertEquals(
                    900,
                    ConfigurationSources().integer("RATE_LIMIT_SIGNIN_SOURCE_WINDOW_SECONDS", 900, 1, 86400).value,
                )
                assertThrows(ConfigurationViolation::class.java) {
                    ConfigurationSources(
                        mapOf("RATE_LIMIT_SIGNIN_SOURCE_WINDOW_SECONDS" to "NaN"),
                    ).integer("RATE_LIMIT_SIGNIN_SOURCE_WINDOW_SECONDS", 900, 1, 86400)
                }
            },
            dynamicTest("F-CONFIG row109 REGISTER quota") {
                assertEquals(
                    5,
                    ConfigurationSources().integer("RATE_LIMIT_REGISTER_MAX_REQUESTS", 5, 1, 2147483647).value,
                )
                assertThrows(ConfigurationViolation::class.java) {
                    ConfigurationSources(
                        mapOf("RATE_LIMIT_REGISTER_MAX_REQUESTS" to "NaN"),
                    ).integer("RATE_LIMIT_REGISTER_MAX_REQUESTS", 5, 1, 2147483647)
                }
            },
            dynamicTest("F-CONFIG row110 REGISTER duration") {
                assertEquals(
                    3600,
                    ConfigurationSources().integer("RATE_LIMIT_REGISTER_WINDOW_SECONDS", 3600, 1, 86400).value,
                )
                assertThrows(ConfigurationViolation::class.java) {
                    ConfigurationSources(
                        mapOf("RATE_LIMIT_REGISTER_WINDOW_SECONDS" to "NaN"),
                    ).integer("RATE_LIMIT_REGISTER_WINDOW_SECONDS", 3600, 1, 86400)
                }
            },
            dynamicTest("F-CONFIG row111 MUTATION quota") {
                assertEquals(
                    60,
                    ConfigurationSources().integer("RATE_LIMIT_MUTATION_MAX_REQUESTS", 60, 1, 2147483647).value,
                )
                assertThrows(ConfigurationViolation::class.java) {
                    ConfigurationSources(
                        mapOf("RATE_LIMIT_MUTATION_MAX_REQUESTS" to "NaN"),
                    ).integer("RATE_LIMIT_MUTATION_MAX_REQUESTS", 60, 1, 2147483647)
                }
            },
            dynamicTest("F-CONFIG row112 MUTATION duration") {
                assertEquals(
                    60,
                    ConfigurationSources().integer("RATE_LIMIT_MUTATION_WINDOW_SECONDS", 60, 1, 86400).value,
                )
                assertThrows(ConfigurationViolation::class.java) {
                    ConfigurationSources(
                        mapOf("RATE_LIMIT_MUTATION_WINDOW_SECONDS" to "NaN"),
                    ).integer("RATE_LIMIT_MUTATION_WINDOW_SECONDS", 60, 1, 86400)
                }
            },
            dynamicTest("F-CONFIG row113 READ quota") {
                assertEquals(
                    120,
                    ConfigurationSources().integer("RATE_LIMIT_READ_MAX_REQUESTS", 120, 1, 2147483647).value,
                )
                assertThrows(ConfigurationViolation::class.java) {
                    ConfigurationSources(
                        mapOf("RATE_LIMIT_READ_MAX_REQUESTS" to "NaN"),
                    ).integer("RATE_LIMIT_READ_MAX_REQUESTS", 120, 1, 2147483647)
                }
            },
            dynamicTest("F-CONFIG row114 READ duration") {
                assertEquals(60, ConfigurationSources().integer("RATE_LIMIT_READ_WINDOW_SECONDS", 60, 1, 86400).value)
                assertThrows(ConfigurationViolation::class.java) {
                    ConfigurationSources(
                        mapOf("RATE_LIMIT_READ_WINDOW_SECONDS" to "NaN"),
                    ).integer("RATE_LIMIT_READ_WINDOW_SECONDS", 60, 1, 86400)
                }
            },
            dynamicTest("F-CONFIG row115 Core rate MAC key reference") {
                assertEquals(
                    "/private/synthetic-file",
                    ConfigurationSources(
                        mapOf("RATE_LIMIT_KEY_FILE" to "/private/synthetic-file"),
                    ).required("RATE_LIMIT_KEY_FILE").value,
                )
                assertThrows(
                    ConfigurationViolation::class.java,
                ) { ConfigurationSources().required("RATE_LIMIT_KEY_FILE") }
            },
            dynamicTest("F-CONFIG row116 MCP local binding capacity") {
                assertEquals(1024, ConfigurationSources().integer("MCP_RATE_LIMIT_MAX_BINDINGS", 1024, 1, 100000).value)
                assertThrows(ConfigurationViolation::class.java) {
                    ConfigurationSources(
                        mapOf("MCP_RATE_LIMIT_MAX_BINDINGS" to "NaN"),
                    ).integer("MCP_RATE_LIMIT_MAX_BINDINGS", 1024, 1, 100000)
                }
            },
            dynamicTest("F-CONFIG row117 level") {
                assertEquals("info", ConfigurationSources().choice("LOG_LEVEL", "info", setOf("debug", "info")).value)
                assertThrows(ConfigurationViolation::class.java) {
                    ConfigurationSources(
                        mapOf("LOG_LEVEL" to "trace"),
                    ).choice("LOG_LEVEL", "info", setOf("debug", "info"))
                }
            },
            dynamicTest("F-CONFIG row118 enabled for configured ChatGPT verification") {
                assertEquals(true, ConfigurationSources().boolean("TUNNEL_ENABLED", true).value)
                assertThrows(ConfigurationViolation::class.java) {
                    ConfigurationSources(
                        mapOf("TUNNEL_ENABLED" to "False"),
                    ).boolean("TUNNEL_ENABLED", true)
                }
            },
            dynamicTest("F-CONFIG row119 tunnel identity") { conditionalReference("TUNNEL_ID", "TUNNEL_ENABLED") },
            dynamicTest(
                "F-CONFIG row120 runtime key reference",
            ) { conditionalReference("TUNNEL_KEY_FILE", "TUNNEL_ENABLED") },
            dynamicTest("F-CONFIG row121 runner path") { conditionalReference("TUNNEL_RUNNER_PATH", "TUNNEL_ENABLED") },
            dynamicTest("F-CONFIG row122 publication enabled") {
                assertEquals(false, ConfigurationSources().boolean("MCP_PUBLICATION_ENABLED", false).value)
                assertThrows(ConfigurationViolation::class.java) {
                    ConfigurationSources(
                        mapOf("MCP_PUBLICATION_ENABLED" to "False"),
                    ).boolean("MCP_PUBLICATION_ENABLED", false)
                }
            },
            dynamicTest(
                "F-CONFIG row123 MCP_UI_ASSERTION_KEY_FILE",
            ) { conditionalReference("MCP_UI_ASSERTION_KEY_FILE", "MCP_PUBLICATION_ENABLED") },
            dynamicTest(
                "F-CONFIG row124 MCP_UI_ASSERTION_KEY_ID",
            ) { conditionalReference("MCP_UI_ASSERTION_KEY_ID", "MCP_PUBLICATION_ENABLED") },
            dynamicTest("F-CONFIG row125 old-key read overlap") {
                CompatibilityFacts.validate("old-key read overlap", 60)
                assertThrows(
                    ConfigurationViolation::class.java,
                ) { CompatibilityFacts.validate("old-key read overlap", 61) }
            },
        )

    @TestFactory
    fun configurationRows() = recipes
}
