"""Run actual owner suites and require fresh, nonempty JUnit results for each owner."""

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
OWNERS = ("core", "catalog", "media")
REPORT = ROOT / ".ai/evidence/numbering-v1/t003-owner-tests.json"


def junit_counts(paths, started_ns):
    if not paths or any(path.stat().st_mtime_ns < started_ns for path in paths):
        raise SetupError("Fresh owner JUnit reports are required")
    cases = []
    for path in paths:
        cases.extend(ET.parse(path).getroot().iter("testcase"))
    if not cases:
        raise SetupError("Owner JUnit selection is empty")
    return {"selected": len(cases),
            "executed": sum(case.find("skipped") is None for case in cases),
            "failed": sum(case.find("failure") is not None or case.find("error") is not None for case in cases)}


def prerequisites():
    for owner in OWNERS:
        source = ROOT / f"services/{owner}/src/integrationTest/kotlin/com/spoolin/{owner}/boundary/bootstrap"
        if not any(source.rglob("*.kt")):
            raise SetupError(f"Required {owner} bootstrap suite is pending")
    try:
        result = subprocess.run(["docker", "info", "--format", "{{.ServerVersion}}"], capture_output=True, timeout=15)
    except (OSError, subprocess.TimeoutExpired) as error:
        raise SetupError("Docker is unavailable for the disposable owner fixture") from error
    if result.returncode:
        raise SetupError("Docker is unavailable for the disposable owner fixture")


def owner_results(started_ns):
    totals = {"selected": 0, "executed": 0, "failed": 0}
    reports = []
    for owner in OWNERS:
        paths = sorted(ROOT.glob(f"services/{owner}/build/test-results/integrationTest/TEST-*.xml"))
        counts = junit_counts(paths, started_ns)
        for key in totals:
            totals[key] += counts[key]
        reports.extend({"path": str(path.relative_to(ROOT)), "sha256": hashlib.sha256(path.read_bytes()).hexdigest()}
                       for path in paths)
    return totals, reports


def invoke(fixture):
    environment = {key: value for key, value in os.environ.items() if key in (
        "PATH", "JAVA_HOME", "GRADLE_USER_HOME", "HOME", "TMPDIR", "LANG", "LC_ALL",
        "DOCKER_HOST", "DOCKER_CONTEXT", "DOCKER_TLS_VERIFY", "DOCKER_CERT_PATH")}
    config = fixture.root / "config.json"
    config.write_text(json.dumps({**fixture.configuration(), "synthetic_secrets": str(fixture.root / "private")}))
    config.chmod(0o600)
    environment.update({"SPOOLIN_TEST_NAMESPACE": fixture.namespace, "SPOOLIN_TEST_CONFIG": str(config),
                        "TESTCONTAINERS_REUSE_ENABLE": "false"})
    argv = ["./gradlew", *[f":services:{owner}:integrationTest" for owner in OWNERS],
            "--no-daemon", "--rerun-tasks", "--console", "plain"]
    started_ns = time.time_ns()
    for owner in OWNERS:
        for path in ROOT.glob(f"services/{owner}/build/test-results/integrationTest/TEST-*.xml"):
            path.unlink()
    result = subprocess.run(argv, cwd=ROOT, env=environment, capture_output=True, timeout=1500)
    log = ROOT / ".ai/evidence/numbering-v1/t003-owner-command.txt"
    log.write_bytes(result.stdout + result.stderr)
    command = {"argv": argv, "exit_code": result.returncode, "started_ns": started_ns,
               "log": str(log.relative_to(ROOT)), "log_sha256": hashlib.sha256(log.read_bytes()).hexdigest()}
    return result.returncode, started_ns, command


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--task", choices=["T003"], required=True)
    parser.parse_args()
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    REPORT.unlink(missing_ok=True)
    counts = {"selected": 1, "executed": 0, "failed": 0}
    evidence = {}
    fixture = None
    exit_code = 2
    try:
        prerequisites()
        fixture = TestInstallation.create()
        code, started_ns, command = invoke(fixture)
        evidence["command"] = command
        exit_code = 1 if code else 2
        counts, reports = owner_results(started_ns)
        evidence.update({"reports": reports, "namespace": fixture.namespace})
        exit_code = 1 if code or counts["failed"] else 2 if counts["selected"] != counts["executed"] else 0
    except (SetupError, OSError, ET.ParseError, subprocess.TimeoutExpired) as error:
        print(f"Setup unavailable: {error}", file=sys.stderr)
        evidence["setup_error"] = str(error)
    finally:
        if fixture is not None:
            exit_code = preserve_failure(exit_code, fixture.reset)
    REPORT.write_text(json.dumps({**counts, **evidence, "exit_code": exit_code,
                                  "finished_at_utc": datetime.now(timezone.utc).isoformat()}, indent=2) + "\n")
    return exit_code


if __name__ == "__main__":
    sys.exit(main())
