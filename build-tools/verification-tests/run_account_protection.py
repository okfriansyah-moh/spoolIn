"""Execute account suites and record only fresh JUnit results at their actual boundary."""

import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

from fixture import SetupError, TestInstallation, preserve_failure

ROOT = Path(__file__).resolve().parents[2]


def invoke(level, fixture, selection="T005"):
    task = "test" if level == "unit" else "cryptoIntegrationTest" if selection == "T005" else "accountIntegrationTest"
    argv = ["./gradlew", f":services:core:{task}", "--no-daemon", "--rerun-tasks", "--console", "plain"]
    if selection == "T008":
        argv = ["./gradlew", f":services:core:{task}", "--console", "plain"]
    if level == "unit":
        patterns = ["com.spoolin.core.features.*"] if selection == "T005" else [
            "com.spoolin.core.features.session.*", "*IdentityRulesTest", "*PasswordRulesTest",
            "*PolicyBoundaryRulesTest", "*OperationGraphqlRulesTest", "com.spoolin.core.boundary.http.*"]
        for pattern in patterns:
            argv.extend(["--tests", pattern])
    environment = {key: value for key, value in os.environ.items() if key in (
        "PATH", "JAVA_HOME", "GRADLE_USER_HOME", "HOME", "TMPDIR", "LANG", "LC_ALL",
        "DOCKER_HOST", "DOCKER_CONTEXT", "DOCKER_TLS_VERIFY", "DOCKER_CERT_PATH")}
    if fixture:
        config = fixture.root / "config.json"
        config.write_text(json.dumps({**fixture.configuration(), "synthetic_secrets": str(fixture.root / "private")}))
        config.chmod(0o600)
        environment.update({"SPOOLIN_TEST_NAMESPACE": fixture.namespace, "SPOOLIN_TEST_CONFIG": str(config),
                            "TESTCONTAINERS_REUSE_ENABLE": "false"})
    directory = ROOT / f"services/core/build/test-results/{task}"
    for old in directory.glob("TEST-*.xml"):
        old.unlink()
    started = time.time_ns()
    result = subprocess.run(argv, cwd=ROOT, env=environment, capture_output=True, timeout=1500)
    log = ROOT / f".ai/evidence/numbering-v1/{selection.lower()}-{level}-command.log"
    log.write_bytes(result.stdout + result.stderr)
    return result.returncode, started, directory, {
        "argv": argv, "exit_code": result.returncode, "started_ns": started,
        "log": str(log.relative_to(ROOT)), "log_sha256": hashlib.sha256(log.read_bytes()).hexdigest()}


def results(directory, started):
    files = sorted(directory.glob("TEST-*.xml"))
    if not files or any(path.stat().st_mtime_ns < started for path in files):
        raise SetupError("Fresh account JUnit reports are required")
    cases = [case for path in files for case in ET.parse(path).getroot().iter("testcase")]
    if not cases:
        raise SetupError("Required account suite is empty")
    counts = {"selected": len(cases), "executed": sum(case.find("skipped") is None for case in cases),
              "failed": sum(case.find("failure") is not None or case.find("error") is not None for case in cases)}
    evidence = [{"path": str(path.relative_to(ROOT)), "sha256": hashlib.sha256(path.read_bytes()).hexdigest()} for path in files]
    selected = [{"class": case.attrib["classname"], "method": case.attrib["name"],
                 "passed": case.find("failure") is None and case.find("error") is None and case.find("skipped") is None} for case in cases]
    return counts, evidence, selected


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--task", choices=["T005", "T008"], required=True)
    parser.add_argument("--level", choices=["unit", "integration"], required=True)
    arguments = parser.parse_args()
    report = ROOT / f".ai/evidence/numbering-v1/{arguments.task.lower()}-{arguments.level}-results.json"
    report.parent.mkdir(parents=True, exist_ok=True)
    report.unlink(missing_ok=True)
    counts = {"selected": 1, "executed": 0, "failed": 0}
    evidence = {}
    fixture = None
    code = 2
    try:
        if arguments.level == "integration" and arguments.task == "T005":
            fixture = TestInstallation.create()
        exit_code, started, directory, command = invoke(arguments.level, fixture, arguments.task)
        evidence["command"] = command
        counts, reports, cases = results(directory, started)
        evidence.update({"reports": reports, "cases": cases,
                         "proof_boundary": "isolated owner rules" if arguments.level == "unit" else "Core PostgreSQL18.6, actual processes/files/Argon2id"})
        code = 1 if exit_code or counts["failed"] else 2 if counts["selected"] != counts["executed"] else 0
    except (SetupError, OSError, ET.ParseError, subprocess.TimeoutExpired) as error:
        print(f"Setup unavailable: {error}", file=sys.stderr)
        evidence["setup_error"] = str(error)
    finally:
        if fixture:
            code = preserve_failure(code, fixture.reset)
    report.write_text(json.dumps({**counts, **evidence, "exit_code": code,
                                  "finished_at_utc": datetime.now(timezone.utc).isoformat()}, indent=2) + "\n")
    return code


if __name__ == "__main__":
    sys.exit(main())
