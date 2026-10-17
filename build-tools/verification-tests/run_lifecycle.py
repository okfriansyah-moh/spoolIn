"""Run T024 lifecycle backend checks."""

import argparse
from datetime import datetime, timezone
import json
from pathlib import Path
import shutil
import uuid
import xml.etree.ElementTree as ET

import run_policy as runner

ROOT = Path(__file__).resolve().parents[2]


def catalog_lifecycle():
    directory = ROOT / "services/catalog/build/test-results/catalogLifecycleUnitTest"
    for path in directory.glob("TEST-*.xml"):
        path.unlink()
    started, result = runner.command(
        ["./gradlew", ":services:catalog:catalogLifecycleUnitTest", "--console", "plain"],
        "catalog-lifecycle-unit",
    )
    cases, reports = runner.junit(directory, started, runner.OUTPUT / "junit" / "catalog-lifecycle-unit")
    return result, cases, reports


def core_lifecycle():
    directory = ROOT / "services/core/build/test-results/lifecycleProjectionUnitTest"
    for path in directory.glob("TEST-*.xml"):
        path.unlink()
    started, result = runner.command(
        ["./gradlew", ":services:core:lifecycleProjectionUnitTest", "--console", "plain"],
        "core-lifecycle-unit",
    )
    cases, reports = runner.junit(directory, started, runner.OUTPUT / "junit" / "core-lifecycle-unit")
    return result, cases, reports


def integration():
    directory = ROOT / "tests/integration/build/test-results/lifecycleIntegrationTest"
    for path in directory.glob("TEST-*.xml"):
        path.unlink()
    started, result = runner.command(
        ["./gradlew", ":tests:integration:lifecycleIntegrationTest", "--console", "plain"],
        "lifecycle-integration",
    )
    cases, reports = runner.junit(directory, started, runner.OUTPUT / "junit" / "lifecycle-integration")
    return result, cases, reports


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--task", choices=["T024"], required=True)
    parser.add_argument("--level", choices=["unit", "integration"], required=True)
    arguments = parser.parse_args()
    runner.OUTPUT = ROOT / ".ai/evidence/numbering-v1/t024-lifecycle" / arguments.level / ("run-" + uuid.uuid4().hex)
    runner.OUTPUT.mkdir(parents=True)
    report = ROOT / f".ai/evidence/numbering-v1/{arguments.task.lower()}-{arguments.level}-results.json"
    report.unlink(missing_ok=True)

    suites = [catalog_lifecycle, core_lifecycle] if arguments.level == "unit" else [integration]
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
    except (OSError, ValueError, ET.ParseError) as problem:
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
                "proof_boundary": "isolated rules" if arguments.level == "unit" else "cross-module lifecycle smoke",
                "finished_at_utc": datetime.now(timezone.utc).isoformat(),
            },
            indent=2,
        )
        + "\n",
    )
    shutil.copyfile(report, runner.OUTPUT / "results.json")
    return code


if __name__ == "__main__":
    raise SystemExit(main())
