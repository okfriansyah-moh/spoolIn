"""Run T022 artwork stream acceptance and processing checks."""

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


def media_unit():
    directory = ROOT / "services/media/build/test-results/artworkUnitTest"
    for path in directory.glob("TEST-*.xml"):
        path.unlink()
    started, result = runner.command(
        ["./gradlew", ":services:media:artworkUnitTest", "--console", "plain"], "media-artwork-unit")
    cases, reports = runner.junit(directory, started, runner.OUTPUT / "junit" / "media-artwork-unit")
    return result, cases, reports


def media_integration():
    directory = ROOT / "services/media/build/test-results/artworkIntegrationTest"
    for path in directory.glob("TEST-*.xml"):
        path.unlink()
    started, result = runner.command(
        ["./gradlew", ":services:media:artworkIntegrationTest", "--console", "plain"], "media-artwork-integration")
    cases, reports = runner.junit(directory, started, runner.OUTPUT / "junit" / "media-artwork-integration")
    return result, cases, reports


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--task", choices=["T022"], required=True)
    parser.add_argument("--level", choices=["unit", "integration"], required=True)
    arguments = parser.parse_args()
    runner.OUTPUT = (
        ROOT / f".ai/evidence/numbering-v1/t022-artwork" / arguments.level / ("run-" + uuid.uuid4().hex)
    )
    runner.OUTPUT.mkdir(parents=True)
    report = ROOT / f".ai/evidence/numbering-v1/{arguments.task.lower()}-{arguments.level}-results.json"
    report.unlink(missing_ok=True)

    suites = [media_unit] if arguments.level == "unit" else [media_integration]

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
        "selected": len(cases),
        "executed": sum(case["executed"] for case in cases),
        "failed": sum(case["failed"] for case in cases),
        "exit_code": code,
        "commands": commands,
        "reports": reports,
        "cases": cases,
        "setup_error": error,
        "proof_boundary": (
            "isolated unit rules — no DB or filesystem" if arguments.level == "unit"
            else "real owner PostgreSQL18.6 with asset schema boundary probes"
        ),
        "finished_at_utc": datetime.now(timezone.utc).isoformat(),
    }, indent=2) + "\n")
    shutil.copyfile(report, runner.OUTPUT / "results.json")
    return code


if __name__ == "__main__":
    sys.exit(main())
