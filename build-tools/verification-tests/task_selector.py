"""Execute only the selected task/level and preserve actual check results."""

import argparse
import json
import re
from pathlib import Path
import subprocess
import sys
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parents[2]
MAP = ROOT / "build-tools/verification-map.json"
def report_path(level, tasks):
    name = tasks[0].lower() if tasks and len(tasks) == 1 and re.fullmatch(r"T[0-9]{3}", tasks[0]) else f"{level}-selection"
    return ROOT / f".ai/evidence/numbering-v1/{name}-tests.json"


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
    for pattern in entry.get("required_paths", []):
        if not any(path.is_file() for path in cwd.glob(pattern)):
            print(f"Setup unavailable: required source {pattern}", file=sys.stderr)
            return 2, {"selected": 1, "executed": 0, "failed": 0}
    try:
        result = subprocess.run(entry["argv"], cwd=cwd, timeout=entry["timeout"], check=False)
    except (OSError, subprocess.TimeoutExpired) as error:
        print(f"Setup unavailable: {error}", file=sys.stderr)
        return 2, {"selected": 1, "executed": 0, "failed": 0}
    if not report.is_file():
        return 2, {"selected": 1, "executed": 0, "failed": 0}
    try:
        counts = json.loads(report.read_text())
        if any(type(counts[key]) is not int or counts[key] < 0 for key in ("selected", "executed", "failed")):
            raise ValueError("Invalid report counts")
        if counts["selected"] < 1 or counts["executed"] != counts["selected"]:
            return 2, counts
        if counts["failed"] > counts["executed"]:
            raise ValueError("Invalid failure count")
    except (OSError, ValueError, KeyError, TypeError):
        return 2, {"selected": 1, "executed": 0, "failed": 0}
    if counts["failed"]:
        return 1, counts
    return 0 if result.returncode == 0 else 2 if result.returncode == 2 else 1, counts


def save_report(counts, entries, exit_code, report, level):
    report.parent.mkdir(parents=True, exist_ok=True)
    report.write_text(json.dumps({
        **counts, "exit_code": exit_code, "entries": entries,
        "finished_at_utc": datetime.now(timezone.utc).isoformat(),
        "level": level, "proof_boundary": "selected task suites at their declared boundaries",
    }, indent=2) + "\n")


def run(entries, report, level):
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
    save_report(counts, results, exit_code, report, level)
    return exit_code


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("level", choices=["unit", "integration"])
    parser.add_argument("--task", action="append")
    arguments = parser.parse_args()
    report = report_path(arguments.level, arguments.task)
    try:
        mapping = json.loads(MAP.read_text())
        entries = selected_entries(mapping, arguments.level, arguments.task)
    except (ValueError, OSError) as error:
        print(f"Setup unavailable: {error}", file=sys.stderr)
        return 2
    report.unlink(missing_ok=True)
    return run(entries, report, arguments.level)


if __name__ == "__main__":
    sys.exit(main())
