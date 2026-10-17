"""Run T018 person write and managed-person checks — unit rules."""

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


def catalog_person_unit(task="catalogPersonWriteUnitTest"):
    module = "services/catalog"
    directory = ROOT / module / "build/test-results" / task
    for path in directory.glob("TEST-*.xml"):
        path.unlink()
    started, result = runner.command(
        ["./gradlew", ":" + module.replace("/", ":") + ":" + task, "--console", "plain"],
        "catalog-person-write-unit",
    )
    cases, reports = runner.junit(directory, started, runner.OUTPUT / "junit" / "catalog-person-write-unit")
    return result, cases, reports


def core_managed_person_unit(task="managedPersonUnitTest"):
    module = "services/core"
    directory = ROOT / module / "build/test-results" / task
    for path in directory.glob("TEST-*.xml"):
        path.unlink()
    started, result = runner.command(
        ["./gradlew", ":" + module.replace("/", ":") + ":" + task, "--console", "plain"],
        "core-managed-person-unit",
    )
    cases, reports = runner.junit(directory, started, runner.OUTPUT / "junit" / "core-managed-person-unit")
    return result, cases, reports


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--task", choices=["T018"], required=True)
    parser.add_argument("--level", choices=["unit"], required=True)
    arguments = parser.parse_args()
    runner.OUTPUT = ROOT / ".ai/evidence/numbering-v1/t018-person-writes" / arguments.level / (
        "run-" + uuid.uuid4().hex
    )
    runner.OUTPUT.mkdir(parents=True)
    report = ROOT / f".ai/evidence/numbering-v1/{arguments.task.lower()}-{arguments.level}-results.json"
    report.unlink(missing_ok=True)

    suites = [catalog_person_unit, core_managed_person_unit]

    commands, cases, reports_list, error = [], [], [], None
    code = 2
    try:
        for run in suites:
            command, selected, originals = run()
            commands.append(command)
            cases.extend(selected)
            reports_list.extend(originals)
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
                "reports": reports_list,
                "cases": cases,
                "setup_error": error,
                "proof_boundary": "isolated rules — no database access",
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
