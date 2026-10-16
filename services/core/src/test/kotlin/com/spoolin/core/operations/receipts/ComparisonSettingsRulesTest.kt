package com.spoolin.core.operations.receipts

import com.spoolin.support.receipts.OperationIntent
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.util.UUID

class ComparisonSettingsRulesTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `native bracket map preserves exact key IDs and merges retained lower source`() {
        val old = key("old", 1)
        val low = key("low", 2)
        val high = key("high", 3)
        val environment = StandardEnvironment()
        environment.propertySources.addFirst(
            MapPropertySource(
                "mounted",
                mapOf(
                    "spoolin.operations.comparison-keys[Old_A]" to old,
                    "spoolin.operations.comparison-keys[active]" to low,
                ),
            ),
        )
        environment.propertySources.addFirst(
            MapPropertySource(
                "explicit",
                mapOf(
                    "spoolin.operations.comparison-keys[active]" to high,
                    "OPERATION_HMAC_KEY_ID" to "active",
                    "OPERATION_HMAC_KEY_FILE" to high,
                ),
            ),
        )
        val hashes = ComparisonKeySettings.load(environment)
        val intent = OperationIntent(UUID.randomUUID(), "account:A", "DRAFT_SAVE", "p1", emptyList())
        val reference =
            com.spoolin.support.receipts.ComparisonKeys
                .load(mapOf("Old_A" to old), "Old_A", old)
        assertArrayEquals(reference.hash(intent, "Old_A"), hashes.hash(intent, "Old_A"))
        assertThrows(IllegalStateException::class.java) { hashes.hash(intent, "old_a") }
    }

    @Test
    fun `present invalid root entry or active selector cannot fall back and diagnostics are redacted`() {
        val good = key("good", 4)
        val variants =
            listOf(
                mapOf("spoolin.operations.comparison-keys" to "invalid-root"),
                mapOf("spoolin.operations.comparison-keys[active]" to ""),
                mapOf("OPERATION_HMAC_KEY_ID" to ""),
                mapOf("OPERATION_HMAC_KEY_FILE" to ""),
            )
        variants.forEach { invalid ->
            val environment = StandardEnvironment()
            environment.propertySources.addFirst(
                MapPropertySource(
                    "mounted",
                    mapOf(
                        "spoolin.operations.comparison-keys[active]" to good,
                        "OPERATION_HMAC_KEY_ID" to "active",
                        "OPERATION_HMAC_KEY_FILE" to good,
                    ),
                ),
            )
            environment.propertySources.addFirst(MapPropertySource("explicit", invalid))
            val failure = assertThrows(IllegalArgumentException::class.java) { ComparisonKeySettings.load(environment) }
            assertFalse(failure.message.orEmpty().contains(good))
        }
    }

    @Test
    fun `actual Spring application JSON source preserves exact IDs above mounted entries`() {
        val old = key("json-old", 11)
        val active = key("json-active", 12)
        val environment = StandardEnvironment()
        environment.propertySources.addFirst(
            MapPropertySource(
                "mounted",
                mapOf(
                    "spoolin.operations.comparison-keys[Retained_B]" to key("retained", 13),
                ),
            ),
        )
        val json =
            tools.jackson.databind.ObjectMapper().writeValueAsString(
                mapOf(
                    "spoolin" to
                        mapOf(
                            "operations" to mapOf("comparison-keys" to mapOf("active" to active, "Old_A" to old)),
                        ),
                ),
            )
        environment.propertySources.addFirst(
            MapPropertySource(
                "explicit",
                mapOf(
                    "SPRING_APPLICATION_JSON" to json,
                    "OPERATION_HMAC_KEY_ID" to "active",
                    "OPERATION_HMAC_KEY_FILE" to active,
                ),
            ),
        )
        org.springframework.boot.support
            .SpringApplicationJsonEnvironmentPostProcessor()
            .postProcessEnvironment(environment, org.springframework.boot.SpringApplication())
        val hashes = ComparisonKeySettings.load(environment)
        val intent = OperationIntent(UUID.randomUUID(), "account:A", "DRAFT_SAVE", "p1", emptyList())
        val expected =
            com.spoolin.support.receipts.ComparisonKeys
                .load(mapOf("Old_A" to old), "Old_A", old)
        assertArrayEquals(expected.hash(intent, "Old_A"), hashes.hash(intent, "Old_A"))
    }

    @Test
    fun `duplicate JSON key IDs are refused after native source processing`() {
        val file = key("duplicate", 17)
        val quoted =
            tools.jackson.databind
                .ObjectMapper()
                .writeValueAsString(file)
        val raw = "{\"spoolin\":{\"operations\":{\"comparison-keys\":{\"active\":$quoted,\"active\":$quoted}}}}"
        val environment = StandardEnvironment()
        environment.propertySources.addFirst(
            MapPropertySource(
                "explicit",
                mapOf(
                    "SPRING_APPLICATION_JSON" to raw,
                    "OPERATION_HMAC_KEY_ID" to "active",
                    "OPERATION_HMAC_KEY_FILE" to file,
                ),
            ),
        )
        org.springframework.boot.support
            .SpringApplicationJsonEnvironmentPostProcessor()
            .postProcessEnvironment(environment, org.springframework.boot.SpringApplication())
        val problem = assertThrows(IllegalArgumentException::class.java) { ComparisonKeySettings.load(environment) }
        assertFalse(problem.message.orEmpty().contains(file))
    }

    private fun key(
        name: String,
        value: Int,
    ): String {
        val file = directory.resolve(name)
        Files.write(file, ByteArray(32) { value.toByte() })
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"))
        return file.toString()
    }
}
