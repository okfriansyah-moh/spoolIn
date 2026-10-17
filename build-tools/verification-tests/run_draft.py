"""Run T015 draft controller unit and component browser checks."""

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


def frontend_unit():
    path = ROOT / ".ai/evidence/numbering-v1/t015-unit-vitest.json"
    path.unlink(missing_ok=True)
    _, setup = runner.command(
        ["npm", "run", "pretest:coverage", "--workspace", "frontend"],
        "frontend-setup",
    )
    if setup["exit_code"]:
        raise ValueError("Required draft frontend setup failed")
    argv = [
        "node",
        "build-tools/verification-tests/frontend/run-t015-tests.mjs",
    ]
    started, result = runner.command(argv, "frontend-unit")
    if not path.is_file() or path.stat().st_mtime_ns < started:
        raise ValueError("Fresh T015 Vitest report is required")
    document = json.loads(path.read_text())
    cases = [
        {
            "class": suite["name"],
            "method": case["fullName"],
            "executed": case["status"] != "pending",
            "failed": case["status"] != "passed",
        }
        for suite in document["testResults"]
        for case in suite["assertionResults"]
    ]
    result["setup"] = setup
    return result, cases, [runner.artifact(path)]


def browser():
    started, result = runner.command(
        ["node", "tests/browser/run-draft-component.mjs"], "browser"
    )
    cases, reports = runner.junit(
        ROOT / "test-results/draft-component",
        started,
        runner.OUTPUT / "junit/browser",
        "junit.xml",
    )
    return result, cases, reports


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--task", choices=["T015"], required=True)
    parser.add_argument("--level", choices=["unit", "integration"], required=True)
    arguments = parser.parse_args()
    runner.OUTPUT = (
        ROOT
        / f".ai/evidence/numbering-v1/{arguments.task.lower()}-draft"
        / arguments.level
        / ("run-" + uuid.uuid4().hex)
    )
    runner.OUTPUT.mkdir(parents=True)
    report = (
        ROOT
        / f".ai/evidence/numbering-v1/{arguments.task.lower()}-{arguments.level}-results.json"
    )
    report.unlink(missing_ok=True)

    suites = [frontend_unit] if arguments.level == "unit" else [browser]

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
        code = int(
            any(item["exit_code"] for item in commands)
            or any(case["failed"] for case in cases)
        )
        if (
            len(commands) != len(suites)
            or not cases
            or not all(case["executed"] for case in cases)
        ):
            code = code or 2
    except (OSError, ValueError, ET.ParseError, subprocess.TimeoutExpired) as problem:
        error = str(problem)

    report.write_text(
        json.dumps(
            {
                "selected": len(cases),
                "executed": sum(case["executed"] for case in cases),
                "failed": sum(case["failed"] for case in cases),
                "exit_code": code,
                "commands": commands,
                "reports": reports,
                "cases": cases,
                "setup_error": error,
                "proof_boundary": (
                    "isolated draft controller and controlled component fixture"
                    if arguments.level == "unit"
                    else "rendered draft component with controlled GraphQL"
                ),
                "finished_at_utc": datetime.now(timezone.utc).isoformat(),
            },
            indent=2,
        )
        + "\n"
    )
    shutil.copyfile(report, runner.OUTPUT / "results.json")
    return code


if __name__ == "__main__":
    sys.exit(main())
