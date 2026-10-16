"""Run selected owner boundary checks through existing command and JUnit helpers."""

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


def owner(owner_name, task="receiptIntegrationTest", module=None):
    module = module or f"services/{owner_name}"
    directory = ROOT / module / "build/test-results" / task
    for path in directory.glob("TEST-*.xml"):
        path.unlink()
    started, result = runner.command(
        ["./gradlew", ":" + module.replace("/", ":") + ":" + task, "--console", "plain"], owner_name)
    cases, reports = runner.junit(directory, started, runner.OUTPUT / "junit" / owner_name)
    return result, cases, reports


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--task", choices=["T007", "T009"], required=True)
    parser.add_argument("--level", choices=["unit", "integration"], required=True)
    arguments = parser.parse_args()
    folder = "t007-receipts" if arguments.task == "T007" else "t009-boundaries"
    runner.OUTPUT = ROOT / ".ai/evidence/numbering-v1" / folder / arguments.level / ("run-" + uuid.uuid4().hex)
    runner.OUTPUT.mkdir(parents=True)
    report = ROOT / f".ai/evidence/numbering-v1/{arguments.task.lower()}-{arguments.level}-results.json"
    report.unlink(missing_ok=True)
    suites = [lambda: runner.backend("libraries/backend-support", ["com.spoolin.support.receipts.*"], "shared")]
    if arguments.level == "unit":
        suites.append(lambda: runner.backend("services/core", ["com.spoolin.core.operations.receipts.*"], "core"))
    if arguments.level == "integration":
        suites = [lambda name=name: owner(name) for name in ["core", "catalog", "media"]]
    if arguments.task == "T009":
        suites = [lambda: runner.backend("libraries/backend-support", ["com.spoolin.support.identity.*"], "shared")]
        if arguments.level == "unit":
            suites.append(lambda: runner.backend("services/core", ["*ActorIssuerRulesTest"], "issuer"))
        else:
            suites = [lambda: owner("admission", "admissionIntegrationTest", "services/core"),
                      lambda: owner("actors", "test", "tests/integration")]
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
        "proof_boundary": "isolated rules" if arguments.level == "unit" else "real owner PostgreSQL and service transport",
        "finished_at_utc": datetime.now(timezone.utc).isoformat(),
    }, indent=2) + "\n")
    shutil.copyfile(report, runner.OUTPUT / "results.json")
    return code


if __name__ == "__main__":
    sys.exit(main())
