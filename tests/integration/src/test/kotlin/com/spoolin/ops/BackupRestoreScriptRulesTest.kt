package com.spoolin.ops

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path

/** Source-level checks for backup/restore script contracts (T041). */
class BackupRestoreScriptRulesTest {
    @Test
    fun backup_script_supports_dry_run_default() {
        val script = repoRoot().resolve("ops/bin/backup.sh")
        val text = script.toFile().readText()
        assertTrue(text.contains("dry_run=1"))
        assertTrue(text.contains("pg_dump"))
    }

    @Test
    fun restore_script_requires_input_dump() {
        val script = repoRoot().resolve("ops/bin/restore.sh")
        val text = script.toFile().readText()
        assertTrue(text.contains("pg_restore"))
        assertTrue(text.contains("--input"))
    }

    private fun repoRoot(): Path =
        Path.of(System.getProperty("user.dir")).let { cwd ->
            if (cwd.endsWith("tests/integration")) cwd.parent.parent else cwd
        }
}
