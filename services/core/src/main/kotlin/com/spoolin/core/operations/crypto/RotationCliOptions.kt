package com.spoolin.core.operations.crypto

import com.spoolin.core.features.crypto.KeyPurpose
import java.nio.file.Path

internal data class RotationCliOptions(
    val from: String,
    val to: String,
    val purpose: KeyPurpose,
    val keys: Path,
    val dryRun: Boolean,
    val register: Boolean,
    val maxBatches: Int,
) {
    companion object {
        fun parse(
            args: Array<String>,
            defaultFile: String?,
        ): RotationCliOptions {
            val values = mutableMapOf<String, String>()
            val flags = mutableSetOf<String>()
            var index = 0
            while (index < args.size) {
                val name = args[index++]
                if (name in setOf("--dry-run", "--register-target")) {
                    require(flags.add(name)) { "Duplicate option" }
                } else {
                    require(name in setOf("--from", "--to", "--purpose", "--keys", "--max-batches")) {
                        "Unknown option"
                    }
                    require(index < args.size && !args[index].startsWith("--")) { "Missing option value" }
                    require(values.put(name, args[index++]) == null) { "Duplicate option" }
                }
            }
            return checked(values, flags, defaultFile)
        }

        private fun checked(
            values: Map<String, String>,
            flags: Set<String>,
            defaultFile: String?,
        ): RotationCliOptions {
            val from = requireNotNull(values["--from"])
            val to = requireNotNull(values["--to"])
            require(from != to && listOf(from, to).all { it.matches(Regex("[A-Za-z0-9_-]{1,64}")) })
            val purpose = KeyPurpose.valueOf(values["--purpose"] ?: "DRAFT")
            val keys = Path.of(requireNotNull(values["--keys"] ?: defaultFile))
            require(keys.isAbsolute && keys.normalize() == keys)
            val batches = batches(values["--max-batches"] ?: "100")
            return RotationCliOptions(
                from,
                to,
                purpose,
                keys,
                "--dry-run" in flags,
                "--register-target" in flags,
                batches,
            )
        }

        private fun batches(value: String): Int {
            require(value.matches(Regex("[1-9][0-9]{0,4}")))
            val batches = value.toInt()
            require(batches in 1..MAX_ROTATION_BATCHES)
            return batches
        }
    }
}
