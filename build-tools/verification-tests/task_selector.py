"""Execute only the selected task/level and preserve actual check results."""

import argparse
import json
import os
from pathlib import Path
import subprocess
import sys
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parents[2]
MAP = ROOT / "build-tools/verification-map.json"
REPORT = ROOT / ".ai/evidence/t001-tests.json"


def selected_entries(mapping, level, tasks):
    names = tasks or list(mapping["tasks"])
    entries = []
    for name in names:
        task = mapping["tasks"].get(name)
        if task is None:
            raise ValueError(f"Unknown task: {name}")
        suite = task.get(level)
        if not suite:
            raise ValueError(f"Unavailable required {level} selection for {name}: {task['pending_reason']}")
        entries.extend(suite)
    if not entries:
        raise ValueError("Empty required selection")
    return entries


def execute(entry, cwd=ROOT):
    report = cwd / entry["report"]
    report.unlink(missing_ok=True)
    try:
        result = subprocess.run(entry["argv"], cwd=cwd, timeout=entry["timeout"], check=False)
    except (FileNotFoundError, subprocess.TimeoutExpired) as error:
        print(f"Setup unavailable: {error}", file=sys.stderr)
        return 2, {"selected": 1, "executed": 0, "failed": 0}
    if not report.is_file():
        return result.returncode or 2, {"selected": 1, "executed": 0, "failed": 0}
    try:
        counts = json.loads(report.read_text())
        if any(type(counts[key]) is not int or counts[key] < 0 for key in ("selected", "executed", "failed")):
            raise ValueError("Invalid report counts")
        if counts["selected"] < 1 or counts["executed"] != counts["selected"]:
            return result.returncode or 2, counts
        if counts["failed"] > counts["executed"]:
            raise ValueError("Invalid failure count")
    except (ValueError, KeyError, TypeError):
        return 2, {"selected": 1, "executed": 0, "failed": 0}
    if counts["failed"] and result.returncode == 0:
        return 1, counts
    return 0 if result.returncode == 0 else 2 if result.returncode == 2 else 1, counts


def save_report(counts, entries, exit_code):
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    REPORT.write_text(json.dumps({
        **counts, "exit_code": exit_code, "entries": entries,
        "finished_at_utc": datetime.now(timezone.utc).isoformat(),
        "proof_boundary": "isolated harness, compiler and static build fixture",
    }, indent=2) + "\n")


def run(entries):
    counts = {"selected": 0, "executed": 0, "failed": 0}
    results = []
    exit_code = 0
    for entry in entries:
        code, result = execute(entry)
        results.append({"id": entry["id"], "argv": entry["argv"], "exit_code": code, **result})
        for key in counts:
            counts[key] += result[key]
        if code:
            exit_code = code
            break
    save_report(counts, results, exit_code)
    return exit_code


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("level", choices=["unit", "integration"])
    parser.add_argument("--task", action="append")
    arguments = parser.parse_args()
    REPORT.unlink(missing_ok=True)
    try:
        mapping = json.loads(MAP.read_text())
        entries = selected_entries(mapping, arguments.level, arguments.task)
    except (ValueError, OSError) as error:
        print(f"Setup unavailable: {error}", file=sys.stderr)
        return 2
    return run(entries)


if __name__ == "__main__":
    sys.exit(main())
