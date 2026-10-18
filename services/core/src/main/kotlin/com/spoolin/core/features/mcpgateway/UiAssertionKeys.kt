package com.spoolin.core.features.mcpgateway

import org.springframework.core.env.Environment
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.security.MessageDigest
import java.time.Clock

/** One active key and one explicitly timed previous read key; restart never extends its deadline. */
class UiAssertionKeys private constructor(
    private val activeId: String,
    private val active: ByteArray,
    private val previousId: String?,
    private var previous: ByteArray?,
    private val rotatedAt: Long?,
) : AutoCloseable {
    private var closed = false

    @Synchronized
    fun keyFor(id: String, now: Long): ByteArray? {
        if (closed) return null
        if (rotatedAt != null && now >= rotatedAt + 60) { previous?.fill(0); previous = null }
        if (id == activeId) return active.copyOf()
        if (id != previousId || rotatedAt == null || now < rotatedAt || now >= rotatedAt + 60) return null
        return previous?.copyOf()
    }

    @Synchronized
    override fun close() { closed = true; active.fill(0); previous?.fill(0); previous = null }

    companion object {
        fun single(id: String, key: ByteArray): UiAssertionKeys {
            requireKey(id, key)
            return UiAssertionKeys(id, key.copyOf(), null, null, null)
        }

        fun rotated(id: String, key: ByteArray, previousId: String, previous: ByteArray, rotatedAt: Long, now: Long): UiAssertionKeys {
            requireKey(id, key)
            requireKey(previousId, previous)
            require(id != previousId && !MessageDigest.isEqual(key, previous))
            require(rotatedAt in 1..(Long.MAX_VALUE - 60) && rotatedAt <= now) { "Invalid absolute UI key rotation time" }
            return UiAssertionKeys(id, key.copyOf(), previousId, previous.copyOf(), rotatedAt)
        }

        fun load(environment: Environment, clock: Clock = Clock.systemUTC()): UiAssertionKeys {
            val id = environment.getRequiredProperty("MCP_UI_ASSERTION_KEY_ID")
            val active = read(Path.of(environment.getRequiredProperty("MCP_UI_ASSERTION_KEY_FILE")))
            val names = listOf("MCP_UI_ASSERTION_PREVIOUS_KEY_ID", "MCP_UI_ASSERTION_PREVIOUS_KEY_FILE", "MCP_UI_ASSERTION_ROTATED_AT")
            val prior = names.map { environment.getProperty(it) }
            try {
                distinctPurpose(environment, active)
                if (prior.all { it == null }) return single(id, active)
                require(prior.all { !it.isNullOrBlank() }) { "Supply the complete previous UI key and absolute rotation time" }
                val old = read(Path.of(requireNotNull(prior[1])))
                try {
                    distinctPurpose(environment, old)
                    return rotated(id, active, requireNotNull(prior[0]), old, requireNotNull(prior[2]).toLong(), clock.instant().epochSecond)
                } finally { old.fill(0) }
            } finally { active.fill(0) }
        }

        private fun requireKey(id: String, key: ByteArray) {
            require(id.matches(Regex("[A-Za-z0-9_-]{1,64}")) && key.size == 32) { "Invalid private UI key configuration" }
        }

        private fun distinctPurpose(environment: Environment, key: ByteArray) {
            for (name in listOf("CSRF_KEY_FILE", "RATE_LIMIT_KEY_FILE", "CURSOR_KEY_FILE", "OPERATION_HMAC_KEY_FILE")) {
                val file = environment.getProperty(name) ?: continue
                val other = read(Path.of(file))
                try { require(!MessageDigest.isEqual(key, other)) { "UI assertion material must have an independent purpose" } }
                finally { other.fill(0) }
            }
        }

        private fun read(path: Path): ByteArray {
            require(path.isAbsolute && Files.isRegularFile(path, NOFOLLOW_LINKS) && Files.size(path) == 32L) {
                "Supply a protected32-byte UI assertion key"
            }
            require(Files.getPosixFilePermissions(path) == PosixFilePermissions.fromString("rw-------")) {
                "UI assertion keys require0600 permissions"
            }
            return Files.newInputStream(path, java.nio.file.StandardOpenOption.READ, NOFOLLOW_LINKS).use { input ->
                input.readNBytes(33).also { require(it.size == 32) }
            }
        }
    }
}
