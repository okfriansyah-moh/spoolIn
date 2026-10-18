package com.spoolin.ops

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.util.concurrent.TimeUnit

class BackupRestoreScriptRulesTest {
    @Test
    fun backup_script_supports_dry_run_default() {
        val program =
            """
            import importlib.util
            import sys
            spec = importlib.util.spec_from_file_location('checkpoint', 'ops/bin/checkpoint.py')
            checkpoint = importlib.util.module_from_spec(spec)
            spec.loader.exec_module(checkpoint)
            checkpoint.Installation = object
            observed = []
            checkpoint.backup = lambda installation, arguments: observed.append(arguments.execute)
            sys.argv = ['checkpoint.py', 'backup', '--output', 'unused-dry-run-directory']
            checkpoint.main()
            assert observed == [False], observed
            print('dry-run default verified')
            """.trimIndent()
        val (code, output) = command("python3", "-c", program)
        assertEquals(0, code, output)
        assertTrue(output.contains("dry-run default verified"))
    }

    @Test
    fun restore_script_requires_input_dump() {
        val (code, output) = command("bash", "ops/bin/restore.sh")
        assertEquals(2, code, output)
        assertTrue(output.contains("restore requires --input DIRECTORY"))
    }

    private fun command(vararg arguments: String): Pair<Int, String> {
        val process = ProcessBuilder(*arguments).directory(repoRoot().toFile()).redirectErrorStream(true).start()
        check(process.waitFor(30, TimeUnit.SECONDS)) { "Checkpoint command exceeded its deadline" }
        return process.exitValue() to process.inputStream.bufferedReader().readText()
    }

    private fun repoRoot(): Path =
        Path.of(System.getProperty("user.dir")).let { cwd ->
            if (cwd.endsWith("tests/integration")) cwd.parent.parent else cwd
        }
}
