"""Run T011 fenced recovery and bounded retries checks."""

import argparse
from datetime import datetime, timezone
import json
from pathlib import Path
import shutil
import subprocess
import sys
import uuid
import xml.etree.ElementTree as ET

import run_policy as runner

ROOT = Path(__file__).resolve().parents[2]


def owner(owner_name, task="workIntegrationTest", module=None):
    module = module or f"services/{owner_name}"
    directory = ROOT / module / "build/test-results" / task
    for path in directory.glob("TEST-*.xml"):
        path.unlink()
    started, result = runner.command(
        ["./gradlew", ":" + module.replace("/", ":") + ":" + task, "--console", "plain"], owner_name)
    cases, reports = runner.junit(directory, started, runner.OUTPUT / "junit" / owner_name)
    return result, cases, reports


def leases(task="leasesTest"):
    module = "tests/integration"
    directory = ROOT / module / "build/test-results" / task
    for path in directory.glob("TEST-*.xml"):
        path.unlink()
    started, result = runner.command(
        ["./gradlew", ":" + module.replace("/", ":") + ":" + task, "--console", "plain"], "leases")
    cases, reports = runner.junit(directory, started, runner.OUTPUT / "junit" / "leases")
    return result, cases, reports


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--task", choices=["T011"], required=True)
    parser.add_argument("--level", choices=["unit", "integration"], required=True)
    arguments = parser.parse_args()
    runner.OUTPUT = ROOT / ".ai/evidence/numbering-v1/t011-work" / arguments.level / ("run-" + uuid.uuid4().hex)
    runner.OUTPUT.mkdir(parents=True)
    report = ROOT / f".ai/evidence/numbering-v1/{arguments.task.lower()}-{arguments.level}-results.json"
    report.unlink(missing_ok=True)

    if arguments.level == "unit":
        suites = [
            lambda: runner.backend("services/core", ["com.spoolin.core.operations.work.*"], "core"),
            lambda: runner.backend("services/catalog", ["com.spoolin.catalog.operations.work.*"], "catalog"),
            lambda: runner.backend("services/media", ["com.spoolin.media.operations.work.*"], "media"),
        ]
    else:
        # Cross-service integration tests live in tests/integration/work/leases/
        suites = [leases]

    commands, cases, reports, error = [], [], [], None
    code = 2
    try:
        for run in suites:
            command, selected, originals = run()
            commands.append(command)
            cases.extend(selected)
            reports.extend(originals)
            if command["exit_code"]:
                break
        code = int(any(item["exit_code"] for item in commands) or any(case["failed"] for case in cases))
        if len(commands) != len(suites) or not cases or not all(case["executed"] for case in cases):
            code = code or 2
    except (OSError, ValueError, ET.ParseError, subprocess.TimeoutExpired) as problem:
        error = str(problem)

    report.write_text(json.dumps({
        "selected": len(cases), "executed": sum(case["executed"] for case in cases),
        "failed": sum(case["failed"] for case in cases), "exit_code": code,
        "commands": commands, "reports": reports, "cases": cases, "setup_error": error,
        "proof_boundary": "isolated rules" if arguments.level == "unit" else "real owner PostgreSQL18.6 and service transport",
        "finished_at_utc": datetime.now(timezone.utc).isoformat(),
    }, indent=2) + "\n")
    shutil.copyfile(report, runner.OUTPUT / "results.json")
    return code


if __name__ == "__main__":
    sys.exit(main())
