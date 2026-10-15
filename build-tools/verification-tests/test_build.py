"""Actual locked compile cycles; generated fixture outputs must match byte for byte."""

from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import time
import unittest
from run_owner_integration import junit_counts

ROOT = Path(__file__).resolve().parents[2]
EVIDENCE = ROOT / ".ai/evidence/t001-build"
COMMANDS = []


def run_command(argv, label, timeout=600):
    EVIDENCE.mkdir(parents=True, exist_ok=True)
    started = datetime.now(timezone.utc).isoformat()
    environment = {key: value for key, value in os.environ.items() if key in (
        "PATH", "JAVA_HOME", "GRADLE_USER_HOME", "HOME", "TMPDIR", "LANG", "LC_ALL")}
    environment["npm_config_userconfig"] = os.devnull
    environment["npm_config_engine_strict"] = "true"
    try:
        process = subprocess.run(argv, cwd=ROOT, env=environment, capture_output=True, timeout=timeout)
    except subprocess.TimeoutExpired as error:
        process = subprocess.CompletedProcess(argv, 124, error.stdout or b"", (error.stderr or b"") + b"\nCommand timed out\n")
    log = EVIDENCE / f"{label}.txt"
    log.write_bytes(process.stdout + process.stderr)
    COMMANDS.append({"argv": argv, "cwd": str(ROOT), "started_at_utc": started,
                     "finished_at_utc": datetime.now(timezone.utc).isoformat(),
                     "exit_code": process.returncode, "log": str(log.relative_to(ROOT)),
                     "log_sha256": hashlib.sha256(log.read_bytes()).hexdigest()})
    if process.returncode:
        print(log.read_text(errors="replace")[-6000:], file=sys.stderr)
    return process


def digest_files(paths):
    files = []
    for path in paths:
        files.extend([path] if path.is_file() else sorted(path.rglob("*")))
    return {str(path.relative_to(ROOT)): hashlib.sha256(path.read_bytes()).hexdigest()
            for path in files if path.is_file()}


def locked_inputs():
    paths = [ROOT / "package-lock.json", ROOT / "gradle/wrapper/gradle-wrapper.properties",
             ROOT / "gradle/verification-metadata.xml", ROOT / "gradle/libs.versions.toml"]
    paths += sorted(ROOT.joinpath("gradle").rglob("*.lockfile"))
    paths += sorted(ROOT.joinpath("contracts").rglob("gradle.lockfile"))
    paths += sorted(ROOT.joinpath("build-tools/verification-tests/jvm/proto").rglob("*.proto"))
    paths += sorted(ROOT.joinpath("build-tools/verification-tests/graphql").rglob("*.graphql"))
    return digest_files(paths)


def check_prerequisites():
    for command in ("node", "npm"):
        if shutil.which(command) is None:
            return f"Missing required executable: {command}"
    node = run_command(["node", "--version"], "node-version")
    npm = run_command(["npm", "--version"], "npm-version")
    java_executable = Path(os.environ.get("JAVA_HOME", "")) / "bin/java"
    if not java_executable.is_file():
        return "Select Eclipse Temurin 21.0.12.1+1 through JAVA_HOME"
    java_path = str(java_executable)
    java = run_command([java_path, "-version"], "java-version")
    if node.stdout.strip() != b"v24.21.0" or npm.stdout.strip() != b"11.19.0":
        return "Select Node 24.21.0 and npm 11.19.0 on PATH"
    if b"Temurin-21.0.12.1+1" not in java.stderr + java.stdout:
        return "Select Eclipse Temurin 21.0.12.1+1 through JAVA_HOME and PATH"
    if not ROOT.joinpath("gradlew").is_file():
        return "Missing required Gradle wrapper"
    return None


class LockedBuildTest(unittest.TestCase):
    def test_approved_security_overrides_preserve_selected_managed_versions(self):
        expected = {
            "org.jetbrains.kotlin:kotlin-stdlib": "2.4.20",
            "org.jetbrains.kotlin:kotlin-reflect": "2.4.20",
            "org.apache.tomcat.embed:tomcat-embed-core": "11.0.26",
            "org.apache.tomcat.embed:tomcat-embed-el": "11.0.26",
            "org.apache.tomcat.embed:tomcat-embed-websocket": "11.0.26",
            "tools.jackson:jackson-bom": "3.1.7",
            "tools.jackson.core:jackson-core": "3.1.7",
            "tools.jackson.core:jackson-databind": "3.1.7",
            "com.fasterxml.jackson.core:jackson-annotations": "2.21",
            "org.springframework.boot:spring-boot": "4.1.1",
            "org.springframework.grpc:spring-grpc-core": "1.1.1",
            "org.hibernate.orm:hibernate-core": "7.4.5.Final",
            "org.flywaydb:flyway-core": "12.4.0",
            "io.grpc:grpc-api": "1.83.1",
            "com.google.protobuf:protobuf-java": "4.35.1",
            "org.postgresql:postgresql": "42.7.13",
        }
        for owner in ("core", "catalog", "media"):
            lines = ROOT.joinpath(f"gradle/locks/_services_{owner}/gradle.lockfile").read_text().splitlines()
            selected = {line.split("=", 1)[0].rsplit(":", 1)[0]: line.split("=", 1)[0].rsplit(":", 1)[1]
                        for line in lines if "=" in line and not line.startswith(("#", "empty="))
                        and "runtimeClasspath" in line.split("=", 1)[1].split(",")}
            for package, version in expected.items():
                self.assertEqual(selected.get(package), version, f"{owner}: {package}")

    def check_command(self, argv, label):
        result = run_command(argv, label)
        self.assertEqual(result.returncode, 0, f"Command failed; see {label}.txt")

    def build_cycle(self, number):
        prefix = f"cycle-{number}"
        shutil.rmtree(ROOT / "build-tools/verification-tests/frontend/.svelte-kit", ignore_errors=True)
        self.check_command(["npm", "ci", "--ignore-scripts", "--strict-peer-deps"], f"{prefix}-npm-ci")
        for script in ("generate:fixture", "lint", "check", "format:check", "build"):
            self.check_command(["npm", "run", script], f"{prefix}-npm-{script.replace(':', '-')}")
        for workspace in ("frontend", "mcp"):
            self.check_command(["npm", "run", "check:dependencies", "--workspace", workspace], f"{prefix}-{workspace}-dependencies")
        tasks = [":clean", ":test", ":jar", ":ktlintCheck", ":detekt", ":generateTsFixture", ":koverXmlReport"]
        tasks += [f":services:{owner}:classes" for owner in ("core", "catalog", "media")]
        started_ns = time.time_ns()
        self.check_command(["./gradlew", *tasks, "--no-daemon", "--no-build-cache", "--rerun-tasks", "--console", "plain"], f"{prefix}-gradle")
        reports = sorted(ROOT.glob("build/test-results/test/TEST-*.xml"))
        counts = junit_counts(reports, started_ns)
        self.assertEqual(counts["selected"], counts["executed"], "Skipped JVM tests cannot pass")
        self.assertEqual(counts["failed"], 0)
        archived = EVIDENCE / f"{prefix}-junit"
        archived.mkdir(exist_ok=True)
        for report in reports:
            shutil.copy2(report, archived / report.name)
        self.check_command(["node_modules/.bin/tsc", "--noEmit", "--strict", "--types", "node", "--target", "ES2024", "--module", "NodeNext", "--moduleResolution", "NodeNext", "build/generated/ts-fixture/fixture.ts", "build-tools/verification-tests/graphql/build/types.ts"], f"{prefix}-generated-ts")
        return digest_files([ROOT / "build/generated/sources/proto", ROOT / "build/generated/ts-fixture", ROOT / "build-tools/verification-tests/graphql/build", ROOT / "build/libs",
                             ROOT / "build-tools/verification-tests/frontend/build", ROOT / "mcp/dist"])

    def test_clean_locked_build_twice_preserves_inputs_and_outputs(self):
        before = locked_inputs()
        self.assertGreater(len(before), 3, "Project and plugin lockfiles must exist")
        first = self.build_cycle(1)
        self.assertTrue(any(path.endswith(".java") for path in first), "Actual protobuf/gRPC generation is required")
        self.assertTrue(any(path.endswith("200.html") for path in first), "Static SPA fallback must be built")
        second = self.build_cycle(2)
        self.assertEqual(first, second, "Clean builds produced different generated/package bytes")
        self.assertEqual(before, locked_inputs(), "A locked build changed its source inputs")
        self.assertFalse(ROOT.joinpath("settings-gradle.lockfile").exists(), "A normal locked build wrote settings inventory")
        EVIDENCE.joinpath("reproducibility.json").write_text(json.dumps({
            "cycles": 2, "source_inputs": before, "first_outputs": first,
            "second_outputs": second, "case": "T001-A1",
        }, indent=2) + "\n")


def main():
    EVIDENCE.mkdir(parents=True, exist_ok=True)
    issue = check_prerequisites()
    if issue:
        print(f"Setup unavailable: {issue}", file=sys.stderr)
        counts = {"selected": 1, "executed": 0, "failed": 0, "setup_error": issue}
        exit_code = 2
    else:
        suite = unittest.defaultTestLoader.loadTestsFromTestCase(LockedBuildTest)
        result = unittest.TextTestRunner(verbosity=2).run(suite)
        counts = {"selected": suite.countTestCases(), "executed": result.testsRun,
                  "failed": len(result.errors) + len(result.failures), "cases": ["T001-A1"]}
        exit_code = 0 if result.wasSuccessful() else 1
    ROOT.joinpath(".ai/evidence/t001-build.json").write_text(json.dumps(counts, indent=2) + "\n")
    EVIDENCE.joinpath("commands.json").write_text(json.dumps(COMMANDS, indent=2) + "\n")
    return exit_code


if __name__ == "__main__":
    sys.exit(main())
