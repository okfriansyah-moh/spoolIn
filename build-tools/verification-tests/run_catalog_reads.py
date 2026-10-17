"""Run T012 catalog read checks — unit rules and integration boundary."""

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


def catalog_unit(task="catalogReadUnitTest"):
    module = "services/catalog"
    directory = ROOT / module / "build/test-results" / task
    for path in directory.glob("TEST-*.xml"):
        path.unlink()
    started, result = runner.command(
        ["./gradlew", ":" + module.replace("/", ":") + ":" + task, "--console", "plain"],
        "catalog-read-unit",
    )
    cases, reports = runner.junit(directory, started, runner.OUTPUT / "junit" / "catalog-read-unit")
    return result, cases, reports


def core_unit(task="catalogViewUnitTest"):
    module = "services/core"
    directory = ROOT / module / "build/test-results" / task
    for path in directory.glob("TEST-*.xml"):
        path.unlink()
    started, result = runner.command(
        ["./gradlew", ":" + module.replace("/", ":") + ":" + task, "--console", "plain"],
        "core-catalogview-unit",
    )
    cases, reports = runner.junit(directory, started, runner.OUTPUT / "junit" / "core-catalogview-unit")
    return result, cases, reports


def catalog_reads_integration(task="catalogReadsTest"):
    module = "tests/integration"
    directory = ROOT / module / "build/test-results" / task
    for path in directory.glob("TEST-*.xml"):
        path.unlink()
    started, result = runner.command(
        ["./gradlew", ":" + module.replace("/", ":") + ":" + task, "--console", "plain"],
        "catalog-reads-integration",
    )
    cases, reports = runner.junit(directory, started, runner.OUTPUT / "junit" / "catalog-reads-integration")
    return result, cases, reports


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--task", choices=["T012"], required=True)
    parser.add_argument("--level", choices=["unit", "integration"], required=True)
    arguments = parser.parse_args()
    runner.OUTPUT = ROOT / ".ai/evidence/numbering-v1/t012-catalog-reads" / arguments.level / (
        "run-" + uuid.uuid4().hex
    )
    runner.OUTPUT.mkdir(parents=True)
    report = ROOT / f".ai/evidence/numbering-v1/{arguments.task.lower()}-{arguments.level}-results.json"
    report.unlink(missing_ok=True)

    if arguments.level == "unit":
        suites = [catalog_unit, core_unit]
    else:
        suites = [catalog_reads_integration]

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
            any(item["exit_code"] for item in commands) or any(case["failed"] for case in cases)
        )
        if len(commands) != len(suites) or not cases or not all(case["executed"] for case in cases):
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
                    "isolated rules"
                    if arguments.level == "unit"
                    else "real PostgreSQL 18.6 and service transport"
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
